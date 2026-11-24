package com.yooshastan.ultmessenger.video;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;

import org.telegram.tgnet.TLRPC;
import org.telegram.messenger.DialogObject;

import com.yausername.ffmpeg.FFmpeg;
import com.yausername.youtubedl_android.YoutubeDL;
import com.yausername.youtubedl_android.YoutubeDLRequest;

import java.io.File;
import java.io.FileInputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import android.os.SystemClock;

import kotlin.Unit;

/**
 * Experimental opt-in on-device yt-dlp/FFmpeg worker for supported public sites.
 * Never uses an extraction bot or shares chat contents with a download service.
 * External traffic DOES NOT inherit Telegram MTProxy.
 *
 * Strict mode blocks new work and requests cancellation of active subprocesses.
 * Android process cancellation is best effort; this is NOT a system VPN kill switch.
 */
public final class UltVideoSiteDownloader {
    private static final Map<String, Integer> OWNERS = new ConcurrentHashMap<>();
    private static volatile String extractorVersion = "Not initialized";

    public static String extractorVersion() { return extractorVersion; }

    public static boolean hasActive(int account) { return OWNERS.containsValue(account); }

    public static void cancelAccount(int account) {
        for (Map.Entry<String, Integer> entry : OWNERS.entrySet()) {
            if (entry.getValue() != account) continue;
            AtomicBoolean flag = ACTIVE.get(entry.getKey());
            if (flag != null) flag.set(true);
            try { YoutubeDL.getInstance().destroyProcessById(entry.getKey()); }
            catch (Exception ignored) { }
        }
    }
    // The extractor can block without generating a callback. Both limits
    // are measured with a monotonic clock (not the adjustable wall clock).
    private static final long MAX_EXTRACTION_MS = TimeUnit.MINUTES.toMillis(4);
    private static final long NO_PROGRESS_MS = TimeUnit.SECONDS.toMillis(90);
    private static final Map<String, AtomicBoolean> ACTIVE = new ConcurrentHashMap<>();
    private static final ScheduledExecutorService WATCHDOG =
            Executors.newSingleThreadScheduledExecutor();
    private static final Object START_LOCK = new Object();

    private UltVideoSiteDownloader() { }

    public static void cancelAll() {
        for (Map.Entry<String, AtomicBoolean> entry : ACTIVE.entrySet()) {
            entry.getValue().set(true);
            try {
                YoutubeDL.getInstance().destroyProcessById(entry.getKey());
            } catch (Exception ignored) {
                // The process may not have started yet. The watchdog will retry.
            }
        }
    }

    /** Called only for URLs that passed UltVideoLinkRules.isSiteUrl(). */
    public static boolean download(Context context, int account, String url,
                                   long dialogId, TLRPC.Message original, boolean outgoingLink) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            UltVideoAutoSave.noteResult(account, "Site downloads require Android 10+");
            return false;
        }
        if (!UltVideoLinkRules.isSiteUrl(url) || !allowed(account)) return false;

        final int limitMiB = UltVideoAutoSave.siteLimitMiB(account);
        final long maxBytes = limitMiB * 1024L * 1024L;
        String processId = "ultsite-" + UUID.randomUUID();
        AtomicBoolean stopped = new AtomicBoolean(false);
        File cacheBase = context.getExternalCacheDir();
        if (cacheBase == null) cacheBase = context.getCacheDir();
        File work = new File(cacheBase, processId);
        if (!work.mkdirs()) {
            UltVideoAutoSave.noteResult(account, "Cannot create download workspace");
            return false;
        }
        ACTIVE.put(processId, stopped);
        OWNERS.put(processId, account);
        ScheduledFuture<?> guard = null;
        String stage = "initialization";
        final AtomicReference<String> stopReason = new AtomicReference<>();
        final AtomicLong lastProgress = new AtomicLong(0L);
        final AtomicLong lastProgressDisplayed = new AtomicLong(-1L);
        try {
            if (!allowed(account)) return false;
            // The Android library contains its own Python/yt-dlp and FFmpeg runtimes.
            // Initialization is deliberately lazy; this code runs on a worker thread.
            YoutubeDL.getInstance().init(context);
            FFmpeg.getInstance().init(context);
            if (!allowed(account) || stopped.get()) return false;
            stage = "download";

            String extractionUrl = UltVideoLinkRules.extractionUrl(url);
            if ("X/Twitter".equals(UltVideoLinkRules.sourceCategory(url))) {
                extractionUrl = com.yooshastan.ultmessenger.settings.UltLinkSanitizerPolicy.cleanUrl(extractionUrl);
            }
            YoutubeDLRequest request = new YoutubeDLRequest(extractionUrl);
            request.addOption("--ignore-config");
            request.addOption("--no-playlist");
            request.addOption("--no-overwrites");
            request.addOption("--no-mtime");
            request.addOption("--no-cache-dir");
            request.addOption("--no-write-thumbnail");
            request.addOption("--max-filesize", Long.toString(maxBytes));
            request.addOption("--socket-timeout", 12);
            request.addOption("--retries", 2);
            request.addOption("--fragment-retries", 2);
            request.addOption("--extractor-retries", 2);
            request.addOption("-f", UltVideoLinkRules.videoFormat(UltVideoAutoSave.maxVideoHeight(account)));
            request.addOption("--merge-output-format", "mp4");
            request.addOption("-o", new File(work, "ult_video.%(ext)s").getAbsolutePath());

            // A finite extraction timeout is essential. Some supported sites
            // hang before they return any stdout or progress callback.
            final long startedAt = SystemClock.elapsedRealtime();
            lastProgress.set(startedAt);
            guard = WATCHDOG.scheduleAtFixedRate(() -> {
                if (!stopped.get()) {
                    final long now = SystemClock.elapsedRealtime();
                    final String reason;
                    if (!allowed(account)) {
                        reason = "Download stopped: network or strict-proxy protection changed";
                    } else if (totalBytes(work, maxBytes) > maxBytes) {
                        reason = "Download stopped: " + limitMiB + " MiB workspace limit";
                    } else if (now - startedAt >= MAX_EXTRACTION_MS) {
                        reason = "Extractor timed out after 4 minutes; video not saved";
                    } else if (now - lastProgress.get() >= NO_PROGRESS_MS) {
                        reason = "Extractor stalled for 90 seconds; no media progress";
                    } else {
                        reason = null;
                    }
                    if (reason != null && stopReason.compareAndSet(null, reason)) {
                        stopped.set(true);
                        // Set an honest terminal-looking status even if the
                        // external process never exits its blocking read.
                        UltVideoAutoSave.noteResult(account, reason);
                    }
                }
                if (stopped.get()) {
                    try {
                        YoutubeDL.getInstance().destroyProcessById(processId);
                    } catch (Exception ignored) { }
                }
            }, 0, 500, TimeUnit.MILLISECONDS);

            if (!allowed(account) || stopped.get()) return false;
            // Query the executable, not the updater's optional SharedPreferences metadata.
            if (!allowed(account) || stopped.get()) return false;
            try {
                YoutubeDLRequest versionRequest = new YoutubeDLRequest(
                        java.util.Collections.<String>emptyList());
                versionRequest.addOption("--ignore-config");
                versionRequest.addOption("--version");
                String version = YoutubeDL.getInstance()
                        .execute(versionRequest, processId, null).getOut().trim();
                extractorVersion = version.matches("[0-9]{4}\\.[0-9]{1,2}\\.[0-9]{1,2}[A-Za-z0-9._+-]{0,60}")
                        ? version : "Unknown (invalid version output)";
            } catch (Exception ignored) {
                extractorVersion = "Unknown (version query failed)";
            }
            if (!allowed(account) || stopped.get()) return false;
            lastProgress.set(SystemClock.elapsedRealtime());
            UltVideoAutoSave.noteResult(account, "Extracting video; 90-second inactivity limit");
            // Never display 'line': extractor output can expose URL paths,
            // authorization tokens, cookies, or personal information.
            YoutubeDL.getInstance().execute(request, processId,
                    (progress, eta, line) -> {
                        if (!allowed(account) && stopReason.compareAndSet(null,
                                "Download stopped: network or strict-proxy protection changed")) {
                            stopped.set(true);
                        }
                        if (!stopped.get() && progress >= 1.0f && progress <= 100.0f) {
                            lastProgress.set(SystemClock.elapsedRealtime());
                            final long bucket = Math.min(100L, (progress.longValue() / 10L) * 10L);
                            if (bucket >= 10 && bucket > lastProgressDisplayed.get()) {
                                lastProgressDisplayed.set(bucket);
                                UltVideoAutoSave.noteResult(account,
                                        "Downloading stream: " + bucket + "%"
                                                + (eta >= 0 ? " · ETA " + eta + "s" : ""));
                            }
                        }
                        if (stopped.get()) {
                            try {
                                YoutubeDL.getInstance().destroyProcessById(processId);
                            } catch (Exception ignored) { }
                        }
                        return Unit.INSTANCE;
                    });

            if (stopped.get() || !allowed(account)) {
                UltVideoAutoSave.noteResult(account, stopReason.get() == null
                        ? "Download cancelled or blocked by protection" : stopReason.get());
                return false;
            }
            stage = "verification";
            File result = selectFinishedVideo(work);
            if (result == null || result.length() <= 0 || result.length() > maxBytes) {
                UltVideoAutoSave.noteResult(account, "No supported video file within " + limitMiB + " MiB");
                return false;
            }
            stage = "saving";
            if (!publish(context, result, account, maxBytes, stopped)) {
                UltVideoAutoSave.noteResult(account, stopped.get() ? "Download cancelled" : "Failed to save video");
                return false;
            }
            UltVideoAutoSave.noteResult(account, "Saved to Downloads/Ultgram");
            if (outgoingLink && UltVideoAutoSave.isPostToChat(account) && !stopped.get()) {
                if (!UltVideoChatPost.safeDestination(account, dialogId, original)) {
                    UltVideoAutoSave.noteResult(account,
                            "Saved locally; group identity, permissions or topic not supported");
                } else if (!UltVideoChatPost.enqueue(context, account, dialogId,
                        original, result, true)) {
                    UltVideoAutoSave.noteResult(account,
                            "Saved locally; Telegram media auto-post not queued");
                }
            }
            return true;
        } catch (Exception failure) {
            // Exceptions can contain private URLs or query tokens; never show raw text.
            UltVideoAutoSave.noteResult(account, stopped.get()
                    ? (stopReason.get() == null
                            ? "Download cancelled or blocked by protection"
                            : stopReason.get())
                    : UltVideoFailure.publicReason(failure, stage));
            return false;
        } finally {
            stopped.set(true);
            if (guard != null) guard.cancel(true);
            try {
                YoutubeDL.getInstance().destroyProcessById(processId);
            } catch (Exception ignored) { }
            OWNERS.remove(processId);
            ACTIVE.remove(processId);
            removeTree(work);
        }
    }

    private static boolean allowed(int account) {
        if (UltVideoAutoSave.strictModeRequested() || !UltVideoAutoSave.isEnabled(account))
            return false;
        return UltVideoAutoSave.networkAllowed(account);
    }

    private static long totalBytes(File dir, long maxBytes) {
        if (!dir.exists()) return 0L;
        if (!dir.isDirectory()) return dir.length();
        long size = 0L;
        File[] children = dir.listFiles();
        if (children != null) for (File f : children) {
            size += totalBytes(f, maxBytes);
            if (size > maxBytes) return size;
        }
        return size;
    }

    private static File selectFinishedVideo(File dir) {
        List<File> matching = new ArrayList<>();
        File[] files = dir.listFiles();
        if (files == null) return null;
        for (File file : files) {
            if (!file.isFile()) continue;
            String name = file.getName().toLowerCase(Locale.US);
            if (name.endsWith(".mp4") || name.endsWith(".webm")
                    || name.endsWith(".mkv") || name.endsWith(".mov")
                    || name.endsWith(".m4v")) matching.add(file);
        }
        return matching.size() == 1 ? matching.get(0) : null;
    }

    private static boolean publish(Context context, File file, int account, long maxBytes, AtomicBoolean stopped) {
        if (stopped.get() || !allowed(account)) return false;
        String originalName = file.getName().toLowerCase(Locale.US);
        String extension = originalName.substring(originalName.lastIndexOf('.') + 1);
        String mime = "video/mp4";
        if ("webm".equals(extension)) mime = "video/webm";
        if ("mkv".equals(extension)) mime = "video/x-matroska";
        if ("mov".equals(extension)) mime = "video/quicktime";
        String name = "ult_video_" + System.currentTimeMillis() + "_"
                + UUID.randomUUID().toString().substring(0, 8) + "." + extension;
        ContentResolver resolver = context.getContentResolver();
        ContentValues values = new ContentValues();
        values.put(MediaStore.MediaColumns.DISPLAY_NAME, name);
        values.put(MediaStore.MediaColumns.MIME_TYPE, mime);
        values.put(MediaStore.MediaColumns.RELATIVE_PATH,
                Environment.DIRECTORY_DOWNLOADS + "/Ultgram");
        values.put(MediaStore.MediaColumns.IS_PENDING, 1);
        Uri dest = null;
        boolean done = false;
        try {
            dest = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
            if (dest == null) return false;
            try (FileInputStream input = new FileInputStream(file);
                 OutputStream output = resolver.openOutputStream(dest, "w")) {
                if (output == null) return false;
                byte[] buffer = new byte[65536];
                long copied = 0;
                for (int n; (n = input.read(buffer)) != -1; ) {
                    if (stopped.get() || !allowed(account) || copied + n > maxBytes) return false;
                    output.write(buffer, 0, n);
                    copied += n;
                }
            }
            if (stopped.get() || !allowed(account)) return false;
            ContentValues visible = new ContentValues();
            visible.put(MediaStore.MediaColumns.IS_PENDING, 0);
            if (resolver.update(dest, visible, null, null) != 1) return false;
            done = true;
            UltVideoAutoSave.recordSiteSave(account, name, dest, file.length());
            return true;
        } catch (Exception ignored) {
            return false;
        } finally {
            if (!done && dest != null) {
                try { resolver.delete(dest, null, null); }
                catch (Exception ignored) { }
            }
        }
    }

    private static void removeTree(File dir) {
        if (dir == null || !dir.exists()) return;
        if (dir.isDirectory()) {
            File[] children = dir.listFiles();
            if (children != null) for (File f : children) removeTree(f);
        }
        dir.delete();
    }
}

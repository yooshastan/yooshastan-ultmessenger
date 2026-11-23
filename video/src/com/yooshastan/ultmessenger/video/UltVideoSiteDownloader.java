package com.yooshastan.ultmessenger.video;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
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
    private static final long MAX_BYTES = 100L * 1024L * 1024L;
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
                                   long dialogId, int originalMessageId, boolean outgoingLink) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            UltVideoAutoSave.noteResult(account, "Site downloads require Android 10+");
            return false;
        }
        if (!UltVideoLinkRules.isSiteUrl(url) || !allowed(account)) return false;

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
        ScheduledFuture<?> guard = null;
        String stage = "initialization";
        try {
            if (!allowed(account)) return false;
            // The Android library contains its own Python/yt-dlp and FFmpeg runtimes.
            // Initialization is deliberately lazy; this code runs on a worker thread.
            YoutubeDL.getInstance().init(context);
            FFmpeg.getInstance().init(context);
            if (!allowed(account) || stopped.get()) return false;
            stage = "download";

            YoutubeDLRequest request = new YoutubeDLRequest(url);
            request.addOption("--no-playlist");
            request.addOption("--no-overwrites");
            request.addOption("--no-mtime");
            request.addOption("--no-cache-dir");
            request.addOption("--no-write-thumbnail");
            request.addOption("--max-filesize", "100M");
            request.addOption("--socket-timeout", 12);
            request.addOption("--retries", 2);
            request.addOption("--fragment-retries", 2);
            request.addOption("--extractor-retries", 2);
            request.addOption("-f", "bv*[ext=mp4]+ba[ext=m4a]/b[ext=mp4]/b");
            request.addOption("--merge-output-format", "mp4");
            request.addOption("-o", new File(work, "ult_video.%(ext)s").getAbsolutePath());

            // A second byte-accurate disk check is required because --max-filesize
            // only works reliably when the site exposes the expected size.
            guard = WATCHDOG.scheduleAtFixedRate(() -> {
                if (!allowed(account) || totalBytes(work) > MAX_BYTES) {
                    stopped.set(true);
                }
                if (stopped.get()) {
                    try {
                        YoutubeDL.getInstance().destroyProcessById(processId);
                    } catch (Exception ignored) { }
                }
            }, 0, 250, TimeUnit.MILLISECONDS);

            if (!allowed(account) || stopped.get()) return false;
            UltVideoAutoSave.noteResult(account, "Extracting and downloading video");
            // Java/Kotlin bridge: callback is invoked off the UI thread.
            YoutubeDL.getInstance().execute(request, processId,
                    (progress, eta, line) -> {
                        if (!allowed(account)) stopped.set(true);
                        if (stopped.get()) {
                            YoutubeDL.getInstance().destroyProcessById(processId);
                        }
                        return Unit.INSTANCE;
                    });

            if (stopped.get() || !allowed(account)) {
                UltVideoAutoSave.noteResult(account, "Download stopped by protection or size limit");
                return false;
            }
            stage = "verification";
            File result = selectFinishedVideo(work);
            if (result == null || result.length() <= 0 || result.length() > MAX_BYTES) {
                UltVideoAutoSave.noteResult(account, "No supported video file within 100 MiB");
                return false;
            }
            stage = "saving";
            if (!publish(context, result, account)) {
                UltVideoAutoSave.noteResult(account, "Failed to save video");
                return false;
            }
            UltVideoAutoSave.noteResult(account, "Saved to Downloads/Ultgram");
            if (outgoingLink && UltVideoAutoSave.isPostToChat(account)) {
                if (!DialogObject.isUserDialog(dialogId)) {
                    // Telegram can post as channel/anonymous identity in groups.
                    // The ordinary media send API does not inherit that identity.
                    UltVideoAutoSave.noteResult(account,
                            "Saved locally; auto-post paused for group/anonymous sender safety");
                } else if (!UltVideoChatPost.enqueue(context, account, dialogId,
                        originalMessageId, result, true)) {
                    UltVideoAutoSave.noteResult(account,
                            "Saved locally; automatic Telegram send was not queued");
                }
            }
            return true;
        } catch (Exception failure) {
            // Exceptions can contain private URLs or query tokens; never show raw text.
            UltVideoAutoSave.noteResult(account, stopped.get()
                    ? "Download cancelled or blocked by protection"
                    : UltVideoFailure.publicReason(failure, stage));
            return false;
        } finally {
            stopped.set(true);
            if (guard != null) guard.cancel(true);
            try {
                YoutubeDL.getInstance().destroyProcessById(processId);
            } catch (Exception ignored) { }
            ACTIVE.remove(processId);
            removeTree(work);
        }
    }

    private static boolean allowed(int account) {
        if (UltVideoAutoSave.strictModeRequested() || !UltVideoAutoSave.isEnabled(account))
            return false;
        return UltVideoAutoSave.networkAllowed(account);
    }

    private static long totalBytes(File dir) {
        if (!dir.exists()) return 0L;
        if (!dir.isDirectory()) return dir.length();
        long size = 0L;
        File[] children = dir.listFiles();
        if (children != null) for (File f : children) {
            size += totalBytes(f);
            if (size > MAX_BYTES) return size;
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

    private static boolean publish(Context context, File file, int account) {
        if (!allowed(account)) return false;
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
                    if (!allowed(account) || copied + n > MAX_BYTES) return false;
                    output.write(buffer, 0, n);
                    copied += n;
                }
            }
            if (!allowed(account)) return false;
            ContentValues visible = new ContentValues();
            visible.put(MediaStore.MediaColumns.IS_PENDING, 0);
            if (resolver.update(dest, visible, null, null) != 1) return false;
            done = true;
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

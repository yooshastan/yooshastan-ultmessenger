package com.yooshastan.ultmessenger.video;

import android.app.DownloadManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Environment;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.DialogObject;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.UserConfig;
import org.telegram.tgnet.TLRPC;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Opt-in auto-save for newly received chat URLs. Only public HTTPS video
 * resources with a known length <=100 MiB are queued in Android DownloadManager.
 * External HTTP(S) traffic is NOT routed through Telegram's MTProto proxy.
 * Never process protected/secret messages or run while native strict mode is ON.
 */
public final class UltVideoAutoSave implements NotificationCenter.NotificationCenterDelegate {
    private static final String PREFS = "ult_video_link_autosave_v1";
    private static final long MAX_BYTES = 100L * 1024 * 1024;
    private static final ExecutorService EXECUTOR = Executors.newFixedThreadPool(2);
    private static final Set<String> IN_FLIGHT = Collections.synchronizedSet(new HashSet<>());
    private static final UltVideoAutoSave[] OBSERVERS = new UltVideoAutoSave[UserConfig.MAX_ACCOUNT_COUNT];
    private final int account;

    private UltVideoAutoSave(int account) { this.account = account; }

    public static void install() {
        AndroidUtilities.runOnUIThread(() -> {
            for (int a = 0; a < OBSERVERS.length; a++) {
                if (OBSERVERS[a] != null) continue;
                OBSERVERS[a] = new UltVideoAutoSave(a);
                NotificationCenter.getInstance(a).addObserver(OBSERVERS[a], NotificationCenter.didReceiveNewMessages);
            }
            if (strictModeRequested()) cancelPendingForStrict();
        });
    }

    private static SharedPreferences preferences() {
        return ApplicationLoader.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    private static String setting(String name, int account) { return name + "_" + account; }

    public static boolean isEnabled(int account) {
        return preferences().getBoolean(setting("enabled", account), false);
    }

    public static void setEnabled(int account, boolean enabled) {
        preferences().edit().putBoolean(setting("enabled", account), enabled).apply();
    }

    public static boolean isWifiOnly(int account) {
        return preferences().getBoolean(setting("wifi_only", account), true);
    }

    public static void setWifiOnly(int account, boolean enabled) {
        preferences().edit().putBoolean(setting("wifi_only", account), enabled).apply();
    }

    public static String lastResult(int account) {
        return preferences().getString(setting("last_result", account), "No video attempt yet");
    }

    public static void noteResult(int account, String status) {
        preferences().edit().putString(setting("last_result", account), status).apply();
    }

    private static synchronized void markSiteCompleted(int account, String key) {
        Set<String> seen = new HashSet<>(preferences().getStringSet("seen", Collections.emptySet()));
        if (seen.size() >= 1000) seen.clear();
        seen.add(fingerprint(key));
        preferences().edit().putStringSet("seen", seen)
                .putInt(setting("queued", account), queuedCount(account) + 1).apply();
    }

    public static int queuedCount(int account) {
        return preferences().getInt(setting("queued", account), 0);
    }

    public static boolean strictModeRequested() {
        return ApplicationLoader.applicationContext
                .getSharedPreferences("mainconfig", Context.MODE_PRIVATE)
                .getBoolean("ult_strict_native_proxy", false);
    }

    @Override
    public void didReceivedNotification(int id, int eventAccount, Object... args) {
        if (id != NotificationCenter.didReceiveNewMessages || eventAccount != account
                || !isEnabled(account) || strictModeRequested()
                || args.length < 2 || !(args[0] instanceof Long)
                || !(args[1] instanceof ArrayList)) return;

        long dialogId = (Long) args[0];
        if (DialogObject.isEncryptedDialog(dialogId)) return;
        for (Object raw : (ArrayList<?>) args[1]) {
            if (!(raw instanceof MessageObject)) continue;
            MessageObject message = (MessageObject) raw;
            if (message.messageOwner == null || message.messageOwner.noforwards
                    || message.isSponsored()) continue;
            List<String> urls = new ArrayList<>(
                    UltVideoLinkRules.findUrls(message.messageOwner.message));
            if (message.messageOwner.entities != null) {
                for (TLRPC.MessageEntity entity : message.messageOwner.entities) {
                    if (entity instanceof TLRPC.TL_messageEntityTextUrl) {
                        String target = ((TLRPC.TL_messageEntityTextUrl) entity).url;
                        if (UltVideoLinkRules.isAllowedUrl(target) && !urls.contains(target)
                                && urls.size() < 3) urls.add(target);
                    }
                }
            }
            for (String url : urls) {
                String key = account + ":" + dialogId + ":" + message.getId() + ":" + url;
                if (isQueued(key) || !IN_FLIGHT.add(key)) continue;
                EXECUTOR.execute(() -> {
                    try {
                        if (!isEnabled(account) || strictModeRequested()
                                || !networkAllowed(account)) return;
                        if (UltVideoLinkRules.isSiteUrl(url)) {
                            noteResult(account, "Starting on-device site extractor");
                            if (UltVideoSiteDownloader.download(
                                    ApplicationLoader.applicationContext, account, url)) {
                                markSiteCompleted(account, key);
                            }
                            return;
                        }
                        Probe probe = probe(url);
                        if (probe == null || !isEnabled(account) || strictModeRequested()
                                || !networkAllowed(account) || isQueued(key)) return;
                        enqueue(probe, key);
                        noteResult(account, "Direct video queued in Downloads/Ultgram");
                    } catch (Exception ignored) {
                        // No URL/token logging. Unsupported/private resources are skipped.
                    } finally {
                        IN_FLIGHT.remove(key);
                    }
                });
            }
        }
    }

    public static boolean networkAllowed(int account) {
        ConnectivityManager cm = (ConnectivityManager) ApplicationLoader.applicationContext
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return false;
        NetworkInfo network = cm.getActiveNetworkInfo();
        return network != null && network.isConnected()
                && (!isWifiOnly(account) || !cm.isActiveNetworkMetered());
    }

    private static boolean publicHost(String address) {
        if (!UltVideoLinkRules.isAllowedUrl(address)) return false;
        try {
            String host = new URL(address).getHost();
            for (InetAddress resolved : InetAddress.getAllByName(host)) {
                if (resolved.isAnyLocalAddress() || resolved.isLoopbackAddress()
                        || resolved.isLinkLocalAddress() || resolved.isSiteLocalAddress()
                        || resolved.isMulticastAddress()) return false;
                byte[] b = resolved.getAddress();
                if (b.length == 4) {
                    int first = b[0] & 255, second = b[1] & 255;
                    if (first == 0 || first >= 224 || first == 100 && second >= 64 && second <= 127
                            || first == 198 && (second == 18 || second == 19)) return false;
                } else if (b.length == 16 && ((b[0] & 0xfe) == 0xfc)) return false;
            }
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    /** Cancel incomplete owned external transfers on strict-mode activation. */
    public static synchronized void cancelPendingForStrict() {
        if (!strictModeRequested()) return;
        UltVideoSiteDownloader.cancelAll();
        Context context = ApplicationLoader.applicationContext;
        DownloadManager dm = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
        if (dm == null) return;
        Set<String> owned = new HashSet<>(preferences().getStringSet("owned_ids", Collections.emptySet()));
        for (String raw : owned) {
            try {
                long id = Long.parseLong(raw);
                boolean completed = false;
                try (android.database.Cursor cursor =
                             dm.query(new DownloadManager.Query().setFilterById(id))) {
                    if (cursor != null && cursor.moveToFirst()) {
                        int status = cursor.getInt(
                                cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS));
                        completed = status == DownloadManager.STATUS_SUCCESSFUL
                                || status == DownloadManager.STATUS_FAILED;
                    }
                }
                if (!completed) dm.remove(id);
            } catch (Exception ignored) {
                // Do not log URL contents. Device QA must verify cancellation.
            }
        }
        preferences().edit().remove("owned_ids").apply();
    }

    private static final class Probe {
        final String url, mime;
        Probe(String url, String mime) { this.url = url; this.mime = mime; }
    }

    private static long declaredSize(HttpURLConnection connection, int response) {
        if (response == HttpURLConnection.HTTP_PARTIAL) {
            String range = connection.getHeaderField("Content-Range");
            if (range == null || !range.contains("/")) return -1;
            try { return Long.parseLong(range.substring(range.lastIndexOf('/') + 1).trim()); }
            catch (NumberFormatException ignored) { return -1; }
        }
        return connection.getContentLengthLong();
    }

    private static Probe probe(String original) throws Exception {
        String current = original;
        Set<String> visited = new HashSet<>();
        for (int step = 0; step < 5; step++) {
            if (!publicHost(current) || !visited.add(current)) return null;
            URL target = new URL(current);
            HttpURLConnection connection = (HttpURLConnection) target.openConnection();
            try {
                connection.setInstanceFollowRedirects(false);
                connection.setConnectTimeout(8000);
                connection.setReadTimeout(8000);
                connection.setRequestProperty("Range", "bytes=0-65535");
                connection.setRequestProperty("Accept", "video/*,text/html;q=0.8,*/*;q=0.1");
                int code = connection.getResponseCode();
                if (code == 301 || code == 302 || code == 303 || code == 307 || code == 308) {
                    String location = connection.getHeaderField("Location");
                    if (location == null) return null;
                    current = new URL(target, location).toExternalForm();
                    continue;
                }
                if (code != 200 && code != 206) return null;
                String type = connection.getContentType();
                type = type == null ? "" : type.split(";", 2)[0].trim().toLowerCase(Locale.US);
                if (type.startsWith("video/") || type.equals("application/octet-stream")
                        && UltVideoLinkRules.hasVideoExtension(current)) {
                    long declared = declaredSize(connection, code);
                    if (declared <= 0 || declared > MAX_BYTES) return null;
                    return new Probe(current, type.equals("application/octet-stream")
                            ? "video/mp4" : type);
                }
                if (!type.equals("text/html") && !type.equals("application/xhtml+xml")) return null;
                ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                try (InputStream stream = connection.getInputStream()) {
                    byte[] buffer = new byte[4096];
                    while (bytes.size() < 65536) {
                        int read = stream.read(buffer, 0, Math.min(buffer.length, 65536 - bytes.size()));
                        if (read == -1) break;
                        bytes.write(buffer, 0, read);
                    }
                }
                String discovered = UltVideoLinkRules.htmlVideoUrl(
                        new String(bytes.toByteArray(), StandardCharsets.UTF_8));
                if (discovered == null) return null;
                current = discovered;
            } finally {
                connection.disconnect();
            }
        }
        return null;
    }

    private static String fingerprint(String key) {
        return Integer.toHexString(key.hashCode()) + ":" + key.length();
    }

    private static boolean isQueued(String key) {
        return preferences().getStringSet("seen", Collections.emptySet()).contains(fingerprint(key));
    }

    private void enqueue(Probe video, String key) {
        synchronized (UltVideoAutoSave.class) {
        Context context = ApplicationLoader.applicationContext;
        DownloadManager dm = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
        if (dm == null) return;
        if (strictModeRequested() || !isEnabled(account) || !networkAllowed(account)) return;
        String extension = video.mime.contains("webm") ? ".webm"
                : video.mime.contains("quicktime") ? ".mov"
                : video.mime.contains("3gpp") ? ".3gp"
                : video.mime.contains("matroska") ? ".mkv" : ".mp4";
        String name = "video_" + System.currentTimeMillis() + "_"
                + Integer.toHexString(video.url.hashCode()) + extension;
        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(video.url))
                .setTitle("Ultgram video")
                .setDescription("Auto-saved video link")
                .setMimeType(video.mime)
                .setAllowedOverMetered(!isWifiOnly(account))
                .setAllowedOverRoaming(false)
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "Ultgram/" + name);
        long downloadId = dm.enqueue(request);
        if (downloadId <= 0) return;
        synchronized (UltVideoAutoSave.class) {
            Set<String> seen = new HashSet<>(preferences().getStringSet("seen", Collections.emptySet()));
            if (seen.size() >= 1000) seen.clear();
            seen.add(fingerprint(key));
            Set<String> owned = new HashSet<>(
                    preferences().getStringSet("owned_ids", Collections.emptySet()));
            owned.add(Long.toString(downloadId));
            preferences().edit().putStringSet("seen", seen)
                    .putStringSet("owned_ids", owned)
                    .putInt(setting("queued", account), queuedCount(account) + 1).apply();
        }
        }
    }
}

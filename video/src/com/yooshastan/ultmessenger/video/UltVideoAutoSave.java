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
    // Single worker prevents concurrent transfers from interleaving diagnostics.
    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();
    private static final Set<String> IN_FLIGHT = Collections.synchronizedSet(new HashSet<>());
    private static final UltVideoAutoSave[] OBSERVERS = new UltVideoAutoSave[UserConfig.MAX_ACCOUNT_COUNT];
    private final int account;
    private static final java.util.concurrent.ConcurrentHashMap<String, SiteJob> SITE_JOBS = new java.util.concurrent.ConcurrentHashMap<>();
    private static final int MAX_SITE_JOBS = 24;
    private static final class SiteJob {
        final int account;
        final String key, source;
        final UltMediaOptions options;
        final java.util.concurrent.atomic.AtomicBoolean cancelled = new java.util.concurrent.atomic.AtomicBoolean();
        volatile boolean running;
        SiteJob(int account, String key, String source, UltMediaOptions options) {
            this.account = account; this.key = key; this.source = source; this.options = options;
        }
    }
    // One retry ticket per account, memory only; never persist remote URLs.
    private static final java.util.concurrent.ConcurrentHashMap<Integer, RetryTicket> RETRIES =
            new java.util.concurrent.ConcurrentHashMap<>();
    private static final long RETRY_TTL_MS = 15L * 60 * 1000;
    private static final class RetryTicket {
        final String url, key;
        final UltMediaOptions options;
        final long ownerId, createdAt;
        RetryTicket(String url, String key, UltMediaOptions options, long ownerId) {
            this.url = url; this.key = key; this.options = options; this.ownerId = ownerId;
            createdAt = android.os.SystemClock.elapsedRealtime();
        }
    }

    private UltVideoAutoSave(int account) { this.account = account; }

    public static void install() {
        AndroidUtilities.runOnUIThread(() -> {
            for (int a = 0; a < OBSERVERS.length; a++) {
                if (OBSERVERS[a] != null) continue;
                OBSERVERS[a] = new UltVideoAutoSave(a);
                NotificationCenter.getInstance(a).addObserver(OBSERVERS[a], NotificationCenter.didReceiveNewMessages);
                NotificationCenter.getInstance(a).addObserver(OBSERVERS[a], NotificationCenter.messageReceivedByServer);
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
        if (!enabled) {
            RetryTicket ticket = RETRIES.get(account);
            if (ticket != null && !ticket.options.manual) RETRIES.remove(account, ticket);
            for (SiteJob job : SITE_JOBS.values()) if (job.account == account && !job.options.manual) job.cancelled.set(true);
        }
    }

    /** A separate, explicitly opt-in setting; never enabled by default. */
    public static boolean isPostToChat(int account) {
        return preferences().getBoolean(setting("post_to_chat", account), false);
    }

    public static void setPostToChat(int account, boolean enabled) {
        preferences().edit().putBoolean(setting("post_to_chat", account), enabled).apply();
    }

    public static boolean canAutoPost(int account) {
        return isEnabled(account) && isPostToChat(account)
                && !strictModeRequested() && networkAllowed(account);
    }

    public static boolean isWifiOnly(int account) {
        return preferences().getBoolean(setting("wifi_only", account), true);
    }

    public static void setWifiOnly(int account, boolean enabled) {
        preferences().edit().putBoolean(setting("wifi_only", account), enabled).apply();
    }

    public static boolean audioOnly(int account) {
        return preferences().getBoolean(setting("audio_only", account), false);
    }

    public static void setAudioOnly(int account, boolean value) {
        preferences().edit().putBoolean(setting("audio_only", account), value)
                .putBoolean(setting("post_to_chat", account), false).apply();
    }

    public static String defaultAudioFormat(int account) {
        return new UltMediaOptions(true, true, 0, preferences().getString(setting("audio_format", account), "m4a")).audioFormat;
    }
    public static void setDefaultAudioFormat(int account, String format) {
        preferences().edit().putString(setting("audio_format", account),
                new UltMediaOptions(true, true, 0, format).audioFormat).apply();
    }

    public static boolean metadataProbeEnabled(int account) {
        return preferences().getBoolean(setting("metadata_probe", account), false);
    }

    public static void setMetadataProbeEnabled(int account, boolean value) {
        preferences().edit().putBoolean(setting("metadata_probe", account), value).apply();
    }

    private static RetryTicket retryTicket(int account) {
        RetryTicket ticket = RETRIES.get(account);
        if (ticket == null) return null;
        if (android.os.SystemClock.elapsedRealtime() - ticket.createdAt >= RETRY_TTL_MS
                || ticket.ownerId == 0 || ticket.ownerId != UserConfig.getInstance(account).getClientUserId()
                || isQueued(ticket.key)) {
            RETRIES.remove(account, ticket);
            return null;
        }
        return ticket;
    }

    public static boolean canRetry(int account) {
        RetryTicket ticket = retryTicket(account);
        return ticket != null && !IN_FLIGHT.contains(ticket.key) && requestBlockReason(account, ticket.options.manual, ticket.ownerId, null) == null
                && !UltExtractorMaintenance.busy() && !UltVideoSiteDownloader.hasActive(account);
    }

    /** Explicit retry is local-only, never posts another message to the chat. */
    public static void retryLastSite(int account) {
        RetryTicket ticket = retryTicket(account);
        if (ticket == null || !canRetry(account)) return;
        scheduleSite(account, ticket.url, ticket.key, 0L, null, false, ticket.options, ticket.ownerId);
    }

    public static String siteQueueSummary(int account) {
        StringBuilder text = new StringBuilder();
        for (SiteJob job : SITE_JOBS.values()) {
            if (job.account != account) continue;
            if (text.length() > 0) text.append('\n');
            text.append(job.cancelled.get() ? "Cancelling" : job.running ? "Active" : "Queued")
                    .append(" · ").append(job.source).append(" · ").append(job.options.variant())
                    .append(job.options.manual ? " · Manual" : " · Auto");
        }
        return text.length() == 0 ? "No queued site downloads" : text.toString();
    }

    public static boolean hasSiteJobs(int account) {
        for (SiteJob job : SITE_JOBS.values()) if (job.account == account && !job.cancelled.get()) return true;
        return false;
    }

    public static void cancelSiteJobs(int account) {
        for (SiteJob job : SITE_JOBS.values()) if (job.account == account) job.cancelled.set(true);
        UltVideoSiteDownloader.cancelAccount(account);
    }

    public static String requestBlockReason(int account, boolean manual, long ownerId,
                                             java.util.concurrent.atomic.AtomicBoolean cancelled) {
        if (cancelled != null && cancelled.get()) return "Cancelled";
        if (ownerId == 0 || ownerId != UserConfig.getInstance(account).getClientUserId()) return "Account changed";
        if (strictModeRequested()) return "Native strict proxy enabled";
        if (!manual && !isEnabled(account)) return "Automatic downloads disabled";
        return networkBlockReason(account);
    }

    private static String siteKey(int account, long dialogId, int messageId, String url, UltMediaOptions options) {
        return account + ":" + dialogId + ":" + messageId + ":" + UltVideoLinkRules.extractionUrl(url) + ":" + options.variant();
    }

    /** A bounded common queue for manual and automatic website jobs. */
    private static synchronized String scheduleSite(int account, String url, String key, long dialogId,
            TLRPC.Message original, boolean outgoingLink, UltMediaOptions options, long ownerId) {
        String blocked = requestBlockReason(account, options.manual, ownerId, null);
        if (blocked != null) return "Not queued: " + blocked;
        if (isQueued(key)) return "Already saved for this message and format";
        if (SITE_JOBS.size() >= MAX_SITE_JOBS) return "Site queue is full";
        if (!IN_FLIGHT.add(key)) return "Already queued";
        SiteJob job = new SiteJob(account, key, UltVideoLinkRules.sourceCategory(url), options);
        SITE_JOBS.put(key, job);
        EXECUTOR.execute(() -> {
            try {
                job.running = true;
                String reason = requestBlockReason(account, options.manual, ownerId, job.cancelled);
                if (reason != null) { noteResult(account, "Not started: " + reason); return; }
                noteAttempt(account, url);
                RETRIES.remove(account);
                noteResult(account, "Starting on-device site extractor");
                if (UltVideoSiteDownloader.download(ApplicationLoader.applicationContext, account,
                        url, dialogId, original, outgoingLink, options, ownerId, job.cancelled)) {
                    markSiteCompleted(account, key);
                } else if (!job.cancelled.get() && ownerId == UserConfig.getInstance(account).getClientUserId()
                        && (options.manual || isEnabled(account))) {
                    RETRIES.put(account, new RetryTicket(url, key, options, ownerId));
                }
            } catch (Exception ignored) { noteResult(account, "Failed to process site media"); }
            finally { SITE_JOBS.remove(key, job); IN_FLIGHT.remove(key); }
        });
        return "Queued " + (options.audio ? options.audioFormat.toUpperCase(Locale.US) : "video");
    }

    public static String queueManualSite(int account, long dialogId, int messageId, String url, UltMediaOptions options, long ownerId) {
        if (!options.manual || messageId <= 0 || DialogObject.isEncryptedDialog(dialogId)
                || !UltVideoLinkRules.isManualMediaUrl(url)) return "Unsupported or protected message";
        return scheduleSite(account, url, siteKey(account, dialogId, messageId, url, options),
                0L, null, false, options, ownerId);
    }

    public static int maxVideoHeight(int account) {
        int value = preferences().getInt(setting("site_max_height", account), 0);
        return value == 480 || value == 720 || value == 1080 ? value : 0;
    }

    public static void setMaxVideoHeight(int account, int height) {
        int value = height == 480 || height == 720 || height == 1080 ? height : 0;
        preferences().edit().putInt(setting("site_max_height", account), value).apply();
    }

    public static int siteLimitMiB(int account) {
        int value = preferences().getInt(setting("site_limit_mib", account), 100);
        return value == 250 || value == 500 || value == 1024 ? value : 100;
    }

    public static void setSiteLimitMiB(int account, int value) {
        preferences().edit().putInt(setting("site_limit_mib", account),
                value == 250 || value == 500 || value == 1024 ? value : 100).apply();
    }

    /** Only confirmed exports, bounded local metadata, no remote links or chat IDs. */
    public static synchronized void recordSiteSave(int account, String name, Uri uri, long bytes) {
        try {
            org.json.JSONArray previous = siteHistory(account);
            org.json.JSONObject row = new org.json.JSONObject();
            row.put("name", name);
            row.put("uri", uri.toString());
            row.put("bytes", bytes);
            row.put("saved", System.currentTimeMillis());
            row.put("source", lastSourceCategory(account));
            org.json.JSONArray next = new org.json.JSONArray();
            next.put(row);
            for (int i = 0; i < Math.min(previous.length(), 29); i++) {
                next.put(previous.getJSONObject(i));
            }
            preferences().edit().putString(setting("site_history", account), next.toString()).apply();
        } catch (Exception ignored) {
            // History must not turn a confirmed file export into a failed download.
        }
    }

    public static org.json.JSONArray siteHistory(int account) {
        try {
            return new org.json.JSONArray(preferences().getString(
                    setting("site_history", account), "[]"));
        } catch (org.json.JSONException ignored) {
            return new org.json.JSONArray();
        }
    }

    public static synchronized void clearSiteHistory(int account) {
        preferences().edit().remove(setting("site_history", account)).apply();
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
                .putInt(setting("queued", account), queuedCount(account) + 1)
                .putInt(setting("site_saved_count", account),
                        completedSiteSaves(account) + 1).apply();
    }

    public static int queuedCount(int account) {
        return preferences().getInt(setting("queued", account), 0);
    }

    /** Only confirmed site MediaStore exports; counter starts new in Beta 3.4.4. */
    public static int completedSiteSaves(int account) {
        return preferences().getInt(setting("site_saved_count", account), 0);
    }

    public static String lastSourceCategory(int account) {
        return preferences().getString(setting("last_source", account),
                "No source recorded");
    }

    public static long lastAttemptMillis(int account) {
        return preferences().getLong(setting("last_attempt_at", account), 0L);
    }

    private static void noteAttempt(int account, String url) {
        preferences().edit()
                .putString(setting("last_source", account), UltVideoLinkRules.sourceCategory(url))
                .putLong(setting("last_attempt_at", account),
                        System.currentTimeMillis()).apply();
    }

    public static boolean strictModeRequested() {
        return ApplicationLoader.applicationContext
                .getSharedPreferences("mainconfig", Context.MODE_PRIVATE)
                .getBoolean("ult_strict_native_proxy", false);
    }

    @Override
    public void didReceivedNotification(int id, int eventAccount, Object... args) {
        if (eventAccount != account || !isEnabled(account) || strictModeRequested()) return;
        if (id == NotificationCenter.didReceiveNewMessages) {
            if (args.length < 2 || !(args[0] instanceof Long)
                    || !(args[1] instanceof ArrayList)) return;
            long dialogId = (Long) args[0];
            if (DialogObject.isEncryptedDialog(dialogId)) return;
            for (Object raw : (ArrayList<?>) args[1]) {
                if (!(raw instanceof MessageObject)) continue;
                MessageObject message = (MessageObject) raw;
                if (message.messageOwner == null || message.messageOwner.noforwards
                        || message.isSponsored()) continue;
                // Wait until the server acknowledges our own messages; avoids
                // downloading once for a local temporary ID and again for server ID.
                if (message.messageOwner.out) continue;
                queueLinks(dialogId, message.messageOwner, false);
            }
        } else if (id == NotificationCenter.messageReceivedByServer) {
            // Telegram dispatches outgoing server-confirmed messages here.
            // Example arguments: oldId, newId, TLRPC.Message, dialogId, ...
            if (args.length < 4 || !(args[2] instanceof TLRPC.Message)
                    || !(args[3] instanceof Long)) return;
            TLRPC.Message sent = (TLRPC.Message) args[2];
            long dialogId = (Long) args[3];
            if (DialogObject.isEncryptedDialog(dialogId) || sent.noforwards) return;
            queueLinks(dialogId, sent, true);
        }
    }

    private void queueLinks(long dialogId, TLRPC.Message owner, boolean outgoingLink) {
        if (owner == null) return;
        List<String> urls = new ArrayList<>(UltVideoLinkRules.findUrls(owner.message));
        if (owner.entities != null) {
            for (TLRPC.MessageEntity entity : owner.entities) {
                if (entity instanceof TLRPC.TL_messageEntityTextUrl) {
                    String target = ((TLRPC.TL_messageEntityTextUrl) entity).url;
                    if (UltVideoLinkRules.isAllowedUrl(target) && !urls.contains(target)
                            && urls.size() < 3) urls.add(target);
                }
            }
        }
        for (String url : urls) {
            // Ordinary webpages are not contacted unless explicitly opted in.
            if (!UltVideoLinkRules.isSiteUrl(url) && !UltVideoLinkRules.hasVideoExtension(url)
                    && !metadataProbeEnabled(account)) continue;
            final boolean audio = audioOnly(account);
            if (audio && !UltVideoLinkRules.isSiteUrl(url)) continue;
            final long ownerId = UserConfig.getInstance(account).getClientUserId();
            if (UltVideoLinkRules.isSiteUrl(url)) {
                UltMediaOptions options = new UltMediaOptions(false, audio, maxVideoHeight(account), defaultAudioFormat(account));
                scheduleSite(account, url, siteKey(account, dialogId, owner.id, url, options), dialogId, owner, outgoingLink, options, ownerId);
                continue;
            }
            String key = account + ":" + dialogId + ":" + owner.id + ":" + url;
            if (isQueued(key) || !IN_FLIGHT.add(key)) continue;
            EXECUTOR.execute(() -> {
                try {
                    if (ownerId == 0 || ownerId != UserConfig.getInstance(account).getClientUserId()) return;
                    noteAttempt(account, url);
                    if (!isEnabled(account)) {
                        noteResult(account, "Skipped: automatic downloads OFF");
                        return;
                    }
                    if (strictModeRequested()) {
                        noteResult(account, "Paused by native strict proxy");
                        return;
                    }
                    String networkReason = networkBlockReason(account);
                    if (networkReason != null) {
                        noteResult(account, "Skipped: " + networkReason);
                        return;
                    }
                    Probe probe = probe(url, account);
                    if (probe == null || !isEnabled(account) || strictModeRequested()
                            || !networkAllowed(account) || isQueued(key)) {
                        noteResult(account, "No public direct video or OG metadata on this webpage; site extractor was not used");
                        return;
                    }
                    enqueue(probe, key);
                    noteResult(account, "Direct video queued in Downloads/Ultgram");
                } catch (Exception ignored) {
                    noteResult(account, "Failed to process video URL");
                    // Never log chat URLs or query tokens.
                } finally {
                    IN_FLIGHT.remove(key);
                }
            });
        }
    }

    /** Null means the existing network policy permits work. No addresses or identifiers. */
    public static String networkBlockReason(int account) {
        ConnectivityManager cm = (ConnectivityManager) ApplicationLoader.applicationContext
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return "Network service unavailable";
        NetworkInfo network = cm.getActiveNetworkInfo();
        if (network == null || !network.isConnected()) return "Network disconnected";
        if (isWifiOnly(account) && cm.isActiveNetworkMetered()) return "Network metered; unmetered-only enabled";
        return null;
    }

    public static boolean networkAllowed(int account) {
        return networkBlockReason(account) == null;
    }

    public static String siteBlockReason(int account) {
        if (strictModeRequested()) return "Native strict proxy enabled";
        if (!isEnabled(account)) return "Automatic downloads disabled";
        return networkBlockReason(account);
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
        for (SiteJob job : SITE_JOBS.values()) job.cancelled.set(true);
        UltExtractorMaintenance.cancelForStrict();
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

    private static Probe probe(String original, int account) throws Exception {
        String current = original;
        Set<String> visited = new HashSet<>();
        for (int step = 0; step < 5; step++) {
            if (siteBlockReason(account) != null || !publicHost(current) || !visited.add(current)) return null;
            URL target = new URL(current);
            HttpURLConnection connection = (HttpURLConnection) target.openConnection();
            try {
                connection.setInstanceFollowRedirects(false);
                connection.setConnectTimeout(8000);
                connection.setReadTimeout(8000);
                connection.setRequestProperty("Range", "bytes=0-65535");
                connection.setRequestProperty("Accept", "video/*,text/html;q=0.8,*/*;q=0.1");
                if (siteBlockReason(account) != null) return null;
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
                if (!metadataProbeEnabled(account)) return null;
                if (!type.equals("text/html") && !type.equals("application/xhtml+xml")) return null;
                ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                try (InputStream stream = connection.getInputStream()) {
                    byte[] buffer = new byte[4096];
                    while (bytes.size() < 65536) {
                        if (siteBlockReason(account) != null) return null;
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

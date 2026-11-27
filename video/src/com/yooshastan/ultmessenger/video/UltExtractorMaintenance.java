package com.yooshastan.ultmessenger.video;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.SystemClock;
import com.yausername.youtubedl_android.YoutubeDL;
import org.json.JSONObject;
import org.json.JSONArray;
import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantLock;
import javax.net.ssl.HttpsURLConnection;

/** App-wide extractor maintenance. No chat URLs, credentials or cookies are sent.
 * Network cancellation is best effort, not a device-wide VPN kill switch. */
public final class UltExtractorMaintenance {
    private static final ReentrantLock LOCK = new ReentrantLock();
    private static final ExecutorService WORK = Executors.newSingleThreadExecutor();
    private static volatile boolean busy, cancelled, remote;
    private static volatile HttpsURLConnection activeConnection;
    private static volatile String status = "Ready; checks are optional";
    private static volatile String available = "";
    private static volatile String installed = "";
    private static volatile boolean runtimeInitialized;
    private static volatile boolean runtimeLinkageFailed;
    private static boolean baselineReady;
    public static boolean runtimeInitialized() { return runtimeInitialized; }
    public static void recordDetectedVersion(String value) {
        installed = UltExtractorPolicy.validVersion(value) ? value : "Unknown (local version probe failed)";
    }
    public static void markInitialized() { runtimeInitialized = true; }
    private static final long DAY = 24L * 60 * 60 * 1000;
    private UltExtractorMaintenance() {}
    public static boolean beginDownload() { return LOCK.tryLock(); }
    public static void endDownload() { LOCK.unlock(); }
    public static boolean busy() { return busy; }
    public static String status() { return status; }
    public static String available() { return available; }
    public static String installed() { return installed; }
    private static SharedPreferences prefs(Context c) {
        return c.getSharedPreferences("ult_extractor_maintenance", Context.MODE_PRIVATE);
    }
    public static boolean nightly(Context c) { return prefs(c).getBoolean("nightly", false); }
    public static boolean automatic(Context c) { return prefs(c).getBoolean("auto_check", false); }
    public static boolean unmetered(Context c) { return prefs(c).getBoolean("unmetered", true); }
    public static synchronized void setNightly(Context c, boolean value) {
        if (busy) return;
        prefs(c).edit().putBoolean("nightly", value).remove("last_check").apply();
        available = "";
    }
    public static void setAutomatic(Context c, boolean value) {
        prefs(c).edit().putBoolean("auto_check", value).apply();
    }
    public static void setUnmetered(Context c, boolean value) {
        prefs(c).edit().putBoolean("unmetered", value).apply();
    }
    public static void cancelForStrict() {
        if (remote) {
            cancelled = true;
            HttpsURLConnection c = activeConnection;
            if (c != null) c.disconnect();
        }
    }
    private static File dir(Context c) {
        return new File(c.getNoBackupFilesDir(), "youtubedl-android/yt-dlp");
    }
    public static boolean hasBackup(Context c) { return new File(dir(c), "ult-previous").isFile(); }
    public static void maybeCheck(Context c) {
        if (runtimeLinkageFailed) return;
        long last = prefs(c).getLong("last_check", 0);
        if (automatic(c) && !busy && System.currentTimeMillis() - last >= DAY
                && networkBlock(c) == null) start(c, "check");
        else if (!busy && installed.isEmpty()) start(c, "inspect");
    }
    public static synchronized void start(Context context, String action) {
        if (busy) return;
        if (runtimeLinkageFailed) {
            status = "Embedded extractor unavailable; restart after updating Ultgram";
            return;
        }
        if (Build.VERSION.SDK_INT < 29) { status = "Maintenance requires Android 10+"; return; }
        if (!("check".equals(action) || "update".equals(action) || "rollback".equals(action)
                || "bundled".equals(action) || "verify".equals(action) || "inspect".equals(action))) return;
        Context c = context.getApplicationContext();
        busy = true;
        cancelled = false;
        remote = "check".equals(action) || "update".equals(action);
        status = "Preparing extractor maintenance";
        final boolean channel = nightly(c);
        WORK.execute(() -> {
            boolean locked = LOCK.tryLock();
            try {
                if (!locked) { status = "Finish active site download before maintenance"; return; }
                if (remote) checkNetwork(c);
                run(c, action, channel);
            } catch (LinkageError failure) {
                // Failed class initialization can poison the class for this process.
                runtimeLinkageFailed = true;
                runtimeInitialized = false;
                status = "Embedded extractor initialization failed; update Ultgram and restart";
            } catch (Exception ignored) {
                // Never expose raw server responses, signed asset URLs or paths.
                String blocked = remote ? networkBlock(c) : null;
                status = cancelled ? "Cancelled by network protection"
                        : blocked != null ? blocked : "Maintenance failed; installed extractor not intentionally changed";
            } finally {
                if (locked) LOCK.unlock();
                activeConnection = null;
                remote = false;
                busy = false;
            }
        });
    }
    private static String networkBlock(Context c) {
        if (UltVideoAutoSave.strictModeRequested()) return "Blocked: native strict proxy enabled";
        ConnectivityManager cm = (ConnectivityManager)c.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo n = cm == null ? null : cm.getActiveNetworkInfo();
        if (n == null || !n.isConnected()) return "Blocked: network disconnected";
        if (unmetered(c) && cm.isActiveNetworkMetered()) return "Blocked: metered network";
        return null;
    }
    private static void checkNetwork(Context c) throws IOException {
        if (cancelled || networkBlock(c) != null) throw new IOException("Network policy");
    }
    private static boolean permitted(URL u) {
        String host = u.getHost().toLowerCase(Locale.US);
        return "https".equals(u.getProtocol()) && u.getUserInfo() == null
                && (u.getPort() == -1 || u.getPort() == 443)
                && (host.equals("api.github.com") || host.equals("github.com")
                    || host.equals("release-assets.githubusercontent.com")
                    || host.equals("objects.githubusercontent.com")
                    || host.equals("github-releases.githubusercontent.com"));
    }
    private static void fetch(Context c, String address, File target, long max) throws Exception {
        URL url = new URL(address);
        long deadline = SystemClock.elapsedRealtime() + 120000;
        for (int redirects = 0; redirects <= 5; redirects++) {
            checkNetwork(c);
            if (!permitted(url)) throw new IOException("Unexpected asset host");
            HttpsURLConnection conn = (HttpsURLConnection)url.openConnection();
            activeConnection = conn;
            try {
                checkNetwork(c);
                conn.setInstanceFollowRedirects(false);
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(10000);
                conn.setUseCaches(false);
                conn.setRequestProperty("User-Agent", "Ultgram-Extractor-Maintenance");
                int code = conn.getResponseCode();
                if (code == 301 || code == 302 || code == 303 || code == 307 || code == 308) {
                    String location = conn.getHeaderField("Location");
                    if (location == null) throw new IOException("Invalid redirect");
                    url = new URL(url, location);
                    continue;
                }
                if (code != 200 || conn.getContentLengthLong() > max) throw new IOException("Asset unavailable");
                try (InputStream in = conn.getInputStream(); FileOutputStream out = new FileOutputStream(target)) {
                    byte[] bytes = new byte[32768]; long count = 0;
                    for (;;) {
                        checkNetwork(c);
                        if (SystemClock.elapsedRealtime() >= deadline) throw new IOException("Asset timeout");
                        int n = in.read(bytes);
                        if (n == -1) break;
                        count += n;
                        if (count > max) throw new IOException("Asset too large");
                        out.write(bytes, 0, n);
                    }
                    if (count == 0) throw new IOException("Empty asset");
                    out.getFD().sync();
                }
                checkNetwork(c);
                return;
            } finally { activeConnection = null; conn.disconnect(); }
        }
        throw new IOException("Too many redirects");
    }
    private static String read(File f, long max) throws IOException {
        if (f.length() > max) throw new IOException("Output too large");
        try (InputStream in = new FileInputStream(f); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] buf = new byte[4096]; int n;
            while ((n = in.read(buf)) != -1) {
                if (out.size() + n > max) throw new IOException("Output too large");
                out.write(buf, 0, n);
            }
            return new String(out.toByteArray(), StandardCharsets.UTF_8);
        }
    }
    private static String releaseDigest(JSONObject release, String asset, String expectedUrl) throws Exception {
        JSONArray assets = release.getJSONArray("assets");
        String result = null;
        for (int i=0; i<assets.length(); i++) {
            JSONObject item = assets.getJSONObject(i);
            if (!asset.equals(item.optString("name"))) continue;
            String hash = item.optString("digest");
            if (result != null || !expectedUrl.equals(item.optString("browser_download_url"))
                    || !hash.startsWith("sha256:") || !UltExtractorPolicy.validHash(hash.substring(7)))
                throw new IOException("Invalid release asset metadata");
            result = hash.substring(7);
        }
        if (result == null) throw new IOException("Missing official asset digest");
        return result;
    }
    /** Run the exact staged zipapp with the library's pinned local Python runtime,
     * using only --version. Candidate is never run before hash verification. */
    private static String probe(Context c, File binary, File output) throws Exception {
        File base = new File(c.getNoBackupFilesDir(), "youtubedl-android/packages");
        File py = new File(base, "python/usr");
        String nativeDir = c.getApplicationInfo().nativeLibraryDir;
        ProcessBuilder builder = new ProcessBuilder(new File(nativeDir, "libpython.so").getAbsolutePath(),
                binary.getAbsolutePath(), "--ignore-config", "--version");
        builder.environment().put("LD_LIBRARY_PATH", new File(py, "lib").getAbsolutePath());
        builder.environment().put("PYTHONHOME", py.getAbsolutePath());
        builder.environment().put("HOME", py.getAbsolutePath());
        builder.environment().put("SSL_CERT_FILE", new File(py, "etc/tls/cert.pem").getAbsolutePath());
        builder.environment().put("TMPDIR", c.getCacheDir().getAbsolutePath());
        builder.redirectErrorStream(true).redirectOutput(output);
        Process p = builder.start();
        try {
            if (!p.waitFor(20, TimeUnit.SECONDS)) throw new IOException("Version probe timed out");
            if (p.exitValue() != 0) throw new IOException("Version probe failed");
            String v = read(output, 4096).trim();
            if (!UltExtractorPolicy.validVersion(v)) throw new IOException("Invalid version");
            return v;
        } finally { if (p.isAlive()) p.destroyForcibly(); }
    }
    /** Called only under the shared maintenance/download lock. Offline and atomic. */
    static void ensureBaseline(Context c) throws Exception {
        if (baselineReady) return;
        File d = dir(c), live = new File(d, "yt-dlp");
        File stage = new File(d, "ult-baseline-stage"), output = new File(d, "ult-baseline-probe");
        try {
            String current;
            try { current = probe(c, live, output); } catch (Exception ignored) { current = ""; }
            if (UltExtractorPolicy.meetsBaseline(current)) {
                recordDetectedVersion(current);
                baselineReady = true;
                return;
            }
            try (InputStream in = c.getResources().openRawResource(org.telegram.messenger.R.raw.ult_ytdlp_baseline)) {
                UltExtractorPolicy.copy(in, stage, UltExtractorPolicy.MAX_BINARY);
            }
            if (!UltExtractorPolicy.BASELINE_SHA256.equals(UltExtractorPolicy.sha256(stage))
                    || !UltExtractorPolicy.BASELINE_VERSION.equals(probe(c, stage, output)))
                throw new IOException("APK baseline verification failed");
            // Do not overwrite an existing user rollback backup. An old/corrupt
            // extractor is below the supported baseline and must not be restored.
            UltExtractorPolicy.atomicMove(stage, live);
            prefs(c).edit().putString("installed_hash", UltExtractorPolicy.BASELINE_SHA256).apply();
            recordDetectedVersion(UltExtractorPolicy.BASELINE_VERSION);
            status = "Recovered pinned extractor " + UltExtractorPolicy.BASELINE_VERSION + " offline";
            baselineReady = true;
        } finally { stage.delete(); output.delete(); }
    }

    private static void run(Context c, String action, boolean nightly) throws Exception {
        YoutubeDL.getInstance().init(c);
        ensureBaseline(c);
        markInitialized();
        File d = dir(c), live = new File(d,"yt-dlp"), previous = new File(d,"ult-previous");
        File staging = new File(d,"ult-stage"), backupTemp = new File(d,"ult-backup-stage");
        File metadata = new File(d,"ult-release-json"), sums = new File(d,"ult-sums");
        File output = new File(d,"ult-probe-output");
        try {
            String currentHash = UltExtractorPolicy.sha256(live);
            String currentVersion;
            try { currentVersion = probe(c, live, output); }
            catch (Exception e) { currentVersion = "Unknown"; }
            recordDetectedVersion(currentVersion);
            if ("inspect".equals(action)) {
                status = UltExtractorPolicy.validVersion(currentVersion)
                        ? "Installed version read locally; no network contacted"
                        : "Local version probe failed; no network contacted";
                return;
            }
            if ("verify".equals(action)) {
                String expected = prefs(c).getString("installed_hash", "");
                status = expected.isEmpty() ? "Local SHA-256 (no saved trust baseline): " + currentHash
                        : expected.equals(currentHash) ? "Local SHA-256 matches saved baseline: " + currentHash
                        : "WARNING: extractor differs from saved integrity baseline";
                return;
            }
            String expected, targetVersion = null;
            if ("check".equals(action) || "update".equals(action)) {
                checkNetwork(c);
                status = "Checking official release";
                fetch(c, "https://api.github.com/repos/" + UltExtractorPolicy.repository(nightly)
                        + "/releases/latest", metadata, 1024 * 1024);
                JSONObject release = new JSONObject(read(metadata, 1024 * 1024));
                if (release.optBoolean("draft")) throw new IOException("Draft release");
                targetVersion = release.getString("tag_name");
                String binUrl = UltExtractorPolicy.asset(nightly, targetVersion, "yt-dlp");
                String sumUrl = UltExtractorPolicy.asset(nightly, targetVersion, "SHA2-256SUMS");
                expected = releaseDigest(release, "yt-dlp", binUrl);
                String sumsHash = releaseDigest(release, "SHA2-256SUMS", sumUrl);
                available = targetVersion;
                prefs(c).edit().putLong("last_check", System.currentTimeMillis()).apply();
                if (currentHash.equals(expected)) {
                    prefs(c).edit().putString("installed_hash", expected).commit();
                    status = "Already installed; SHA-256 matches official release";
                    return;
                }
                if ("check".equals(action)) {
                    status = "Release available: " + targetVersion + " (not installed)";
                    return;
                }
                status = "Downloading verified release assets";
                fetch(c, sumUrl, sums, 256 * 1024);
                if (!sumsHash.equals(UltExtractorPolicy.sha256(sums))
                        || !expected.equals(UltExtractorPolicy.checksum(read(sums, 256 * 1024))))
                    throw new IOException("Official checksums disagree");
                fetch(c, binUrl, staging, UltExtractorPolicy.MAX_BINARY);
            } else if ("rollback".equals(action)) {
                expected = prefs(c).getString("backup_hash", "");
                if (!UltExtractorPolicy.validHash(expected)
                        || !expected.equals(UltExtractorPolicy.sha256(previous))) {
                    status = "No intact recorded rollback backup"; return;
                }
                try (InputStream in = new FileInputStream(previous)) {
                    UltExtractorPolicy.copy(in, staging, UltExtractorPolicy.MAX_BINARY);
                }
            } else {
                try (InputStream in = c.getResources().openRawResource(org.telegram.messenger.R.raw.ult_ytdlp_baseline)) {
                    UltExtractorPolicy.copy(in, staging, UltExtractorPolicy.MAX_BINARY);
                }
                expected = UltExtractorPolicy.sha256(staging);
            }
            if (!expected.equals(UltExtractorPolicy.sha256(staging))) throw new IOException("Hash mismatch");
            status = "Testing staged extractor with embedded Python";
            String actual = probe(c, staging, output);
            if (!UltExtractorPolicy.meetsBaseline(actual)) {
                status = "Refused extractor older than supported baseline " + UltExtractorPolicy.BASELINE_VERSION;
                return;
            }
            if (targetVersion != null && !targetVersion.equals(actual)) throw new IOException("Version mismatch");
            if (remote) checkNetwork(c);
            if (!prefs(c).edit().putString("backup_hash", currentHash).commit())
                throw new IOException("Cannot record backup");
            UltExtractorPolicy.replace(staging, live, previous, backupTemp, expected);
            installed = actual;
            if (!prefs(c).edit().putString("installed_hash", expected).commit()) {
                status = "Installed " + actual + "; integrity baseline could not be saved";
                return;
            }
            status = "Installed " + actual + "; previous extractor backed up";
        } finally {
            for (File f : new File[]{staging, backupTemp, metadata, sums, output}) f.delete();
        }
    }
}

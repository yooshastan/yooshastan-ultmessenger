package com.yooshastan.ultmessenger.video;

import java.io.*;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.Locale;
import java.util.regex.*;

/** Pure helpers for bounded, verified, atomic extractor replacement. */
public final class UltExtractorPolicy {
    private UltExtractorPolicy() {}
    public static final long MAX_BINARY = 32L * 1024 * 1024;
    public static final String BASELINE_VERSION = "2026.11.23";
    public static final String BASELINE_SHA256 = "e8665ed64d6b9172773e812f891fb707a0cb1ee8d9ef4fc1acb00cabee4c5256";

    /** Compare calendar release components numerically, including nightly suffix. */
    public static boolean meetsBaseline(String v) {
        if (!validVersion(v)) return false;
        String[] actual = v.split("\\."), baseline = BASELINE_VERSION.split("\\.");
        for (int i = 0; i < 3; i++) {
            int a = Integer.parseInt(actual[i]), b = Integer.parseInt(baseline[i]);
            if (a != b) return a > b;
        }
        return true;
    }
    public static boolean validVersion(String v) {
        return v != null && v.matches("[0-9]{4}\\.[0-9]{1,2}\\.[0-9]{1,2}(?:\\.[0-9]{1,12})?");
    }
    public static String repository(boolean nightly) {
        return nightly ? "yt-dlp/yt-dlp-nightly-builds" : "yt-dlp/yt-dlp";
    }
    public static String asset(boolean nightly, String version, String name) {
        if (!validVersion(version) || !("yt-dlp".equals(name) || "SHA2-256SUMS".equals(name)))
            throw new IllegalArgumentException("Unexpected release asset");
        return "https://github.com/" + repository(nightly) + "/releases/download/" + version + "/" + name;
    }
    public static boolean validHash(String h) {
        return h != null && h.matches("[0-9a-f]{64}");
    }
    public static String checksum(String manifest) throws IOException {
        String hash = null;
        Pattern line = Pattern.compile("^([0-9a-fA-F]{64}) [ *]yt-dlp$");
        for (String row : manifest.split("\\r?\\n")) {
            Matcher m = line.matcher(row);
            if (m.matches()) {
                if (hash != null) throw new IOException("Duplicate checksum");
                hash = m.group(1).toLowerCase(Locale.US);
            }
        }
        if (!validHash(hash)) throw new IOException("Missing checksum");
        return hash;
    }
    public static String sha256(File file) throws Exception {
        if (!file.isFile() || file.length() <= 0 || file.length() > MAX_BINARY)
            throw new IOException("Invalid extractor size");
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        try (InputStream in = new FileInputStream(file)) {
            byte[] buf = new byte[65536];
            long count = 0;
            for (int n; (n = in.read(buf)) != -1;) {
                count += n;
                if (count > MAX_BINARY) throw new IOException("Extractor too large");
                md.update(buf, 0, n);
            }
        }
        StringBuilder out = new StringBuilder(64);
        for (byte b : md.digest()) out.append(String.format(Locale.US, "%02x", b & 255));
        return out.toString();
    }
    public static void atomicMove(File from, File to) throws IOException {
        Files.move(from.toPath(), to.toPath(), StandardCopyOption.ATOMIC_MOVE,
                StandardCopyOption.REPLACE_EXISTING);
    }
    public static void copy(InputStream in, File target, long max) throws IOException {
        try (FileOutputStream out = new FileOutputStream(target)) {
            byte[] buf = new byte[65536];
            long count = 0;
            for (int n; (n = in.read(buf)) != -1;) {
                count += n;
                if (count > max) throw new IOException("Asset too large");
                out.write(buf, 0, n);
            }
            if (count == 0) throw new IOException("Empty asset");
            out.getFD().sync();
        }
    }
    /** Caller stages and tests candidate on the same filesystem before this call.
     * Failure before the last rename cannot replace the working binary. */
    public static void replace(File candidate, File live, File backup, File backupTemp,
                               String expected) throws Exception {
        if (!validHash(expected) || !expected.equals(sha256(candidate)))
            throw new IOException("Candidate hash mismatch");
        String original = sha256(live);
        try (InputStream in = new FileInputStream(live)) { copy(in, backupTemp, MAX_BINARY); }
        if (!original.equals(sha256(backupTemp))) throw new IOException("Backup hash mismatch");
        atomicMove(backupTemp, backup);
        atomicMove(candidate, live);
    }
}

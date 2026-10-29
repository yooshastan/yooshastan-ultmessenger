package com.yooshastan.ultmessenger.settings;

import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;

/** Local, Android-free SHA-256 inspector. No files, account IDs or network calls. */
public final class UltFileInspection {
    public static final long MAX_INSPECT_BYTES = 512L * 1024L * 1024L;

    private UltFileInspection() {}

    public static final class Result {
        public final long bytesRead;
        public final String sha256;
        Result(long bytes, String digest) { bytesRead = bytes; sha256 = digest; }
    }

    public static Result digest(InputStream input) throws IOException {
        if (input == null) throw new IllegalArgumentException("Input is required");
        final MessageDigest md;
        try {
            md = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
        final byte[] buffer = new byte[32768];
        long size = 0;
        int count;
        while ((count = input.read(buffer)) != -1) {
            size += count;
            if (size > MAX_INSPECT_BYTES) {
                throw new IOException("File exceeds local inspection limit");
            }
            md.update(buffer, 0, count);
        }
        StringBuilder hex = new StringBuilder(64);
        for (byte b : md.digest()) {
            hex.append(Character.forDigit((b >>> 4) & 15, 16));
            hex.append(Character.forDigit(b & 15, 16));
        }
        return new Result(size, hex.toString());
    }

    /** Constant-length hash comparison after both bounded streams are read. */
    public static boolean sameContent(Result source, Result selected) {
        return source != null && selected != null
                && source.bytesRead == selected.bytesRead
                && java.security.MessageDigest.isEqual(
                    source.sha256.getBytes(java.nio.charset.StandardCharsets.US_ASCII),
                    selected.sha256.getBytes(java.nio.charset.StandardCharsets.US_ASCII));
    }

    public static String describe(String safeName, String mime, Result result) {
        if (result == null) throw new IllegalArgumentException("No checksum");
        String name = safeName == null ? "unnamed" : safeName.replaceAll("[\\r\\n\\t]", "_");
        if (name.isEmpty()) name = "unnamed";
        String safeMime = mime != null && mime.matches(
                "[A-Za-z0-9][A-Za-z0-9.+_-]*/[A-Za-z0-9][A-Za-z0-9.+_-]*")
                ? mime : "unknown";
        return "File: " + name + "\nMIME: " + safeMime
                + "\nSize: " + String.format(Locale.ROOT, "%,d", result.bytesRead)
                + " bytes\nSHA-256 (local file):\n" + result.sha256
                + "\n\nOnly the local download was hashed. This does not verify "
                + "a separate Save As destination. Copying exposes the hash to your clipboard.";
    }
}

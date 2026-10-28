package com.yooshastan.ultmessenger.chat;

import java.util.Locale;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.regex.Pattern;

/**
 * Read-only chat identifiers and public link helpers.
 * Never sends a Telegram request or copies data without a user tap.
 */
public final class UltChatTools {
    private static final Pattern USERNAME = Pattern.compile("[A-Za-z0-9_]{1,32}");

    private UltChatTools() { }

    public static String numericId(long id) {
        return Long.toString(id);
    }

    /** Returns null when a chat has no safe public username. */
    public static String publicLink(String username) {
        if (username == null) {
            return null;
        }
        String candidate = username.trim();
        if (candidate.startsWith("@")) {
            candidate = candidate.substring(1);
        }
        if (!USERNAME.matcher(candidate).matches()) {
            return null;
        }
        return "https://t.me/" + candidate.toLowerCase(Locale.ROOT);
    }

    /** Public message permalink. Never generate private, invitation or secret-chat links. */
    public static String publicMessageLink(String username, long messageId) {
        if (messageId <= 0) {
            return null;
        }
        final String root = publicLink(username);
        return root != null ? root + "/" + messageId : null;
    }

    /** Local message metadata, copied only following an explicit menu tap. */
    public static String unixTimestamp(long seconds) {
        return seconds > 0 ? Long.toString(seconds) : null;
    }

    /** Human-readable UTC; Z explicitly means UTC, never device-local time. */
    public static String utcTimestamp(long seconds) {
        return seconds > 0 ? DateTimeFormatter.ISO_INSTANT.format(
                Instant.ofEpochSecond(seconds)) : null;
    }

    /** Safe document metadata; never reads, opens or exports the file. */
    public static String mediaDetails(String mime, long bytes) {
        String safeMime = mime != null && mime.length() <= 96 && !mime.contains("..")
                && mime.matches("[A-Za-z0-9][A-Za-z0-9.+_-]*/[A-Za-z0-9][A-Za-z0-9.+_-]*")
                ? mime : "unknown";
        return "MIME: " + safeMime + "\nBytes: " + Math.max(0L, bytes);
    }

    /** Clipboard-only summary: no filesystem, storage or network access. */
    public static String mediaSummary(String filename, String mime, long bytes) {
        String safeName = filename == null ? "" : filename.trim();
        // Prevent terminal/control injection and accidental path/credential copying.
        safeName = safeName.replaceAll("[\\\\/\\r\\n\\t\\p{Cntrl}]", "_");
        if (safeName.length() > 120) {
            safeName = safeName.substring(0, 120);
        }
        if (safeName.isEmpty()) {
            safeName = "unnamed";
        }
        long safeSize = Math.max(0L, bytes);
        final String[] units = {"B", "KiB", "MiB", "GiB", "TiB"};
        double amount = safeSize;
        int unit = 0;
        while (amount >= 1024.0 && unit < units.length - 1) {
            amount /= 1024.0;
            unit++;
        }
        String size = unit == 0 ? Long.toString(safeSize) + " B"
                : String.format(Locale.ROOT, "%.1f %s", amount, units[unit]);
        String details = mediaDetails(mime, safeSize);
        return "File: " + safeName + "\nSize: " + size + "\n" + details;
    }

    /** One-tap, explicitly requested metadata reference; no message body or private links. */
    public static String messageReference(long messageId, long senderPeerId,
                                          long timestamp, String publicUsername) {
        if (messageId <= 0) return null;
        StringBuilder text = new StringBuilder("Message ID: ").append(messageId);
        if (senderPeerId != 0) {
            text.append("\nSender peer ID: ").append(senderPeerId);
        }
        String utc = utcTimestamp(timestamp);
        if (utc != null) text.append("\nUTC: ").append(utc);
        String link = publicMessageLink(publicUsername, messageId);
        if (link != null) text.append("\nPublic link: ").append(link);
        return text.toString();
    }

    public static boolean hasTopic(long topicId) {
        return topicId > 0;
    }
}

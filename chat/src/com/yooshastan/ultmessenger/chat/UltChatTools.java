package com.yooshastan.ultmessenger.chat;

import java.util.Locale;
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

    /** Local message metadata, copied only following an explicit menu tap. */
    public static String unixTimestamp(long seconds) {
        return seconds > 0 ? Long.toString(seconds) : null;
    }

    /** Safe document metadata; never reads, opens or exports the file. */
    public static String mediaDetails(String mime, long bytes) {
        String safeMime = mime != null && mime.matches("[A-Za-z0-9.+_-]+/[A-Za-z0-9.+_-]+")
                ? mime : "unknown";
        return "MIME: " + safeMime + "\nBytes: " + Math.max(0L, bytes);
    }

    public static boolean hasTopic(long topicId) {
        return topicId > 0;
    }
}

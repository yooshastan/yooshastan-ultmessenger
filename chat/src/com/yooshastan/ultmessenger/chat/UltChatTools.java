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

    public static boolean hasTopic(long topicId) {
        return topicId > 0;
    }
}

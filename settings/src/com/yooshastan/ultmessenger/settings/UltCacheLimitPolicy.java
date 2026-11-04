package com.yooshastan.ultmessenger.settings;

/**
 * Native Telegram cache_limit is measured in GB, except 1 is reserved by
 * pinned upstream's 300 MB setting. Do not represent 1 as 1GB.
 */
public final class UltCacheLimitPolicy {
    public static final int MIN_GB = 2;
    public static final int MAX_GB = 32;
    public static final int NO_LIMIT = Integer.MAX_VALUE;
    public static final int DEFAULT_GB = 10;
    private UltCacheLimitPolicy() {}

    public static boolean isAllowed(int limitGb) {
        return limitGb == NO_LIMIT || limitGb >= MIN_GB && limitGb <= MAX_GB;
    }

    public static int initialGb(int saved) {
        if (saved >= MIN_GB && saved <= MAX_GB) return saved;
        return DEFAULT_GB;
    }

    public static String label(int value) {
        if (value == NO_LIMIT) return "No Limit";
        if (value == 1) return "300 MB (legacy)";
        if (value >= MIN_GB) return value + " GB";
        return "Unknown";
    }
}

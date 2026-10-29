package com.yooshastan.ultmessenger.settings;

/** Mirrors the pinned Telegram ThemeActivity's native chat size limits. */
public final class UltChatAppearancePolicy {
    public static final int MIN_FONT = 12;
    public static final int MAX_FONT = 30;
    public static final int MAX_RADIUS = 17;
    private UltChatAppearancePolicy() {}
    public static int font(int value) {
        return Math.min(MAX_FONT, Math.max(MIN_FONT, value));
    }
    public static int radius(int value) {
        return Math.min(MAX_RADIUS, Math.max(0, value));
    }
}

package com.yooshastan.ultmessenger.chat;

/**
 * Opt-in local presentation presets backed by Telegram's native chat
 * text-size and bubble-radius controls. No messages or account data change.
 */
public enum UltChatLayoutPreset {
    TELEGRAM("Telegram Default", 0, -1),
    COMPACT("Compact", -2, 5),
    COMFORTABLE("Comfortable", 2, 17),
    MINIMAL("Minimal Bubbles", 0, 0);

    public final String label;
    public final int fontDeltaSp;
    /** -1 means restore the user's previous Telegram bubble radius. */
    public final int bubbleRadius;
    public final boolean keepAccessibilityLabels = true;

    UltChatLayoutPreset(String label, int fontDeltaSp, int bubbleRadius) {
        this.label = label;
        this.fontDeltaSp = fontDeltaSp;
        this.bubbleRadius = bubbleRadius;
    }

    public int targetFontSize(int original) {
        if (this == TELEGRAM) {
            return original;
        }
        return Math.max(12, Math.min(30, original + fontDeltaSp));
    }

    public int targetBubbleRadius(int original) {
        return this == TELEGRAM ? original : bubbleRadius;
    }

    public static UltChatLayoutPreset fromStoredValue(String value) {
        if (value != null) {
            for (UltChatLayoutPreset preset : values()) {
                if (preset.name().equalsIgnoreCase(value)) {
                    return preset;
                }
            }
        }
        return TELEGRAM;
    }
}

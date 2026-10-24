package com.yooshastan.ultmessenger.chat;

/**
 * Local-only presentation preferences for eventual Telegram chat integration.
 * No server requests, message mutation, or Premium entitlement changes.
 */
public enum UltChatLayoutPreset {
    TELEGRAM("Telegram Default", 0, false, true),
    COMPACT("Compact", -4, false, true),
    COMFORTABLE("Comfortable", 4, false, true),
    MEDIA_FOCUSED("Media Focused", 2, true, true);

    public final String label;
    /** Desired relative vertical bubble padding; UI adapter must clamp safely. */
    public final int verticalPaddingDeltaDp;
    public final boolean emphasizeMedia;
    public final boolean keepAccessibilityLabels;

    UltChatLayoutPreset(String label, int padding, boolean media, boolean a11y) {
        this.label = label;
        this.verticalPaddingDeltaDp = padding;
        this.emphasizeMedia = media;
        this.keepAccessibilityLabels = a11y;
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

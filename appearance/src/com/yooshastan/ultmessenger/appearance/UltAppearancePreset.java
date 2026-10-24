package com.yooshastan.ultmessenger.appearance;

/**
 * ULT appearance presets. Immutable UI palette definitions applied by
 * UltAppearanceThemeAdapter through Telegram's native theme engine.
 */
public enum UltAppearancePreset {
    SYSTEM("System", 0xFF1976D2, 0xFFFFFFFF, 0xFFF5F6FA, false),
    MIDNIGHT("ULT Midnight", 0xFFE86BCB, 0xFFF8F3FF, 0xFF171327, true),
    CANDY("Candy Pink", 0xFFE3499B, 0xFF3A1830, 0xFFFFF0FA, false),
    STAGE("Neon Stage", 0xFF5DF1E6, 0xFFF1FFFF, 0xFF11182A, true),
    MINIMAL("Minimal", 0xFF727B8A, 0xFF1E2630, 0xFFF7F8FA, false);

    public final String displayName;
    public final int accentArgb;
    public final int textArgb;
    public final int backgroundArgb;
    public final boolean dark;

    UltAppearancePreset(String displayName, int accentArgb, int textArgb,
                        int backgroundArgb, boolean dark) {
        this.displayName = displayName;
        this.accentArgb = accentArgb;
        this.textArgb = textArgb;
        this.backgroundArgb = backgroundArgb;
        this.dark = dark;
    }

    public static UltAppearancePreset fromStoredValue(String value) {
        if (value != null) {
            for (UltAppearancePreset preset : values()) {
                if (preset.name().equalsIgnoreCase(value)) {
                    return preset;
                }
            }
        }
        return SYSTEM;
    }
}

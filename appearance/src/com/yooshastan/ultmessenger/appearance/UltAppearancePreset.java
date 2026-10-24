package com.yooshastan.ultmessenger.appearance;

/**
 * ULT appearance presets. Immutable UI palette definitions applied by
 * UltAppearanceThemeAdapter through Telegram's native theme engine.
 * Original ULT palettes; no third-party artwork or copied theme files.
 */
public enum UltAppearancePreset {
    SYSTEM("System", 0xFF1976D2, 0xFF212936, 0xFFFFFFFF, false,
            0xFFFFFFFF, 0xFFE4EFFA),
    MIDNIGHT("ULT Midnight", 0xFFE86BCB, 0xFFF8F3FF, 0xFF171327, true,
            0xFF29223A, 0xFF422C50),
    CANDY("Candy Pink", 0xFFE3499B, 0xFF3A1830, 0xFFFFF0FA, false,
            0xFFFFFFFF, 0xFFFCD5EB),
    STAGE("Neon Stage", 0xFF5DF1E6, 0xFFF1FFFF, 0xFF11182A, true,
            0xFF1C2A3C, 0xFF264D57),
    MINIMAL("Minimal", 0xFF727B8A, 0xFF1E2630, 0xFFF7F8FA, false,
            0xFFFFFFFF, 0xFFE8ECEF),
    AURORA("ULT Aurora", 0xFFB789FF, 0xFFF7F3FF, 0xFF121025, true,
            0xFF232039, 0xFF3D3057),
    OBSIDIAN("ULT Obsidian", 0xFFA5AEE6, 0xFFF3F5FF, 0xFF0E1018, true,
            0xFF1C2330, 0xFF30364A);

    public final String displayName;
    public final int accentArgb;
    public final int textArgb;
    public final int backgroundArgb;
    public final boolean dark;
    public final int incomingBubbleArgb;
    public final int outgoingBubbleArgb;

    UltAppearancePreset(String displayName, int accentArgb, int textArgb,
                        int backgroundArgb, boolean dark, int incomingBubbleArgb,
                        int outgoingBubbleArgb) {
        this.displayName = displayName;
        this.accentArgb = accentArgb;
        this.textArgb = textArgb;
        this.backgroundArgb = backgroundArgb;
        this.dark = dark;
        this.incomingBubbleArgb = incomingBubbleArgb;
        this.outgoingBubbleArgb = outgoingBubbleArgb;
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

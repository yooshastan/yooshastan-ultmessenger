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

    /** A complete light or dark companion palette for the same ULT identity. */
    public static final class Palette {
        public final int accentArgb;
        public final int textArgb;
        public final int backgroundArgb;
        public final int incomingBubbleArgb;
        public final int outgoingBubbleArgb;
        public final boolean dark;

        public Palette(int accentArgb, int textArgb, int backgroundArgb,
                       int incomingBubbleArgb, int outgoingBubbleArgb, boolean dark) {
            this.accentArgb = accentArgb;
            this.textArgb = textArgb;
            this.backgroundArgb = backgroundArgb;
            this.incomingBubbleArgb = incomingBubbleArgb;
            this.outgoingBubbleArgb = outgoingBubbleArgb;
            this.dark = dark;
        }
    }

    /**
     * Explicit original ULT palettes, not automatic inversion. Preserving a
     * distinct accent on both backgrounds avoids illegible pink/violet text.
     */
    public Palette forMode(boolean night) {
        switch (this) {
            case MIDNIGHT:
                return night
                        ? new Palette(0xFFE86BCB, 0xFFF8F3FF, 0xFF171327, 0xFF29223A, 0xFF422C50, true)
                        : new Palette(0xFFC8429F, 0xFF30233A, 0xFFFFF7FC, 0xFFFFFFFF, 0xFFFFD7F3, false);
            case CANDY:
                return night
                        ? new Palette(0xFFFF7BC5, 0xFFFFECF8, 0xFF1D101B, 0xFF302030, 0xFF572B4A, true)
                        : new Palette(0xFFE3499B, 0xFF3A1830, 0xFFFFF0FA, 0xFFFFFFFF, 0xFFFCD5EB, false);
            case STAGE:
                return night
                        ? new Palette(0xFF5DF1E6, 0xFFF1FFFF, 0xFF11182A, 0xFF1C2A3C, 0xFF264D57, true)
                        : new Palette(0xFF087F88, 0xFF16313A, 0xFFF3FFFE, 0xFFFFFFFF, 0xFFD2F6F2, false);
            case MINIMAL:
                return night
                        ? new Palette(0xFFBFC6D1, 0xFFF0F1F5, 0xFF171B22, 0xFF252C35, 0xFF38414C, true)
                        : new Palette(0xFF727B8A, 0xFF1E2630, 0xFFF7F8FA, 0xFFFFFFFF, 0xFFE8ECEF, false);
            case AURORA:
                return night
                        ? new Palette(0xFFB789FF, 0xFFF7F3FF, 0xFF121025, 0xFF232039, 0xFF3D3057, true)
                        : new Palette(0xFF9261E4, 0xFF2B1B3B, 0xFFF6F0FF, 0xFFFFFFFF, 0xFFEBD9FF, false);
            case OBSIDIAN:
                return night
                        ? new Palette(0xFFA5AEE6, 0xFFF3F5FF, 0xFF0E1018, 0xFF1C2330, 0xFF30364A, true)
                        : new Palette(0xFF59688F, 0xFF161B2B, 0xFFF5F7FF, 0xFFFFFFFF, 0xFFDBE2F7, false);
            case SYSTEM:
            default:
                throw new IllegalArgumentException("System uses the saved native Telegram themes");
        }
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

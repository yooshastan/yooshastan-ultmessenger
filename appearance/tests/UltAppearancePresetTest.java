import com.yooshastan.ultmessenger.appearance.UltAppearancePreset;

public final class UltAppearancePresetTest {
    public static void main(String[] args) {
        require(UltAppearancePreset.fromStoredValue(null) == UltAppearancePreset.SYSTEM,
                "null should restore System");
        require(UltAppearancePreset.fromStoredValue("midnight") == UltAppearancePreset.MIDNIGHT,
                "stored Alpha 0.2 preset must remain compatible");
        require(UltAppearancePreset.fromStoredValue("aurora") == UltAppearancePreset.AURORA,
                "Aurora must persist by enum name");
        require(UltAppearancePreset.fromStoredValue("invalid") == UltAppearancePreset.SYSTEM,
                "invalid preset must fail safe");
        for (UltAppearancePreset preset : UltAppearancePreset.values()) {
            require(!preset.displayName.isEmpty(), "missing visible label");
            require((preset.backgroundArgb >>> 24) == 0xFF, "background must be opaque");
            require((preset.incomingBubbleArgb >>> 24) == 0xFF, "incoming bubble must be opaque");
            require((preset.outgoingBubbleArgb >>> 24) == 0xFF, "outgoing bubble must be opaque");
        }
        require(UltAppearancePreset.AURORA.dark && UltAppearancePreset.OBSIDIAN.dark,
                "new high-contrast palettes must use native Night base");
        for (UltAppearancePreset preset : UltAppearancePreset.values()) {
            if (preset == UltAppearancePreset.SYSTEM) {
                continue;
            }
            UltAppearancePreset.Palette day = preset.forMode(false);
            UltAppearancePreset.Palette night = preset.forMode(true);
            require(!day.dark && night.dark, "paired mode flags must match " + preset);
            require(day.backgroundArgb != night.backgroundArgb,
                    "day/night colors must differ for " + preset);
            for (UltAppearancePreset.Palette palette : new UltAppearancePreset.Palette[]{day, night}) {
                require((palette.backgroundArgb >>> 24) == 0xFF,
                        "paired background opacity: " + preset);
                require((palette.accentArgb >>> 24) == 0xFF,
                        "paired accent opacity: " + preset);
                require(contrast(palette.textArgb, palette.backgroundArgb) >= 4.5,
                        "paired text/background contrast: " + preset);
                require(contrast(palette.textArgb, palette.incomingBubbleArgb) >= 4.5,
                        "paired incoming-bubble contrast: " + preset);
                require(contrast(palette.textArgb, palette.outgoingBubbleArgb) >= 4.5,
                        "paired outgoing-bubble contrast: " + preset);
            }
        }
        System.out.println("ULT visual palette checks passed");
    }

    private static double contrast(int a, int b) {
        double x = luminance(a), y = luminance(b);
        return (Math.max(x, y) + 0.05) / (Math.min(x, y) + 0.05);
    }

    private static double luminance(int rgb) {
        return 0.2126 * channel((rgb >>> 16) & 255)
                + 0.7152 * channel((rgb >>> 8) & 255)
                + 0.0722 * channel(rgb & 255);
    }

    private static double channel(int value) {
        double x = value / 255.0;
        return x <= 0.04045 ? x / 12.92 : Math.pow((x + 0.055) / 1.055, 2.4);
    }

    private static void require(boolean condition, String description) {
        if (!condition) {
            throw new AssertionError(description);
        }
    }
}

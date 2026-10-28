package com.yooshastan.ultmessenger.appearance;

import java.util.Locale;

/** Pure, Android-free policy for original Ultgram fixed custom theme colors. */
public final class UltCustomTheme {
    private UltCustomTheme() {}

    public static int parseRgb(String color) {
        if (color == null || !color.matches("#[0-9a-fA-F]{6}")) {
            throw new IllegalArgumentException("Enter exactly six hex digits: #RRGGBB");
        }
        return (int) (0xFF000000L | Long.parseLong(color.substring(1), 16));
    }

    public static String toHex(int argb) {
        return String.format(Locale.ROOT, "#%06X", argb & 0xFFFFFF);
    }

    public static UltAppearancePreset.Palette edit(
            UltAppearancePreset preset, String accentHex, String outgoingHex) {
        if (preset == null || preset == UltAppearancePreset.SYSTEM) {
            throw new IllegalArgumentException("Choose a custom ULT preset base");
        }
        UltAppearancePreset.Palette base = preset.forMode(preset.dark);
        int accent = parseRgb(accentHex);
        int outgoing = parseRgb(outgoingHex);
        if (contrast(accent, base.backgroundArgb) < 3.0) {
            throw new IllegalArgumentException("Accent must contrast with the theme background");
        }
        if (contrast(base.textArgb, outgoing) < 4.5) {
            throw new IllegalArgumentException("Outgoing bubble must contrast with message text");
        }
        return new UltAppearancePreset.Palette(accent, base.textArgb,
                base.backgroundArgb, base.incomingBubbleArgb, outgoing, base.dark);
    }

    public static double contrast(int a, int b) {
        double x = luminance(a), y = luminance(b);
        return (Math.max(x, y) + 0.05) / (Math.min(x, y) + 0.05);
    }

    private static double luminance(int rgb) {
        return 0.2126 * channel((rgb >>> 16) & 255)
                + 0.7152 * channel((rgb >>> 8) & 255)
                + 0.0722 * channel(rgb & 255);
    }

    private static double channel(int component) {
        double c = component / 255.0;
        return c <= 0.04045 ? c / 12.92 : Math.pow((c + 0.055) / 1.055, 2.4);
    }
}

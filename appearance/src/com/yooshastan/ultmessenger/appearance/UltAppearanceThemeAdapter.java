package com.yooshastan.ultmessenger.appearance;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.util.SparseIntArray;

import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.ActionBar.ThemeColors;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

/**
 * Builds complete .attheme files using Telegram's own Day/Night palettes as a
 * base, then applies ULT colors through the native theme engine. The imported
 * theme survives an app restart and remains selectable in Telegram's themes.
 */
public final class UltAppearanceThemeAdapter {
    private static final String PREFERENCES = "ult_appearance";
    private static final String SELECTED = "selected_preset";
    private static final String PREVIOUS_THEME = "previous_theme_key";

    private UltAppearanceThemeAdapter() {}

    public static boolean apply(Context context, UltAppearancePreset preset) throws IOException {
        SharedPreferences prefs = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE);
        if (preset == UltAppearancePreset.SYSTEM) {
            String previous = prefs.getString(PREVIOUS_THEME, "Blue");
            Theme.ThemeInfo original = Theme.getTheme(previous);
            if (original == null) {
                original = Theme.getTheme("Blue");
            }
            if (original == null) {
                return false;
            }
            Theme.applyTheme(original);
            prefs.edit().putString(SELECTED, preset.name()).remove(PREVIOUS_THEME).apply();
            return true;
        }

        Theme.ThemeInfo base = Theme.getTheme(preset.dark ? "Night" : "Day");
        if (base == null || base.assetName == null) {
            return false;
        }
        Theme.ThemeInfo current = Theme.getCurrentTheme();
        String originalKey = current != null ? current.getKey() : "Blue";
        if (!prefs.contains(PREVIOUS_THEME)) {
            prefs.edit().putString(PREVIOUS_THEME, originalKey).apply();
        }

        SparseIntArray colors = Theme.getThemeFileValues(null, base.assetName, null);
        int[] defaults = ThemeColors.createDefaultColors();
        int headerText = Color.luminance(preset.accentArgb) > 0.179 ? Color.BLACK : Color.WHITE;
        colors.put(Theme.key_actionBarDefault, preset.accentArgb);
        colors.put(Theme.key_actionBarDefaultTitle, headerText);
        colors.put(Theme.key_actionBarDefaultSubtitle, headerText);
        colors.put(Theme.key_actionBarDefaultIcon, headerText);
        colors.put(Theme.key_windowBackgroundWhite, preset.backgroundArgb);
        colors.put(Theme.key_windowBackgroundGray, preset.backgroundArgb);
        colors.put(Theme.key_windowBackgroundWhiteBlackText, preset.textArgb);
        colors.put(Theme.key_windowBackgroundWhiteBlueText, preset.accentArgb);
        colors.put(Theme.key_dialogButton, preset.accentArgb);

        StringBuilder body = new StringBuilder(64_000);
        for (int key = 0; key < Theme.colorsCount; key++) {
            String name = ThemeColors.getStringName(key);
            if (name == null || key == Theme.key_wallpaperFileOffset) {
                continue;
            }
            int color = colors.get(key, key < defaults.length ? defaults[key] : 0);
            body.append(name).append('=').append(color).append('\n');
        }

        File temp = new File(context.getCacheDir(),
                "ult-" + preset.name().toLowerCase(Locale.ROOT) + ".attheme");
        try (FileOutputStream stream = new FileOutputStream(temp)) {
            stream.write(body.toString().getBytes(StandardCharsets.UTF_8));
        }
        Theme.ThemeInfo installed = Theme.applyThemeFile(temp, preset.displayName, null, false);
        if (installed == null) {
            return false;
        }
        prefs.edit().putString(SELECTED, preset.name()).apply();
        return true;
    }
}

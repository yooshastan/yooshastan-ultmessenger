package com.yooshastan.ultmessenger.appearance;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
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
    private static final String PREVIOUS_NIGHT_THEME = "previous_night_theme_key";
    private static final String PREVIOUS_AUTO_NIGHT = "previous_auto_night_type";

    private UltAppearanceThemeAdapter() {}

    /** Preserves the Alpha 0.10a API: existing callers choose a fixed theme. */
    public static boolean apply(Context context, UltAppearancePreset preset) throws IOException {
        return apply(context, preset, false);
    }

    /** Select either the tested fixed theme or a paired day/night system theme. */
    public static boolean apply(Context context, UltAppearancePreset preset,
                                boolean followSystem) throws IOException {
        SharedPreferences prefs = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE);
        if (preset == UltAppearancePreset.SYSTEM) {
            if (!prefs.contains(PREVIOUS_THEME)) {
                // Nothing to undo; do not reset a user's existing Telegram theme.
                prefs.edit().putString(SELECTED, preset.name()).apply();
                return true;
            }

            String previousDayKey = prefs.getString(PREVIOUS_THEME, "Blue");
            Theme.ThemeInfo previousDay = Theme.getTheme(previousDayKey);
            if (previousDay == null) {
                previousDay = Theme.getTheme("Blue");
            }
            if (previousDay == null) {
                return false;
            }

            String previousNightKey = prefs.getString(PREVIOUS_NIGHT_THEME, null);
            Theme.ThemeInfo previousNight = previousNightKey != null
                    ? Theme.getTheme(previousNightKey) : null;
            if (previousNight != null) {
                Theme.setCurrentNightTheme(previousNight);
            }
            Theme.applyTheme(previousDay);
            if (prefs.contains(PREVIOUS_AUTO_NIGHT)) {
                Theme.selectedAutoNightType = prefs.getInt(
                        PREVIOUS_AUTO_NIGHT, Theme.AUTO_NIGHT_TYPE_NONE);
                Theme.saveAutoNightThemeConfig();
            }
            Theme.checkAutoNightThemeConditions(true);
            prefs.edit().putString(SELECTED, preset.name())
                    .remove(PREVIOUS_THEME)
                    .remove(PREVIOUS_NIGHT_THEME)
                    .remove(PREVIOUS_AUTO_NIGHT)
                    .apply();
            return true;
        }

        if (followSystem && Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            return false; // Android-wide dark mode arrived with Android 10.
        }

        // Snapshot before importing anything. Theme.applyThemeFile always
        // changes Telegram's DAY slot, even when the theme is dark.
        Theme.ThemeInfo oldDay = Theme.getCurrentTheme();
        Theme.ThemeInfo oldNight = Theme.getCurrentNightTheme();
        int oldAutoNight = Theme.selectedAutoNightType;

        Theme.ThemeInfo pairedDay = null;
        Theme.ThemeInfo pairedNight = null;
        Theme.ThemeInfo installed = null;

        if (followSystem) {
            // Write both complete files before modifying Telegram state. Their
            // stable, separate names avoid replacing the Alpha 0.10a fixed file.
            File dayFile = writeThemeFile(context, preset, preset.forMode(false), "-day");
            File nightFile = writeThemeFile(context, preset, preset.forMode(true), "-night");
            if (dayFile == null || nightFile == null) {
                return false;
            }
            pairedDay = Theme.applyThemeFile(dayFile,
                    "ULT " + preset.name() + " Light", null, false);
            if (pairedDay == null) {
                return false;
            }
            pairedNight = Theme.applyThemeFile(nightFile,
                    "ULT " + preset.name() + " Dark", null, false);
            if (pairedNight == null) {
                // The first import changed the day slot; restore it on failure.
                if (oldDay != null) {
                    Theme.applyTheme(oldDay);
                }
                return false;
            }
        } else {
            // The original fixed palette remains available for users who do
            // not want Telegram/Android to automatically change the colors.
            UltAppearancePreset.Palette fixed = new UltAppearancePreset.Palette(
                    preset.accentArgb, preset.textArgb, preset.backgroundArgb,
                    preset.incomingBubbleArgb, preset.outgoingBubbleArgb, preset.dark);
            File file = writeThemeFile(context, preset, fixed, "");
            if (file == null) {
                return false;
            }
            Theme.ThemeInfo installedTheme = Theme.applyThemeFile(
                    file, preset.displayName, null, false);
            if (installedTheme == null) {
                return false;
            }
            installed = installedTheme;
        }

        SharedPreferences.Editor editor = prefs.edit();
        if (!prefs.contains(PREVIOUS_THEME)) {
            editor.putString(PREVIOUS_THEME, oldDay != null ? oldDay.getKey() : "Blue");
        }
        if (!prefs.contains(PREVIOUS_NIGHT_THEME) && oldNight != null) {
            editor.putString(PREVIOUS_NIGHT_THEME, oldNight.getKey());
        }
        if (!prefs.contains(PREVIOUS_AUTO_NIGHT)) {
            editor.putInt(PREVIOUS_AUTO_NIGHT, oldAutoNight);
        }
        editor.putString(SELECTED, preset.name()).apply();

        if (followSystem) {
            // The second import was temporarily put in the day slot. Assign
            // it to the night slot, restore our light day slot, then allow
            // native Android 10+ system dark-mode detection to control both.
            Theme.setCurrentNightTheme(pairedNight);
            Theme.applyTheme(pairedDay);
            Theme.selectedAutoNightType = Theme.AUTO_NIGHT_TYPE_SYSTEM;
            Theme.saveAutoNightThemeConfig(); // includes the night theme key
            Theme.checkAutoNightThemeConditions(true);
        } else {
            Theme.selectedAutoNightType = Theme.AUTO_NIGHT_TYPE_NONE;
            Theme.saveAutoNightThemeConfig();
            Theme.cancelAutoNightThemeCallbacks();
            Theme.applyTheme(installed);
        }
        return true;
    }

    private static File writeThemeFile(Context context, UltAppearancePreset preset,
                                       UltAppearancePreset.Palette palette,
                                       String suffix) throws IOException {
        Theme.ThemeInfo base = Theme.getTheme(palette.dark ? "Night" : "Day");
        if (base == null || base.assetName == null) {
            return null;
        }
        SparseIntArray colors = Theme.getThemeFileValues(null, base.assetName, null);
        int[] defaults = ThemeColors.createDefaultColors();
        int headerText = Color.luminance(palette.accentArgb) > 0.179 ? Color.BLACK : Color.WHITE;
        colors.put(Theme.key_actionBarDefault, palette.accentArgb);
        colors.put(Theme.key_actionBarDefaultTitle, headerText);
        colors.put(Theme.key_actionBarDefaultSubtitle, headerText);
        colors.put(Theme.key_actionBarDefaultIcon, headerText);
        colors.put(Theme.key_windowBackgroundWhite, palette.backgroundArgb);
        colors.put(Theme.key_windowBackgroundGray, palette.backgroundArgb);
        colors.put(Theme.key_windowBackgroundWhiteBlackText, palette.textArgb);
        colors.put(Theme.key_windowBackgroundWhiteBlueText, palette.accentArgb);
        colors.put(Theme.key_windowBackgroundWhiteGrayText,
                palette.dark ? 0xFFB9B4D0 : 0xFF747786);
        colors.put(Theme.key_chats_name, palette.textArgb);
        colors.put(Theme.key_chats_message,
                palette.dark ? 0xFFB9B4D0 : 0xFF747786);
        colors.put(Theme.key_chat_inBubble, palette.incomingBubbleArgb);
        colors.put(Theme.key_chat_outBubble, palette.outgoingBubbleArgb);
        colors.put(Theme.key_chat_messageTextIn, palette.textArgb);
        colors.put(Theme.key_chat_messageTextOut, palette.textArgb);
        colors.put(Theme.key_dialogButton, palette.accentArgb);

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
                "ult-" + preset.name().toLowerCase(Locale.ROOT) + suffix + ".attheme");
        try (FileOutputStream stream = new FileOutputStream(temp)) {
            stream.write(body.toString().getBytes(StandardCharsets.UTF_8));
        }
        return temp;
    }

}

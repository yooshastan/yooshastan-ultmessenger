package com.yooshastan.ultmessenger.settings;

import android.content.Context;
import android.content.SharedPreferences;

/** Opt-in protection for main Ultgram window; never masks network/account identity. */
public final class UltScreenPrivacy {
    public static final String PREFERENCES = "ult_screen_privacy_v11";
    public static final String KEY_ENABLED = "protect_main_window";

    private UltScreenPrivacy() { }

    public static boolean isEnabled(Context context) {
        return context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
                .getBoolean(KEY_ENABLED, false);
    }

    public static void setEnabled(Context context, boolean enabled) {
        context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE).edit()
                .putBoolean(KEY_ENABLED, enabled).apply();
    }
}

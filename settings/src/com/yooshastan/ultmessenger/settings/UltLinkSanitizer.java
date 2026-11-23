package com.yooshastan.ultmessenger.settings;

import android.content.Context;
import android.text.Spanned;
import android.text.style.CharacterStyle;

/** On-device opt-in outgoing text sanitizer, independent of MTProxy and web requests. */
public final class UltLinkSanitizer {
    private static final String PREFS = "ult_link_sanitizer_v1";
    private UltLinkSanitizer() { }

    public static boolean isEnabled(Context context, int account) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getBoolean("enabled_" + account, false);
    }

    public static void setEnabled(Context context, int account, boolean enabled) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit().putBoolean("enabled_" + account, enabled).apply();
    }

    public static CharSequence sanitizeOutgoing(Context context, int account, CharSequence text) {
        if (text == null || !isEnabled(context, account)) return text;
        // Rich text can carry mention/explicit-URL/formatting spans whose ranges
        // and targets must not be silently changed. Leave rich messages untouched.
        if (text instanceof Spanned
                && ((Spanned) text).getSpans(0, text.length(), CharacterStyle.class).length > 0) {
            return text;
        }
        String plain = text.toString();
        String cleaned = UltLinkSanitizerPolicy.cleanText(plain);
        return cleaned.equals(plain) ? text : cleaned;
    }
}

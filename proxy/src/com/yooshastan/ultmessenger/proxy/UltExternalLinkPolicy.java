package com.yooshastan.ultmessenger.proxy;

import android.content.Context;
import android.net.Uri;

/** Optional, on-device confirmation for HTTP(S) links opened by Browser.openUrl. */
public final class UltExternalLinkPolicy {
    private static final String PREFERENCES = "ult_external_links";
    private static final String CONFIRM_KEY = "confirm_external_http_links";

    private UltExternalLinkPolicy() { }

    public static boolean isEnabled(Context context) {
        return context != null && context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
                .getBoolean(CONFIRM_KEY, false);
    }

    public static void setEnabled(Context context, boolean enabled) {
        if (context != null) {
            context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
                    .edit().putBoolean(CONFIRM_KEY, enabled).apply();
        }
    }

    public static boolean shouldConfirm(Context context, Uri url, boolean internalTelegramLink) {
        return isEnabled(context) && url != null &&
                UltExternalLinkRules.requiresConfirmation(url.getScheme(), internalTelegramLink);
    }
}

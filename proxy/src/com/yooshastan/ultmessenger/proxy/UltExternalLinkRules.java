package com.yooshastan.ultmessenger.proxy;

/** Pure policy predicate; does not claim to control background HTTP or DNS. */
public final class UltExternalLinkRules {
    private UltExternalLinkRules() { }

    public static boolean requiresConfirmation(String scheme, boolean telegramInternal) {
        if (telegramInternal || scheme == null) {
            return false;
        }
        return "http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme);
    }
}

package com.yooshastan.ultmessenger.translation;

/** Pure, testable privacy policy used by the Alpha 0.9 translation menu. */
public final class UltTranslationPolicy {
    private UltTranslationPolicy() {}

    public static boolean isOfflineMode(String mode) {
        return "offline".equals(mode);
    }

    public static boolean mayTranslate(boolean secretChat, String mode, boolean providerReady) {
        if (isOfflineMode(mode)) {
            return providerReady;
        }
        return !secretChat;
    }

    public static boolean requiresRemoteConsent(boolean secretChat, String mode) {
        return !secretChat && !isOfflineMode(mode);
    }

    public static String engineLabel(String mode) {
        if ("offline".equals(mode)) return "Offline (device only)";
        if ("alternative".equals(mode)) return "Alternative HTTP service";
        if ("cloud".equals(mode)) return "Telegram cloud";
        return "Telegram default translation";
    }
}

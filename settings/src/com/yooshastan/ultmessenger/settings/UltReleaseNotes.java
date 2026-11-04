package com.yooshastan.ultmessenger.settings;

/** Source-owned, truthful release notes for the 0.13c+d candidate. */
public final class UltReleaseNotes {
    public static final String PREFERENCES = "ult_release_notes";
    public static final String DISPLAY_VERSION = "Ultgram Beta 0.3 Combined RC1";

    private UltReleaseNotes() {}

    public static String androidPackageVersion(String versionName, long versionCode) {
        final String name = versionName == null || versionName.trim().isEmpty()
                ? "unknown" : versionName.trim();
        return name + " (code " + versionCode + ")";
    }

    public static String seenKey(String androidVersion) {
        return "shown_" + DISPLAY_VERSION.replace(' ', '_')
                + "_" + (androidVersion == null ? "unknown"
                    : Integer.toHexString(androidVersion.hashCode()));
    }

    public static String message(String androidVersion) {
        return "ULTGRAM BETA 0.3 — CONSOLIDATED INTERNAL CANDIDATE\n\n"
                + "BETA 0.1 · INTERFACE STABILIZATION\n"
                + "• Telegram's own folder tabs and bottom navigation are the only category bars. "
                + "The overlapping floating Ultgram strip is gone.\n"
                + "• Message long-press has one ULT Copy reference action; "
                + "eligible documents additionally offer Copy file info. "
                + "Telegram's normal Message Details remains available.\n"
                + "• Existing downloads, proxy, translation, story and cache behavior remains.\n\n"
                + "BETA 0.2 · ADVANCED SEARCH\n"
                + "• Chats menu → ULT: Advanced search opens Telegram's native full "
                + "message/media search with sender and date filters. "
                + "No second search index, storage, or message export.\n\n"
                + "BETA 0.3 · CONNECTION RELIABILITY\n"
                + "• Existing Advanced → Network diagnostics now explains Telegram's "
                + "connection, proxy, and network state in plain language. "
                + "The native strict-proxy implementation was not changed.\n\n"
                + "LIMITS\n"
                + "• Connection status is not independent DNS/media/call route proof. "
                + "No automatic direct fallback or whole-device kill switch is added.\n"
                + "• Debug-signed internal build. Public signing, upgrade verification "
                + "and device regression checks are pending.\n\n"
                + "Android package: " + androidVersion;

    }
}

package com.yooshastan.ultmessenger.settings;

/** Source-owned, truthful release notes for the 0.13c+d candidate. */
public final class UltReleaseNotes {
    public static final String PREFERENCES = "ult_release_notes";
    public static final String DISPLAY_VERSION = "Ultgram Alpha 0.13c+d RC1";

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
        return "WHAT'S NEW\n\n"
                + "• Download shortcut: Chats → ⋮ → ULT: Downloads.\n"
                + "• Real Save As: long-press an already downloaded, permitted document "
                + "in ULT Downloads, then choose an Android document location.\n"
                + "• Custom theme colors and native download queue from Alpha 0.13a+b "
                + "remain available in Ultgram Settings.\n\n"
                + "Android package: " + androidVersion + "\n"
                + "Ultgram source candidate: " + DISPLAY_VERSION + "\n\n"
                + "Debug-signed internal testing build. A proxy is not a "
                + "verified whole-app kill switch, and protected media cannot be exported.";
    }
}

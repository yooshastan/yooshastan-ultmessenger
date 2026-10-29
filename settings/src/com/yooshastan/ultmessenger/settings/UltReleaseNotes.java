package com.yooshastan.ultmessenger.settings;

/** Source-owned, truthful release notes for the 0.13c+d candidate. */
public final class UltReleaseNotes {
    public static final String PREFERENCES = "ult_release_notes";
    public static final String DISPLAY_VERSION = "Ultgram Alpha 0.14a+b RC1";

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
                + "• NEW: ULT Downloads → long-press a permitted downloaded document "
                + "→ Inspect file and SHA-256 or Save As. A local checksum is only "
                + "copied when you tap Copy SHA-256 (up to 512 MiB).\n"
                + "• NEW: Ultgram Settings → Custom ULT theme colors → Preview colors. "
                + "See accent and both bubble samples without applying.\n"
                + "• From 0.13c+d: Chats → ⋮ → ULT: Downloads, and user-selected "
                + "Save As for eligible files still work.\n\n"
                + "Android package: " + androidVersion + "\n"
                + "Ultgram source candidate: " + DISPLAY_VERSION + "\n\n"
                + "Debug-signed internal testing build. A proxy is not a "
                + "verified whole-app kill switch, and protected media cannot be exported.";
    }
}

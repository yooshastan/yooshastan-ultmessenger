package com.yooshastan.ultmessenger.settings;

/** Source-owned, truthful release notes for the 0.13c+d candidate. */
public final class UltReleaseNotes {
    public static final String PREFERENCES = "ult_release_notes";
    public static final String DISPLAY_VERSION = "Ultgram Beta 0.1 Quality Pass RC2";

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
        return "BETA 0.1 — INTERNAL QUALITY PASS\\n\\n"
                + "• Ultgram Settings now has a shorter, clearer menu.\\n"
                + "• Downloads is the single place for queue, recent files, Save As, "
                + "network rules and storage links. Rare actions are under More.\\n"
                + "• Privacy keeps real controls visible: proxy selection, native strict "
                + "guard, screenshot protection, external links and local identity cover.\\n"
                + "• Network diagnostics are in Advanced and remain read-only. "
                + "They do not verify all network traffic routes.\\n"
                + "• Inactive DoQ and tracker-rule previews are no longer promoted "
                + "as usable privacy protections.\\n\\n"
                + "UNCHANGED: Messaging, translation, eligible story saving, "
                + "native proxy behavior and existing cache preferences.\\n\\n"
                + "This is a debug-signed internal candidate, not a public beta. "
                + "Protected media cannot be exported through Ultgram tools.\\n\\n"
                + "Android package: " + androidVersion;

    }
}

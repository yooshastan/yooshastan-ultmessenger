package com.yooshastan.ultmessenger.settings;

/** Source-owned, truthful release notes for the 0.13c+d candidate. */
public final class UltReleaseNotes {
    public static final String PREFERENCES = "ult_release_notes";
    public static final String DISPLAY_VERSION = "Ultgram Alpha 0.14 cache RC1";

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
                + "• NEW: Ultgram Settings → Storage and Cache → Custom maximum cache "
                + "size. Choose 2–32 GB in 1 GB steps (8/10/12 GB included) or "
                + "No Limit. Uses Telegram's existing cache maintenance; does not "
                + "delete anything immediately.\n"
                + "• Stabilization from 0.14d+: result privacy after switching accounts "
                + "while verifying a saved file; same chat, media and story tools.\n"
                + "• Signing metadata: verify APK package/version and certificate "
                + "SHA-256. In-place upgrade compatibility is NOT checked automatically.\n"
                + "• NEW: ULT Downloads → long-press permitted file → Verify saved copy. "
                + "Choose a file in Android picker; compare size and local SHA-256 "
                + "without uploading or overwriting either file (max 512 MiB).\n"
                + "• NEW: Ultgram Settings → Message font and bubble corners. "
                + "Edit native Telegram chat text size and bubble shape; Apply or Reset.\n"
                + "• NEW: Privacy and Network → Local account identity cover "
                + "(Ultgram Settings only); Manual network egress evidence "
                + "(user-marked checklist, NOT route verified).\n"
                + "• From 0.14c: Open an eligible photo/video story and tap "
                + "the Gallery icon beside ⋮ to Save Story to Gallery. "
                + "Your own or Premium-eligible public stories only; no protected "
                + "or live stories. Already cached media is required.\n"
                + "• From 0.14a+b: ULT Downloads → long-press a permitted downloaded document "
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

package com.yooshastan.ultmessenger.settings;

/** Minimal on-device release notes. No account data, I/O, or marketing claims. */
public final class UltReleaseNotes {
    public static final String MILESTONE = "Alpha 0.13c+d";
    public static final String PREFS = "ult_feature_discovery";
    public static final String SEEN_KEY = "seen_alpha13cd";

    private UltReleaseNotes() {}

    public static String summary() {
        return "NEW: Fixed custom ULT theme accents and outgoing bubbles, "
                + "with contrast checks. Find them in Ultgram Settings > Appearance.\n\n"
                + "NEW ENTRY: Active/recent downloads queue, using Telegram's existing "
                + "FileLoader. Available from Chats > menu > ULT: Downloads and "
                + "Ultgram Settings > START HERE.\n\n"
                + "NEW: Chats menu now links directly to What's New and Downloads. "
                + "The first-run discovery notice can be dismissed and reopened later.\n\n"
                + "SHORTCUT ONLY: Mobile/Wi-Fi/roaming auto-download policies are "
                + "Telegram's existing settings, not a new downloader.\n\n"
                + "EARLIER 0.12: Category swipe pad/typography and message/media "
                + "reference copying.\n\n"
                + "NOT INCLUDED: Save As/export, custom file manager, No identity masking, "
                + "complete network leak protection, DoQ/DoH3 or enforced tracker filtering.";
    }
}

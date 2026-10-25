package com.yooshastan.ultmessenger.conversation;

/**
 * Alpha 0.10a local chat view presets.
 * Only presentation policy: never changes TLRPC.Dialog or account storage.
 */
public final class UltChatView {
    public static final String PREFERENCES = "ult_chat_views";
    public static final String ALL = "all";
    public static final String UNREAD = "unread";
    public static final String GROUPS = "groups";
    public static final String CHANNELS = "channels";
    public static final String BOTS = "bots";
    public static final String PEOPLE = "people";
    public static final String UNREAD_FIRST = "unread_first";
    public static final String[] MODES = {ALL, UNREAD, GROUPS, CHANNELS, BOTS, PEOPLE, UNREAD_FIRST};
    private static final String[] QUICK_TABS = {ALL, UNREAD, GROUPS, CHANNELS, BOTS, PEOPLE};
    public static final String[] LABELS = {
        "All chats (Telegram order)", "Unread only", "Groups only", "Channels only",
        "Bots only", "People / private chats", "Unread first (pinned stay on top)"
    };

    private UltChatView() {}

    public static String accountKey(int account) {
        if (account < 0) throw new IllegalArgumentException("Invalid account");
        return "chat_view_account_" + account;
    }

    public static String sanitize(String mode) {
        if (mode != null) {
            for (String candidate : MODES) if (candidate.equals(mode)) return candidate;
        }
        return ALL;
    }

    /** Cycles opt-in display tabs; the unread-first sorting preset is not a category. */
    public static String nextQuickTab(String currentMode) {
        String safe = sanitize(currentMode);
        for (int i = 0; i < QUICK_TABS.length; i++) {
            if (QUICK_TABS[i].equals(safe)) {
                return QUICK_TABS[(i + 1) % QUICK_TABS.length];
            }
        }
        return ALL;
    }

    public static int modeIndex(String mode) {
        String safe = sanitize(mode);
        for (int i = 0; i < MODES.length; i++) if (safe.equals(MODES[i])) return i;
        return 0;
    }

    public static boolean accepts(String mode, boolean unread, boolean group,
                                  boolean channel, boolean bot) {
        switch (sanitize(mode)) {
            case UNREAD: return unread;
            case GROUPS: return group;
            case CHANNELS: return channel;
            case BOTS: return bot;
            case PEOPLE: return !group && !channel && !bot;
            default: return true; // All and Unread First include all normal dialogs.
        }
    }

    /** Stable sort: archive row and existing pinned order stay above unread grouping. */
    public static int compareUnreadFirst(boolean aArchive, boolean bArchive,
                                         boolean aPinned, boolean bPinned,
                                         boolean aUnread, boolean bUnread) {
        if (aArchive != bArchive) return aArchive ? -1 : 1;
        if (aPinned != bPinned) return aPinned ? -1 : 1;
        if (aPinned && bPinned) return 0;
        if (aUnread != bUnread) return aUnread ? -1 : 1;
        return 0;
    }
}

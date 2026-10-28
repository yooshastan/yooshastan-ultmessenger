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
    public static final String[] QUICK_TAB_MODES = {ALL, UNREAD, GROUPS, CHANNELS, BOTS, PEOPLE};
    public static final String[] QUICK_TAB_LABELS = {"All", "Unread", "Groups", "Channels", "Bots", "People"};
    public static final String[] LABELS = {
        "All chats (Telegram order)", "Unread only", "Groups only", "Channels only",
        "Bots only", "People / private chats", "Unread first (pinned stay on top)"
    };

    // Alpha 0.12a: appearance only; independent per-account local UI preference.
    public static final String BAR_COMPACT = "compact";
    public static final String BAR_COMFORTABLE = "comfortable";
    public static final String BAR_SPACIOUS = "spacious";
    public static final String[] BAR_STYLES = {BAR_COMPACT, BAR_COMFORTABLE, BAR_SPACIOUS};
    public static final String[] BAR_STYLE_LABELS = {
        "Compact chips", "Comfortable (default)", "Spacious chips"
    };

    public static String barStyleKey(int account) {
        if (account < 0) throw new IllegalArgumentException("Invalid account");
        return "chat_bar_style_account_" + account;
    }

    public static String sanitizeBarStyle(String saved) {
        for (String style : BAR_STYLES) if (style.equals(saved)) return style;
        return BAR_COMFORTABLE;
    }

    public static int barStyleIndex(String saved) {
        String safe = sanitizeBarStyle(saved);
        for (int i = 0; i < BAR_STYLES.length; i++) {
            if (BAR_STYLES[i].equals(safe)) return i;
        }
        return 1;
    }

    public static int barHeightDp(String saved) {
        switch (sanitizeBarStyle(saved)) {
            case BAR_COMPACT: return 38;
            case BAR_SPACIOUS: return 54;
            default: return 44;
        }
    }

    public static int chipHeightDp(String saved) {
        switch (sanitizeBarStyle(saved)) {
            case BAR_COMPACT: return 30;
            case BAR_SPACIOUS: return 42;
            default: return 34;
        }
    }

    public static int chipPaddingDp(String saved) {
        switch (sanitizeBarStyle(saved)) {
            case BAR_COMPACT: return 10;
            case BAR_SPACIOUS: return 20;
            default: return 14;
        }
    }

    private UltChatView() {}

    public static String quickBarKey(int account) {
        if (account < 0) throw new IllegalArgumentException("Invalid account");
        return "chat_bar_account_" + account;
    }

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


    /** Category order/visibility lives in local per-account UI preferences only. */
    public static String tabConfigKey(int account) {
        if (account < 0) throw new IllegalArgumentException("Invalid account");
        return "chat_tabs_account_" + account;
    }

    public static String defaultTabConfig() {
        return "all,unread,groups,channels,bots,people";
    }

    public static boolean isQuickTab(String mode) {
        if (mode == null) return false;
        for (String candidate : QUICK_TAB_MODES) {
            if (candidate.equals(mode)) return true;
        }
        return false;
    }

    public static String[] visibleTabs(String saved) {
        java.util.ArrayList<String> modes = new java.util.ArrayList<>();
        modes.add(ALL);
        if (saved == null) saved = defaultTabConfig();
        for (String token : saved.split(",", -1)) {
            String mode = token.trim();
            if (isQuickTab(mode) && !ALL.equals(mode) && !modes.contains(mode)) {
                modes.add(mode);
            }
        }
        return modes.toArray(new String[0]);
    }

    public static String serializeTabs(String[] modes) {
        return String.join(",", visibleTabs(String.join(",", modes)));
    }

    public static boolean isTabVisible(String saved, String mode) {
        for (String visible : visibleTabs(saved)) {
            if (visible.equals(mode)) return true;
        }
        return false;
    }

    public static String changeTabVisibility(String saved, String mode, boolean visible) {
        if (!isQuickTab(mode)) throw new IllegalArgumentException("Unknown chat tab");
        if (ALL.equals(mode)) return serializeTabs(visibleTabs(saved));
        java.util.ArrayList<String> modes = new java.util.ArrayList<>(
                java.util.Arrays.asList(visibleTabs(saved)));
        modes.remove(mode);
        if (visible) modes.add(mode);
        return String.join(",", modes);
    }

    public static String moveTab(String saved, String mode, int direction) {
        if (direction != -1 && direction != 1) throw new IllegalArgumentException("Direction");
        java.util.ArrayList<String> modes = new java.util.ArrayList<>(
                java.util.Arrays.asList(visibleTabs(saved)));
        int index = modes.indexOf(mode);
        int to = index + direction;
        if (index <= 0 || to <= 0 || to >= modes.size()) return String.join(",", modes);
        java.util.Collections.swap(modes, index, to);
        return String.join(",", modes);
    }

    public static String quickTabLabel(String mode) {
        for (int i = 0; i < QUICK_TAB_MODES.length; i++) {
            if (QUICK_TAB_MODES[i].equals(mode)) return QUICK_TAB_LABELS[i];
        }
        return QUICK_TAB_LABELS[0];
    }

    public static String nextVisibleTab(String current, String saved) {
        String[] tabs = visibleTabs(saved);
        for (int i = 0; i < tabs.length; i++) {
            if (tabs[i].equals(current)) return tabs[(i + 1) % tabs.length];
        }
        return ALL;
    }

    /** Cycles opt-in display tabs; the unread-first sorting preset is not a category. */
    public static String nextQuickTab(String currentMode) {
        String safe = sanitize(currentMode);
        for (int i = 0; i < QUICK_TAB_MODES.length; i++) {
            if (QUICK_TAB_MODES[i].equals(safe)) {
                return QUICK_TAB_MODES[(i + 1) % QUICK_TAB_MODES.length];
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

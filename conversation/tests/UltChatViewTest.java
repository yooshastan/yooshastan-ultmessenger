import com.yooshastan.ultmessenger.conversation.UltChatView;

public final class UltChatViewTest {
    private static void ok(boolean yes, String label) {
        if (!yes) throw new AssertionError(label);
    }
    public static void main(String[] args) {
        ok(UltChatView.ALL.equals(UltChatView.sanitize(null)), "unset defaults all");
        ok(UltChatView.ALL.equals(UltChatView.sanitize("bogus")), "unknown defaults all");
        ok(UltChatView.MODES.length == UltChatView.LABELS.length, "labels match keys");
        ok(UltChatView.QUICK_TAB_MODES.length == 6, "six categories");
        ok(UltChatView.visibleTabs(null).length == 6, "default six visible");
        ok("groups".equals(UltChatView.stepVisibleTab("all", "all,groups,bots", 1)),
                "swipe forward visible only");
        ok("bots".equals(UltChatView.stepVisibleTab("all", "all,groups,bots", -1)),
                "swipe backwards wraps");
        ok("all".equals(UltChatView.stepVisibleTab("missing", "all,bots", -1)),
                "missing active tab falls back");
        boolean badDirection = false;
        try { UltChatView.stepVisibleTab("all", null, 0); }
        catch (IllegalArgumentException expected) { badDirection = true; }
        ok(badDirection, "swipe direction constrained");
        ok(UltChatView.barFontSp("small") == 12 && UltChatView.barFontSp("large") == 16,
                "category text sizes");
        ok(UltChatView.barFontSp("bogus") == 13, "unknown font fallback");
        ok(!UltChatView.swipePadKey(0).equals(UltChatView.swipePadKey(1)),
                "gesture prefs account isolated");
        ok(!UltChatView.barFontKey(0).equals(UltChatView.barFontKey(1)),
                "typography prefs account isolated");
        ok("comfortable".equals(UltChatView.sanitizeBarStyle("untrusted")),
                "invalid category appearance uses the existing default");
        ok("compact".equals(UltChatView.sanitizeBarStyle("compact")), "compact style");
        ok(UltChatView.barHeightDp("compact") < UltChatView.barHeightDp("spacious"),
                "density visibly changes category strip height");
        ok(UltChatView.chipHeightDp("compact") < UltChatView.chipHeightDp("spacious"),
                "density changes chip height");
        ok(UltChatView.chipPaddingDp("compact") < UltChatView.chipPaddingDp("spacious"),
                "density changes chip spacing");
        ok(UltChatView.barStyleIndex("invalid") == 1, "comfortable default selected");
        ok(!UltChatView.barStyleKey(0).equals(UltChatView.barStyleKey(1)),
                "category appearance is per-account");
        ok("all,groups,bots".equals(UltChatView.changeTabVisibility(
                "all,unread,groups,bots", "unread", false)), "hide tab");
        ok("all,groups,bots,unread".equals(UltChatView.changeTabVisibility(
                "all,groups,bots", "unread", true)), "show tab");
        ok("all,bots,groups".equals(UltChatView.moveTab(
                "all,groups,bots", "bots", -1)), "move earlier");
        ok("all,bots,groups".equals(UltChatView.moveTab(
                "all,groups,bots", "groups", 1)), "move later");
        ok("all,groups".equals(String.join(",", UltChatView.visibleTabs(
                "groups,groups,evil,all"))), "dedupe malformed inputs");
        ok("all".equals(String.join(",", UltChatView.visibleTabs("all"))),
                "all never disappears");
        ok("all".equals(UltChatView.nextVisibleTab("groups", "all")),
                "hidden current falls back to All");
        ok(UltChatView.GROUPS.equals(UltChatView.nextVisibleTab("all", "all,groups")),
                "cycle only visible");
        ok(!UltChatView.tabConfigKey(0).equals(UltChatView.tabConfigKey(1)),
                "tab preferences per account");
        ok(UltChatView.QUICK_TAB_MODES.length == UltChatView.QUICK_TAB_LABELS.length, "category labels match");
        ok(!UltChatView.quickBarKey(0).equals(UltChatView.quickBarKey(1)), "separate per-account bar visibility");
        ok(UltChatView.modeIndex("bots") == 4, "bot index");
        ok(UltChatView.modeIndex("people") == 5, "people index");
        ok(UltChatView.PEOPLE.equals(UltChatView.nextQuickTab(UltChatView.BOTS)), "cycle to people");
        ok(UltChatView.ALL.equals(UltChatView.nextQuickTab(UltChatView.PEOPLE)), "cycle wraps to all");
        ok(UltChatView.ALL.equals(UltChatView.nextQuickTab(UltChatView.UNREAD_FIRST)), "sorting mode exits to all");
        ok(!UltChatView.accountKey(0).equals(UltChatView.accountKey(1)), "per-account isolation");
        boolean badKey = false;
        try { UltChatView.accountKey(-1); } catch (IllegalArgumentException expected) { badKey = true; }
        ok(badKey, "invalid accounts fail closed");
        ok(UltChatView.accepts(UltChatView.ALL, false, false, false, false), "all accepts");
        ok(UltChatView.accepts(UltChatView.UNREAD, true, false, false, false), "unread accepts");
        ok(!UltChatView.accepts(UltChatView.UNREAD, false, true, false, false), "read filtered");
        ok(UltChatView.accepts(UltChatView.GROUPS, false, true, false, false), "groups");
        ok(!UltChatView.accepts(UltChatView.GROUPS, false, false, true, false), "channels not groups");
        ok(UltChatView.accepts(UltChatView.CHANNELS, false, false, true, false), "channels");
        ok(UltChatView.accepts(UltChatView.BOTS, false, false, false, true), "bots");
        ok(UltChatView.accepts(UltChatView.PEOPLE, false, false, false, false), "people");
        ok(!UltChatView.accepts(UltChatView.PEOPLE, false, false, false, true), "people exclude bots");
        ok(!UltChatView.accepts(UltChatView.PEOPLE, false, true, false, false), "people exclude groups");
        ok(!UltChatView.accepts(UltChatView.BOTS, false, false, false, false), "normal users not bots");
        ok(UltChatView.accepts(UltChatView.UNREAD_FIRST, false, false, false, false), "sort mode retains all");
        ok(UltChatView.compareUnreadFirst(true, false, false, true, false, true) < 0, "archive first");
        ok(UltChatView.compareUnreadFirst(false, false, true, false, false, true) < 0, "pinned ahead unread");
        ok(UltChatView.compareUnreadFirst(false, false, false, false, true, false) < 0, "unread first");
        ok(UltChatView.compareUnreadFirst(false, false, true, true, false, true) == 0, "pinned stable");
        ok(UltChatView.compareUnreadFirst(false, false, false, false, false, false) == 0, "read stable");
    }
}

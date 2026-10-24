import com.yooshastan.ultmessenger.conversation.UltChatView;

public final class UltChatViewTest {
    private static void ok(boolean yes, String label) {
        if (!yes) throw new AssertionError(label);
    }
    public static void main(String[] args) {
        ok(UltChatView.ALL.equals(UltChatView.sanitize(null)), "unset defaults all");
        ok(UltChatView.ALL.equals(UltChatView.sanitize("bogus")), "unknown defaults all");
        ok(UltChatView.MODES.length == UltChatView.LABELS.length, "labels match keys");
        ok(UltChatView.modeIndex("bots") == 4, "bot index");
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
        ok(!UltChatView.accepts(UltChatView.BOTS, false, false, false, false), "normal users not bots");
        ok(UltChatView.accepts(UltChatView.UNREAD_FIRST, false, false, false, false), "sort mode retains all");
        ok(UltChatView.compareUnreadFirst(true, false, false, true, false, true) < 0, "archive first");
        ok(UltChatView.compareUnreadFirst(false, false, true, false, false, true) < 0, "pinned ahead unread");
        ok(UltChatView.compareUnreadFirst(false, false, false, false, true, false) < 0, "unread first");
        ok(UltChatView.compareUnreadFirst(false, false, true, true, false, true) == 0, "pinned stable");
        ok(UltChatView.compareUnreadFirst(false, false, false, false, false, false) == 0, "read stable");
    }
}

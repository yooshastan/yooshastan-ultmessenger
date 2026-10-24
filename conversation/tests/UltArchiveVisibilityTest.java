import com.yooshastan.ultmessenger.conversation.UltArchiveVisibility;

public final class UltArchiveVisibilityTest {
    private static void check(boolean value, String label) {
        if (!value) throw new AssertionError(label);
    }

    public static void main(String[] args) {
        check(!UltArchiveVisibility.shouldHideArchiveRow(false, 0, 0, false), "default shows archive");
        check(UltArchiveVisibility.shouldHideArchiveRow(true, 0, 0, false), "opt-in hides main row");
        check(!UltArchiveVisibility.shouldHideArchiveRow(true, 0, 1, false), "archive stays accessible");
        check(!UltArchiveVisibility.shouldHideArchiveRow(true, 7, 0, false), "custom tabs intact");
        check(!UltArchiveVisibility.shouldHideArchiveRow(true, 0, 0, true), "pickers unaffected");
        check(!UltArchiveVisibility.accountKey(0).equals(UltArchiveVisibility.accountKey(1)), "account isolation");
        boolean threw = false;
        try { UltArchiveVisibility.accountKey(-1); } catch (IllegalArgumentException expected) { threw = true; }
        check(threw, "invalid account must fail closed");
    }
}

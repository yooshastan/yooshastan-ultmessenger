import com.yooshastan.ultmessenger.texts.UltTextsAccessPolicy;

public final class UltTextsAccessPolicyTest {
    private static void check(boolean ok, String reason) {
        if (!ok) throw new AssertionError(reason);
    }
    public static void main(String[] args) {
        check(UltTextsAccessPolicy.evaluate(false, true, false, false, false,
                false, false, false) == UltTextsAccessPolicy.Status.DISABLED,
                "not enabled by default");
        check(UltTextsAccessPolicy.evaluate(true, false, false, false, false,
                false, false, false) == UltTextsAccessPolicy.Status.TELEPHONY_UNAVAILABLE,
                "no telephony");
        check(UltTextsAccessPolicy.evaluate(true, true, false, false, false,
                false, false, false) == UltTextsAccessPolicy.Status.DEFAULT_SMS_ROLE_REQUIRED,
                "must be default SMS app");
        check(UltTextsAccessPolicy.evaluate(true, true, true, true, true,
                true, false, true) == UltTextsAccessPolicy.Status.SMS_IMPLEMENTATION_INCOMPLETE,
                "MMS handling is mandatory");
        check(UltTextsAccessPolicy.evaluate(true, true, true, true, true,
                true, true, false) == UltTextsAccessPolicy.Status.SMS_IMPLEMENTATION_INCOMPLETE,
                "provider handlers are mandatory");
        final UltTextsAccessPolicy.Status ready = UltTextsAccessPolicy.evaluate(true,
                true, true, true, true, true, true, true);
        check(UltTextsAccessPolicy.mayAccessSmsProvider(ready), "ready");
        check(!UltTextsAccessPolicy.mayAccessSmsProvider(
                UltTextsAccessPolicy.Status.DEFAULT_SMS_ROLE_REQUIRED),
                "never read without role");
        check(!UltTextsAccessPolicy.mayReadRcsProvider(), "never claim RCS access");
        check(UltTextsAccessPolicy.folderLabel().equals("Texts (SMS/MMS)"),
                "folder must not claim to contain RCS");
        System.out.println("UltTextsAccessPolicyTest OK");
    }
}

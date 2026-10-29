import com.yooshastan.ultmessenger.stories.UltStorySavePolicy;

public final class UltStorySavePolicyTest {
    private static void check(boolean value, String why) {
        if (!value) throw new AssertionError(why);
    }
    private static boolean allowed(boolean owner, boolean premium, boolean share) {
        return UltStorySavePolicy.eligible(true,false,false,false,true,
                owner,premium,share,false);
    }
    public static void main(String[] args) {
        check(allowed(true,false,false), "owner can save eligible own story");
        check(allowed(false,true,true), "premium can save eligible public story");
        check(!allowed(false,false,true), "cannot bypass native Premium save restriction");
        check(!allowed(false,true,false), "private/expired story must not be saved");
        check(!UltStorySavePolicy.eligible(true,false,false,false,false,true,true,true,false),
                "noforwards story");
        check(!UltStorySavePolicy.eligible(true,false,false,false,true,true,true,true,true),
                "protected channel");
        check(!UltStorySavePolicy.eligible(false,false,false,false,true,true,true,true,false),
                "missing story media");
        check(!UltStorySavePolicy.eligible(true,true,false,false,true,true,true,true,false),
                "skipped story");
        check(!UltStorySavePolicy.eligible(true,false,true,false,true,true,true,true,false),
                "unsupported story");
        check(!UltStorySavePolicy.eligible(true,false,false,true,true,true,true,true,false),
                "live story");
        System.out.println("ULT story save eligibility policy passed");
    }
}

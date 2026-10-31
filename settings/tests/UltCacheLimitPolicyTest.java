import com.yooshastan.ultmessenger.settings.UltCacheLimitPolicy;
public final class UltCacheLimitPolicyTest {
    private static void ok(boolean yes, String reason) {
        if (!yes) throw new AssertionError(reason);
    }
    public static void main(String[] args) {
        for (int i=2; i<=32; i++) {
            ok(UltCacheLimitPolicy.isAllowed(i), "supports every whole GB");
            ok(UltCacheLimitPolicy.initialGb(i)==i, "preserves exact preference");
            ok(UltCacheLimitPolicy.label(i).equals(i+" GB"), "honest label");
        }
        for (int v : new int[]{-1,0,1,33,64}) {
            ok(!UltCacheLimitPolicy.isAllowed(v), "reject invalid or reserved value");
        }
        ok(UltCacheLimitPolicy.isAllowed(Integer.MAX_VALUE), "supports no limit");
        ok("No Limit".equals(UltCacheLimitPolicy.label(Integer.MAX_VALUE)), "unlimited label");
        ok("300 MB (legacy)".equals(UltCacheLimitPolicy.label(1)),
                "legacy sentinel is never called 1GB");
        ok(UltCacheLimitPolicy.initialGb(Integer.MAX_VALUE)==10,
                "suggest 10GB but never write until explicit Apply");
        System.out.println("ULT cache limit policy checks passed");
    }
}

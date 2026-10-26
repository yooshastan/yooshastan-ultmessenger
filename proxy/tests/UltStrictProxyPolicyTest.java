import com.yooshastan.ultmessenger.proxy.UltStrictProxyPolicy;

public final class UltStrictProxyPolicyTest {
    private static void check(boolean result, String message) {
        if (!result) throw new AssertionError(message);
    }

    public static void main(String[] args) {
        check(UltStrictProxyPolicy.canEnable(true, true, false),
                "valid selected proxy can enable guard");
        check(!UltStrictProxyPolicy.canEnable(false, true, false),
                "proxy switch off must reject strict enable");
        check(!UltStrictProxyPolicy.canEnable(true, false, false),
                "invalid selection must reject strict enable");
        check(!UltStrictProxyPolicy.canEnable(true, true, true),
                "Mercurygram Tor must remain separate");
        check(!UltStrictProxyPolicy.mayDisableProxy(true),
                "strict state must block silent proxy disable");
        check(UltStrictProxyPolicy.mayDisableProxy(false),
                "proxy disable allowed when strict off");
        check(UltStrictProxyPolicy.nativeStatus(true, true).contains("native observed"),
                "native state advertised as observed");
        check(UltStrictProxyPolicy.nativeStatus(true, false).contains("OUT OF SYNC"),
                "async mismatch disclosed");
        check(UltStrictProxyPolicy.nativeStatus(false, true).contains("native observed ON"),
                "stale guard after off is visible");
        System.out.println("ULT strict proxy policy passed");
    }
}

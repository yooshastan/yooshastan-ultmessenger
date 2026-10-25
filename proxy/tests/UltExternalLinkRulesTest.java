import com.yooshastan.ultmessenger.proxy.UltExternalLinkRules;

public class UltExternalLinkRulesTest {
    private static void check(boolean actual, boolean expected, String caseName) {
        if (actual != expected) {
            throw new AssertionError(caseName + ": expected " + expected + ", got " + actual);
        }
    }

    public static void main(String[] args) {
        check(UltExternalLinkRules.requiresConfirmation("https", false), true, "external https");
        check(UltExternalLinkRules.requiresConfirmation("HTTP", false), true, "external http");
        check(UltExternalLinkRules.requiresConfirmation("https", true), false, "internal Telegram link");
        check(UltExternalLinkRules.requiresConfirmation("tg", false), false, "Telegram scheme");
        check(UltExternalLinkRules.requiresConfirmation("intent", false), false, "Android intent scheme");
        check(UltExternalLinkRules.requiresConfirmation(null, false), false, "unknown scheme");
        System.out.println("UltExternalLinkRulesTest passed");
    }
}

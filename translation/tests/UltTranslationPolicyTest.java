import com.yooshastan.ultmessenger.translation.UltTranslationPolicy;

public final class UltTranslationPolicyTest {
    private static void ok(boolean condition, String label) {
        if (!condition) throw new AssertionError(label);
    }
    public static void main(String[] args) {
        ok(UltTranslationPolicy.isOfflineMode("offline"), "offline detected");
        ok(!UltTranslationPolicy.isOfflineMode("default"), "default remote");
        ok(UltTranslationPolicy.mayTranslate(false, "offline", true), "installed offline provider");
        ok(!UltTranslationPolicy.mayTranslate(false, "offline", false), "offline missing provider fails closed");
        ok(UltTranslationPolicy.mayTranslate(true, "offline", true), "secret local permitted");
        ok(!UltTranslationPolicy.mayTranslate(true, "offline", false), "secret absent provider blocked");
        ok(!UltTranslationPolicy.mayTranslate(true, "cloud", true), "secret cloud forbidden");
        ok(!UltTranslationPolicy.mayTranslate(true, "alternative", true), "secret HTTP forbidden");
        ok(!UltTranslationPolicy.mayTranslate(true, "default", true), "secret default forbidden");
        ok(UltTranslationPolicy.requiresRemoteConsent(false, "cloud"), "cloud consent");
        ok(UltTranslationPolicy.requiresRemoteConsent(false, "alternative"), "HTTP consent");
        ok(UltTranslationPolicy.requiresRemoteConsent(false, "default"), "upstream consent");
        ok(!UltTranslationPolicy.requiresRemoteConsent(false, "offline"), "offline no remote consent");
        ok(!UltTranslationPolicy.requiresRemoteConsent(true, "cloud"), "secret remote never offered");
        ok(UltTranslationPolicy.engineLabel("offline").contains("device"), "correct offline disclosure");
    }
}

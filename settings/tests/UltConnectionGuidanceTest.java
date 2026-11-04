import com.yooshastan.ultmessenger.settings.UltConnectionGuidance;

public final class UltConnectionGuidanceTest {
    private static void check(boolean ok, String why) {
        if (!ok) throw new AssertionError(why);
    }
    public static void main(String[] args) {
        check(UltConnectionGuidance.label(3).equals("Connected"), "connected");
        check(UltConnectionGuidance.label(4).equals("Connecting to proxy"), "proxy");
        check(UltConnectionGuidance.label(2).equals("Waiting for network"), "offline");
        check(UltConnectionGuidance.label(999).equals("Unknown state"), "unknown");
        String mismatch = UltConnectionGuidance.advice(3, true, false, "Wi-Fi");
        check(mismatch.contains("mismatch"), "strict guard mismatch prioritized");
        check(!mismatch.contains("protected"), "do not claim verified protection");
        check(UltConnectionGuidance.advice(4, true, true, "Cellular")
                .contains("no direct fallback"), "strict failure guidance");
        check(UltConnectionGuidance.advice(2, true, true, "Offline")
                .contains("does not bypass"), "offline/no fallback");
        check(UltConnectionGuidance.advice(3, true, true, "Wi-Fi")
                .contains("does not independently"), "no route proof");
        System.out.println("UltConnectionGuidanceTest OK");
    }
}

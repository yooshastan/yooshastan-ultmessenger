import com.yooshastan.ultmessenger.settings.UltDnsPosture;

public final class UltDnsPostureTest {
    private static void expect(UltDnsPosture.Mode actual, UltDnsPosture.Mode expected) {
        if (actual != expected) {
            throw new AssertionError("Expected " + expected + " but got " + actual);
        }
    }

    public static void main(String[] args) {
        expect(UltDnsPosture.classify(false, false, null), UltDnsPosture.Mode.UNKNOWN);
        expect(UltDnsPosture.classify(true, false, "resolver.example"), UltDnsPosture.Mode.INACTIVE);
        expect(UltDnsPosture.classify(true, true, null), UltDnsPosture.Mode.OPPORTUNISTIC);
        expect(UltDnsPosture.classify(true, true, "   "), UltDnsPosture.Mode.OPPORTUNISTIC);
        expect(UltDnsPosture.classify(true, true, "resolver.example"), UltDnsPosture.Mode.STRICT_DOT);
        if (!UltDnsPosture.explain(UltDnsPosture.Mode.STRICT_DOT).contains("DNS-over-TLS")
                || !UltDnsPosture.explain(UltDnsPosture.Mode.OPPORTUNISTIC).contains("not an encrypted-only guarantee")
                || UltDnsPosture.explain(UltDnsPosture.Mode.UNKNOWN).contains("active")) {
            throw new AssertionError("DNS state wording must not overclaim encryption");
        }
        System.out.println("ULT DNS posture classification passed");
    }
}

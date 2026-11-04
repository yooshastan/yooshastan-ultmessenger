import com.yooshastan.ultmessenger.settings.UltNetworkSourceMatrix;

public final class UltNetworkSourceMatrixTest {
    public static void main(String[] args) {
        String report = UltNetworkSourceMatrix.render();
        for (String term : new String[] {
                "NATIVE TGNET", "JAVA HTTP", "DNS / DoT / DoQ", "MEDIA / CDN",
                "WEBVIEW", "CALLS / UDP", "phone-edge/gateway",
                "NOT ROUTE-VERIFIED", "NO ACTIVE CAPTURE"}) {
            if (!report.contains(term)) throw new AssertionError("Missing: " + term);
        }
        if (report.contains("leak-proof protection enabled")) {
            throw new AssertionError("Misleading security claim");
        }
    }
}

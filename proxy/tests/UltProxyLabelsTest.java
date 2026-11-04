import com.yooshastan.ultmessenger.proxy.UltProxyLabels;

public final class UltProxyLabelsTest {
    private static void expectFailure(Runnable action) {
        try {
            action.run();
            throw new AssertionError("Expected rejection");
        } catch (IllegalArgumentException expected) {
            // Checked fail-closed behavior.
        }
    }
    public static void main(String[] args) {
        String key = UltProxyLabels.preferenceKey("socks5", "Proxy.Example", 1080);
        if (!key.equals(UltProxyLabels.preferenceKey("SOCKS5", "proxy.example", 1080))) {
            throw new AssertionError("Endpoint normalization is not deterministic");
        }
        if (key.equals(UltProxyLabels.preferenceKey("MTPROTO", "proxy.example", 1080))) {
            throw new AssertionError("Different protocols should not share aliases");
        }
        if (key.contains("proxy.example")) {
            throw new AssertionError("Endpoint leaked into preferences key");
        }
        if (!UltProxyLabels.normalizeLabel("  🌸 Stage Gateway  ").equals("🌸 Stage Gateway")) {
            throw new AssertionError("Unicode label normalization failed");
        }
        if (!UltProxyLabels.normalizeLabel("  ").isEmpty()) {
            throw new AssertionError("Empty value should clear an alias");
        }
        if (!UltProxyLabels.displayName("Home", "example.org:1080").equals("Home · example.org:1080")) {
            throw new AssertionError("Alias display failed");
        }
        if (!UltProxyLabels.displayName("", "example.org:1080").equals("example.org:1080")) {
            throw new AssertionError("Native endpoint fallback failed");
        }
        expectFailure(() -> UltProxyLabels.normalizeLabel("a".repeat(65)));
        expectFailure(() -> UltProxyLabels.normalizeLabel("bad\nlabel"));
        expectFailure(() -> UltProxyLabels.preferenceKey("SOCKS5", "", 1080));
        expectFailure(() -> UltProxyLabels.preferenceKey("SOCKS5", "example.org", -1));
        System.out.println("ULT proxy alias model passed");
    }
}

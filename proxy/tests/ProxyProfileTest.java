import com.yooshastan.ultmessenger.proxy.ProxyProfile;

public final class ProxyProfileTest {
    private static void reject(String host, int port) {
        try {
            new ProxyProfile("Test", ProxyProfile.Protocol.SOCKS5, host, port, true);
            throw new AssertionError("Expected invalid endpoint: " + host + ":" + port);
        } catch (IllegalArgumentException expected) {
            // Rejected before creating a connection.
        }
    }
    public static void main(String[] args) {
        ProxyProfile p = new ProxyProfile("Privacy proxy", ProxyProfile.Protocol.MTPROTO,
                "Proxy.Example", 443, true);
        if (!p.host.equals("proxy.example") || !p.displayEndpoint().equals("proxy.example:443")) {
            throw new AssertionError("Normalization failed");
        }
        reject("https://example.com", 443);
        reject("name@example.com", 1080);
        reject("example.com/path", 1080);
        reject("", 443);
        reject("example.com", 0);
        reject("example.com", 65536);
        System.out.println("Proxy profile validation passed");
    }
}

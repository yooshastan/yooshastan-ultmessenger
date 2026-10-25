import com.yooshastan.ultmessenger.dns.UltDoqEndpoint;
import com.yooshastan.ultmessenger.dns.UltDoqResolver;
import java.io.IOException;
import java.util.Arrays;

public final class UltDoqContractTest {
    private static final byte[] QUERY = new byte[] {
            0, 0, 1, 0, 0, 1, 0, 0, 0, 0, 0, 0,
            7, 'e', 'x', 'a', 'm', 'p', 'l', 'e', 3, 'o', 'r', 'g', 0,
            0, 1, 0, 1
    };

    private static void check(boolean value, String label) {
        if (!value) {
            throw new AssertionError(label);
        }
    }

    private static void mustReject(String endpoint) {
        try {
            UltDoqEndpoint.parse(endpoint);
            throw new AssertionError("Accepted unsafe DoQ endpoint: " + endpoint);
        } catch (IllegalArgumentException expected) {
            // Good: no ambiguous or plaintext endpoints.
        }
    }

    private static void mustFail(UltDoqResolver resolver, byte[] query) {
        try {
            resolver.exchange(query);
            throw new AssertionError("Unexpected successful query");
        } catch (IOException expected) {
            // No silent platform DNS fallback.
        }
    }

    public static void main(String[] args) throws Exception {
        UltDoqEndpoint defaultEndpoint = UltDoqEndpoint.parse("quic://dns.example.org");
        check(defaultEndpoint.port() == 853, "RFC 9250 default port");
        check("quic://dns.example.org:853".equals(defaultEndpoint.asUri()), "canonical URI");
        UltDoqEndpoint custom = UltDoqEndpoint.parse("quic://DNS.Example.org:8853");
        check(custom.port() == 8853, "custom DoQ port");
        check("dns.example.org".equals(custom.hostname()), "lowercase host");
        for (String value : new String[] {
                "https://dns.example.org", "quic://user@dns.example.org",
                "quic://dns.example.org/path", "quic://dns.example.org?mode=1",
                "quic://127.0.0.1", "quic://localhost", "quic://dns.example.org:70000",
                "quic://dns.example.org:0", "quic://dns.example.org:abc",
                "quic://dns..example.org", "quic://-dns.example.org", "quic://dns.example.org/"
        }) {
            mustReject(value);
        }
        mustFail(new UltDoqResolver(defaultEndpoint, null), QUERY);

        final int[] calls = {0};
        UltDoqResolver transportError = new UltDoqResolver(defaultEndpoint, (server, query) -> {
            calls[0]++;
            throw new IOException("QUIC unreachable");
        });
        mustFail(transportError, QUERY);
        check(calls[0] == 1, "no alternate transport retries");

        byte[] invalidId = Arrays.copyOf(QUERY, QUERY.length);
        invalidId[1] = 1;
        mustFail(transportError, invalidId);
        check(calls[0] == 1, "reject nonzero DNS ID before transport");
        byte[] invalidQr = Arrays.copyOf(QUERY, QUERY.length);
        invalidQr[2] = (byte) 0x81;
        mustFail(transportError, invalidQr);
        check(calls[0] == 1, "reject DNS response sent as query");

        UltDoqResolver echo = new UltDoqResolver(custom, (server, query) -> {
            check(server.port() == 8853, "passed correct endpoint to QUIC backend");
            byte[] response = Arrays.copyOf(query, query.length);
            response[2] = (byte) 0x81;
            response[3] = (byte) 0x80;
            return response;
        });
        byte[] resolved = echo.exchange(QUERY);
        check(resolved[0] == 0 && resolved[1] == 0, "zero DNS response ID");
        check((resolved[2] & 0x80) != 0, "response flag");
        System.out.println("UltDoqContractTest PASS: endpoint, DNS wire, and no-fallback contract");
    }
}

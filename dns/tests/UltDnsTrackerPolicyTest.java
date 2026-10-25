import com.yooshastan.ultmessenger.dns.UltDnsTrackerPolicy;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;

public final class UltDnsTrackerPolicyTest {
    private static final byte[] QUERY = {
            0, 0, 1, 0, 0, 1, 0, 0, 0, 0, 0, 0,
            7, 't','r','a','c','k','e','r', 7, 'e','x','a','m','p','l','e', 3, 'o','r','g', 0,
            0, 1, 0, 1
    };

    private static void check(boolean condition, String reason) {
        if (!condition) throw new AssertionError(reason);
    }

    public static void main(String[] args) throws Exception {
        UltDnsTrackerPolicy policy = new UltDnsTrackerPolicy(
                new HashSet<>(Arrays.asList("example.org", "metrics.example.net")),
                new HashSet<>(Arrays.asList("safe.example.org")));
        check(policy.blocks("TRACKER.EXAMPLE.ORG."), "case and trailing dot");
        check(policy.blocks("a.b.metrics.example.net"), "suffix match");
        check(!policy.blocks("safe.example.org"), "allowlist wins");
        check(!policy.blocks("a.safe.example.org"), "subdomain exception");
        check(!policy.blocks("notexample.org"), "label boundary");
        check(policy.blocksQuestion(QUERY), "read DNS wire question");
        byte[] response = policy.nxdomainResponse(QUERY);
        check(response.length == QUERY.length, "retains question");
        check(response[0] == 0 && response[1] == 0, "DoQ ID remains zero");
        check((response[2] & 0x80) != 0 && (response[3] & 0x0f) == 3,
                "blocked query is NXDOMAIN");
        byte[] invalid = Arrays.copyOf(QUERY, QUERY.length);
        invalid[12] = (byte) 0xc0;
        try {
            policy.blocksQuestion(invalid);
            throw new AssertionError("Compressed pointer unexpectedly accepted");
        } catch (IOException expected) {
            // Correct: malformed DNS messages cannot bypass parser policy.
        }
        try {
            UltDnsTrackerPolicy.normalizeDomain("test../fake");
            throw new AssertionError("Invalid blocklist rule accepted");
        } catch (IllegalArgumentException expected) {
            // Correct: no URL or path-shaped rule.
        }
        System.out.println("UltDnsTrackerPolicyTest PASS");
    }
}

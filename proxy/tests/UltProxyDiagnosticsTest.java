import com.yooshastan.ultmessenger.proxy.UltProxyDiagnostics;

public final class UltProxyDiagnosticsTest {
    private static void expect(String actual, String expected) {
        if (!expected.equals(actual)) {
            throw new AssertionError("Expected " + expected + ", got " + actual);
        }
    }

    public static void main(String[] args) {
        expect(UltProxyDiagnostics.routeStatus(false, true, true, false),
                "Disabled (direct connections may be used)");
        expect(UltProxyDiagnostics.routeStatus(true, false, true, false),
                "Saved but not selected");
        expect(UltProxyDiagnostics.routeStatus(true, true, true, false),
                "Selected; Telegram session connected (routing not proven)");
        expect(UltProxyDiagnostics.routeStatus(true, true, false, true),
                "Selected; Telegram session updating (routing not proven)");
        expect(UltProxyDiagnostics.routeStatus(true, true, false, false),
                "Selected; Telegram session not connected");
        expect(UltProxyDiagnostics.nativeProbeStatus(true, 0, false, 0),
                "Native Telegram reachability check running");
        expect(UltProxyDiagnostics.nativeProbeStatus(false, 0, false, 0),
                "Not yet checked");
        expect(UltProxyDiagnostics.nativeProbeStatus(false, 100, false, 0),
                "Last native Telegram check failed");
        expect(UltProxyDiagnostics.nativeProbeStatus(false, 100, true, 234),
                "Reachable in last native Telegram check (234 ms)");
        expect(UltProxyDiagnostics.nativeProbeStatus(false, 100, true, 0),
                "Reachable in last native Telegram check (latency unavailable)");
        String warning = UltProxyDiagnostics.dnsAuditWarning();
        if (!warning.contains("NOT implemented") || !warning.contains("not proof")) {
            throw new AssertionError("Diagnostics must not promise DNS leak prevention");
        }
        String snapshot = UltProxyDiagnostics.redactedAuditSnapshot(
                "MTPROTO", true, true, true, false, false, 100, true, 69);
        if (!snapshot.contains("local status snapshot")
                || !snapshot.contains("Selected; Telegram session connected")
                || !snapshot.contains("69 ms")
                || !snapshot.contains("NOT implemented")
                || !snapshot.contains("clipboard only on request")) {
            throw new AssertionError("Redacted report must state scope and native status");
        }
        String untrustedTransport = UltProxyDiagnostics.redactedAuditSnapshot(
                "secret@10.0.0.169", true, false, false, false, false, 0, false, 0);
        if (untrustedTransport.contains("secret@") || untrustedTransport.contains("10.0.0.169")
                || !untrustedTransport.contains("Other/unknown")) {
            throw new AssertionError("Redacted report must allowlist transport fields");
        }
        System.out.println("ULT proxy diagnostics passed");
    }
}

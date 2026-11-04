import com.yooshastan.ultmessenger.settings.UltNetworkTestReport;

public final class UltNetworkTestReportTest {
    private static void ok(boolean yes, String label) {
        if (!yes) throw new AssertionError(label);
    }

    public static void main(String[] args) {
        boolean[] checks = new boolean[UltNetworkTestReport.CHECKS.length];
        checks[0] = true;
        checks[2] = true;
        String report = UltNetworkTestReport.render(
                "VPN transport reported; Ultgram routing NOT verified", true, true, 35, checks);
        ok(report.contains("USER-REPORTED"), "not a network verdict");
        ok(report.contains("[marked] Messages"), "manual marks");
        ok(report.contains("[not marked] Channels"), "unmarked state");
        ok(report.contains("route NOT verified"), "no tunnel inference");
        ok(report.contains("FAIL-CLOSED GUARANTEE"), "no false claims");
        ok(report.contains("requested: ON"), "requested native guard shown");
        ok(report.contains("observed (selected account): ON"), "applied native guard shown");
        String transitional = UltNetworkTestReport.render(
                "Unknown", false, true, 35, checks);
        ok(transitional.contains("STATE MISMATCH"), "stale native state is visible");
        ok(!report.contains("10.77.0.2"), "no addresses");
        String other = UltNetworkTestReport.render(
                "proxySecret=do-not-print", false, false, 35, checks);
        ok(!other.contains("proxySecret"), "untrusted status redacted");
        ok(!UltNetworkTestReport.key(0).equals(UltNetworkTestReport.key(1)),
                "separate keys");
        boolean rejected = false;
        try {
            UltNetworkTestReport.render(null, false, false, 1, new boolean[1]);
        } catch (IllegalArgumentException expected) {
            rejected = true;
        }
        ok(rejected, "checklist length");
        System.out.println("ULT network test report passed");
    }
}

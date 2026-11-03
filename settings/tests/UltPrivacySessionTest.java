import com.yooshastan.ultmessenger.settings.UltPrivacySession;

public final class UltPrivacySessionTest {
    private static void check(boolean ok, String msg) {
        if (!ok) throw new AssertionError(msg);
    }
    public static void main(String[] args) {
        UltPrivacySession session = new UltPrivacySession();
        check(session.observe(10000, "Wi-Fi", true, true, 3), "initial event");
        check(!session.observe(12000, "Wi-Fi", true, true, 3), "no repeated event");
        check(session.transitions() == 0, "initial is not a transition");
        check(session.observe(15000, "Cellular", true, true, 2), "network transition");
        check(session.transitions() == 1, "one transition");
        String report = session.redactedReport();
        check(report.contains("Cellular"), "network in report");
        check(report.contains("NOT packet-route"), "route disclaimer");
        check(!report.contains("10.77."), "no address included");
        for (int i = 0; i < 60; i++) session.observe(20000 + i, "VPN", i % 2 == 0, true, i);
        check(session.events().size() == 40, "bounded history");
        session.clear();
        check(session.events().isEmpty() && session.transitions() == 0, "clear");
        check(session.observe(30000, "my-secret-host", false, false, 0), "new initial");
        check(session.redactedReport().contains("Unknown"), "untrusted network label sanitized");
        check(!session.redactedReport().contains("my-secret-host"), "unknown not recorded");
        System.out.println("UltPrivacySessionTest OK");
    }
}

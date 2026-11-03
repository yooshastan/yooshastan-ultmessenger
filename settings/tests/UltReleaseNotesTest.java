import com.yooshastan.ultmessenger.settings.UltReleaseNotes;

public final class UltReleaseNotesTest {
    static void ok(boolean good, String msg) {
        if (!good) throw new AssertionError(msg);
    }
    public static void main(String[] args) {
        String a = UltReleaseNotes.androidPackageVersion("4.0.0", 48);
        ok(a.contains("4.0.0 (code 48)"), "real package version is visible");
        ok(UltReleaseNotes.seenKey(a).equals(UltReleaseNotes.seenKey(a)), "stable key");
        ok(!UltReleaseNotes.seenKey(a).equals(UltReleaseNotes.seenKey("4.0.1 (code 49)")),
                "upgrade reopens changelog");
        String message = UltReleaseNotes.message(a);
        for (String fragment : new String[] {
                "Downloads", "More", "strict", "translation", "Network diagnostics",
                "DoQ", "tracker", "debug-signed", "Protected media", "4.0.0"
        }) {
            ok(message.contains(fragment), "missing concise release note: " + fragment);
        }
        ok(!message.contains("Media Studio:"), "retired duplicate screen is not advertised");
        ok(!message.contains("0.13c+d"), "legacy changelog details are not repeated");
        System.out.println("Ultgram release notes policy passed");
    }
}

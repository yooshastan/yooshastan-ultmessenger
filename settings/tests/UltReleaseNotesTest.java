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
        for (String fragment : new String[] {"Save As", "Downloads", "4.0.0",
                "Debug-signed", "protected media", "0.13c+d", "0.14a+b", "0.14c", "0.14d+", "Stabilization", "cache", "Gallery", "SHA-256", "Preview colors", "Verify saved copy", "bubble corners"}) {
            ok(message.contains(fragment), "missing changelog detail: " + fragment);
        }
        System.out.println("Ultgram release notes policy passed");
    }
}

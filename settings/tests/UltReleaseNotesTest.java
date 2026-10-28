import com.yooshastan.ultmessenger.settings.UltReleaseNotes;
public final class UltReleaseNotesTest {
  public static void main(String[] args) {
    String text=UltReleaseNotes.summary();
    if (text == null || text.length()<150 || !text.contains("NOT INCLUDED")
        || !text.contains("SHORTCUT ONLY") || !text.contains("Downloads")
        || !text.contains("No identity masking") || UltReleaseNotes.SEEN_KEY.isEmpty()) {
      throw new AssertionError("Changelog must be discoverable and accurately scoped");
    }
    System.out.println("Release notes policy passed");
  }
}

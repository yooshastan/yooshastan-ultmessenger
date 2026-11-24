import com.yooshastan.ultmessenger.video.UltVideoFailure;
public final class UltVideoFailureTest {
  static void check(String got,String want){if (!want.equals(got))throw new AssertionError("Expected "+want+" got "+got);}
  public static void main(String[] args) {
    check(UltVideoFailure.publicReason(new RuntimeException("https://some-video.site/secret?token=123"), "download"),
       "Extractor failed (unclassified); no private URL logged");
    check(UltVideoFailure.publicReason(new RuntimeException("ERROR: Sign in to confirm you are not a bot"), "download"),
       "Site requires login; no cookies or account credentials are used");
    check(UltVideoFailure.publicReason(new RuntimeException("ERROR: Requested format is not available"),"download"),
       "No usable video/audio format on this site");
    check(UltVideoFailure.publicReason(new RuntimeException("anything"),"initialization"),
       "Embedded downloader initialization failed (runtime or Python package)");
    check(UltVideoFailure.publicReason(new RuntimeException("HTTP Error 429"),"download"),
       "Video site rate-limited the download");
    check(UltVideoFailure.publicReason(new RuntimeException("HTTP Error 403: Forbidden https://example.com/private?token=secret"),"download"),
       "Site denied access (403 or forbidden); public extraction may be unavailable");
    System.out.println("UltVideoFailureTest OK");
  }
}

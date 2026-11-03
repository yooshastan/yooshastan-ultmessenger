import java.util.Arrays;
import java.util.List;
import com.yooshastan.ultmessenger.settings.UltMediaIndex;

public final class UltMediaIndexTest {
    private static void check(boolean ok, String label) {
        if (!ok) throw new AssertionError(label);
    }
    public static void main(String[] args) {
        UltMediaIndex.Entry a = new UltMediaIndex.Entry("Song.FLAC", "audio/flac", 500, 123);
        UltMediaIndex.Entry b = new UltMediaIndex.Entry("Film.mp4", "video/mp4", 900, 100);
        UltMediaIndex.Entry c = new UltMediaIndex.Entry("Photo.jpg", null, 200, 300);
        UltMediaIndex.Entry d = new UltMediaIndex.Entry("report.PDF", "application/pdf", 700, 200);
        List<UltMediaIndex.Entry> input = Arrays.asList(a, b, c, d);
        check(a.kind == UltMediaIndex.Kind.AUDIO, "FLAC");
        check(b.kind == UltMediaIndex.Kind.VIDEO, "video");
        check(c.kind == UltMediaIndex.Kind.PHOTO, "photo extension");
        check(d.kind == UltMediaIndex.Kind.DOCUMENT, "document");
        check(UltMediaIndex.filter(input, null, "", UltMediaIndex.Order.NEWEST).get(0) == c, "newest");
        check(UltMediaIndex.filter(input, null, "", UltMediaIndex.Order.LARGEST).get(0) == b, "largest");
        check(UltMediaIndex.filter(input, null, "", UltMediaIndex.Order.NAME).get(0) == b, "name");
        check(UltMediaIndex.filter(input, UltMediaIndex.Kind.AUDIO, "SONG", null).size() == 1,
                "case insensitive filter");
        check(UltMediaIndex.filter(input, UltMediaIndex.Kind.DOCUMENT, "song", null).isEmpty(),
                "kind and query intersect");
        check(UltMediaIndex.filter(null, null, "", null).isEmpty(), "empty input");
        System.out.println("UltMediaIndexTest OK");
    }
}

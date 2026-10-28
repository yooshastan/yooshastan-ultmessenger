import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import com.yooshastan.ultmessenger.settings.UltFileInspection;

public final class UltFileInspectionTest {
    private static void check(boolean ok, String label) {
        if (!ok) throw new AssertionError(label);
    }
    public static void main(String[] args) throws Exception {
        UltFileInspection.Result r = UltFileInspection.digest(new ByteArrayInputStream(
                "abc".getBytes(StandardCharsets.UTF_8)));
        check(r.bytesRead == 3, "size");
        check("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad"
                .equals(r.sha256), "sha256 abc");
        String details = UltFileInspection.describe("song.flac", "audio/flac", r);
        check(details.contains("song.flac") && details.contains("3 bytes"), "metadata");
        check(details.contains("This does not verify"), "no false verification");
        check(UltFileInspection.describe("bad\nname", "../invalid", r)
                .contains("MIME: unknown"), "safe MIME");
        try {
            UltFileInspection.digest(null);
            throw new AssertionError("null input must fail");
        } catch (IllegalArgumentException expected) { }
        try {
            UltFileInspection.digest(new InputStream() {
                long left = UltFileInspection.MAX_INSPECT_BYTES + 1;
                @Override public int read() { if (left-- <= 0) return -1; return 0; }
                @Override public int read(byte[] bytes, int off, int len) {
                    if (left <= 0) return -1;
                    int n = (int)Math.min(left, len);
                    java.util.Arrays.fill(bytes, off, off + n, (byte)0);
                    left -= n;
                    return n;
                }
            });
            throw new AssertionError("size limit must apply");
        } catch (IOException expected) { }
        System.out.println("ULT local media inspector checks passed");
    }
}

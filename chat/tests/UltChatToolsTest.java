import com.yooshastan.ultmessenger.chat.UltChatTools;

public final class UltChatToolsTest {
    public static void main(String[] args) {
        if (!UltChatTools.numericId(-100123456789L).equals("-100123456789")) {
            throw new AssertionError("Signed Telegram dialog IDs must be preserved");
        }
        if (!"https://t.me/stage_fan".equals(UltChatTools.publicLink("@Stage_Fan"))) {
            throw new AssertionError("Public link normalization failed");
        }
        if (UltChatTools.publicLink(null) != null || UltChatTools.publicLink("") != null
                || UltChatTools.publicLink("https://evil.example/path") != null
                || UltChatTools.publicLink("evil/name") != null) {
            throw new AssertionError("Malformed usernames should not create links");
        }
        if (!"https://t.me/stage_fan/54321".equals(
                UltChatTools.publicMessageLink("@Stage_Fan", 54321))) {
            throw new AssertionError("Public message link formatting failed");
        }
        if (UltChatTools.publicMessageLink(null, 54321) != null
                || UltChatTools.publicMessageLink("private/path", 1) != null
                || UltChatTools.publicMessageLink("ValidName", 0) != null
                || UltChatTools.publicMessageLink("@ValidName", -1) != null) {
            throw new AssertionError("Private, invalid, or missing message links are forbidden");
        }
        if (!"1970-01-01T00:00:01Z".equals(UltChatTools.utcTimestamp(1))
                || UltChatTools.utcTimestamp(0) != null
                || UltChatTools.utcTimestamp(-1) != null) {
            throw new AssertionError("UTC timestamp conversion must be deterministic");
        }
        if (!UltChatTools.hasTopic(42) || UltChatTools.hasTopic(0) || UltChatTools.hasTopic(-1)) {
            throw new AssertionError("Topic detection failed");
        }
        if (!"12345".equals(UltChatTools.unixTimestamp(12345))
                || UltChatTools.unixTimestamp(0) != null) {
            throw new AssertionError("Message time should be a raw positive Unix timestamp");
        }
        if (!"MIME: audio/flac\nBytes: 4096".equals(UltChatTools.mediaDetails("audio/flac", 4096))) {
            throw new AssertionError("Document metadata should be deterministic");
        }
        if (!"MIME: unknown\nBytes: 0".equals(UltChatTools.mediaDetails("../bad", -8))) {
            throw new AssertionError("Document metadata must not expose malformed MIME data");
        }
        System.out.println("ULT chat tools model passed");
    }
}

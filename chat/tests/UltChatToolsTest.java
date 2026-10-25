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

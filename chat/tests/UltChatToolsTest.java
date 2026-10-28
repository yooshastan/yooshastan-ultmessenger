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
        if (!"File: demo.flac\nSize: 2.0 MiB\nMIME: audio/flac\nBytes: 2097152"
                .equals(UltChatTools.mediaSummary("demo.flac", "audio/flac", 2097152))) {
            throw new AssertionError("File summary must expose readable and exact size");
        }
        if (!UltChatTools.mediaSummary("../secret\nthing", null, -3)
                .startsWith("File: .._secret_thing\nSize: 0 B\nMIME: unknown\nBytes: 0")) {
            throw new AssertionError("File summary must sanitize names and sizes");
        }
        if (!"Message ID: 42\nSender peer ID: -1003\nUTC: 1970-01-01T00:00:01Z\nPublic link: https://t.me/stage_fan/42"
                .equals(UltChatTools.messageReference(42, -1003, 1, "@Stage_Fan"))) {
            throw new AssertionError("Public message reference summary failed");
        }
        if (!"Message ID: 42".equals(UltChatTools.messageReference(42, 0, 0, null))
                || UltChatTools.messageReference(0, 12, 2, "@valid") != null
                || UltChatTools.messageReference(-1, 12, 2, "@valid") != null
                || UltChatTools.messageReference(3, 0, 0, "bad/link").contains("https://")) {
            throw new AssertionError("Private or invalid references must exclude public links");
        }
        System.out.println("ULT chat tools model passed");
    }
}

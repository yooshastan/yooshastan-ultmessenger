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
        System.out.println("ULT chat tools model passed");
    }
}

import com.yooshastan.ultmessenger.chat.UltChatLayoutPreset;

public final class UltChatLayoutPresetTest {
    public static void main(String[] args) {
        if (UltChatLayoutPreset.fromStoredValue(null) != UltChatLayoutPreset.TELEGRAM) {
            throw new AssertionError("Null preferences must retain Telegram default");
        }
        if (UltChatLayoutPreset.fromStoredValue("invalid") != UltChatLayoutPreset.TELEGRAM) {
            throw new AssertionError("Unknown preferences must retain Telegram default");
        }
        if (UltChatLayoutPreset.fromStoredValue("compact") != UltChatLayoutPreset.COMPACT) {
            throw new AssertionError("Saved preferences are case-insensitive");
        }
        for (UltChatLayoutPreset preset : UltChatLayoutPreset.values()) {
            if (!preset.keepAccessibilityLabels || preset.verticalPaddingDeltaDp < -4) {
                throw new AssertionError("Accessibility regression: " + preset.name());
            }
        }
        System.out.println("ULT chat layout preset tests passed");
    }
}

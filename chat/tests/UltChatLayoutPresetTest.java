import com.yooshastan.ultmessenger.chat.UltChatLayoutPreset;

public final class UltChatLayoutPresetTest {
    public static void main(String[] args) {
        if (UltChatLayoutPreset.fromStoredValue(null) != UltChatLayoutPreset.TELEGRAM
                || UltChatLayoutPreset.fromStoredValue("unknown") != UltChatLayoutPreset.TELEGRAM
                || UltChatLayoutPreset.fromStoredValue("compact") != UltChatLayoutPreset.COMPACT) {
            throw new AssertionError("Safe preset loading failed");
        }
        if (UltChatLayoutPreset.COMPACT.targetFontSize(16) != 14
                || UltChatLayoutPreset.COMPACT.targetBubbleRadius(12) != 5) {
            throw new AssertionError("Compact preset incorrect");
        }
        if (UltChatLayoutPreset.COMFORTABLE.targetFontSize(16) != 18
                || UltChatLayoutPreset.COMFORTABLE.targetBubbleRadius(12) != 17) {
            throw new AssertionError("Comfortable preset incorrect");
        }
        if (UltChatLayoutPreset.MINIMAL.targetBubbleRadius(16) != 0) {
            throw new AssertionError("Minimal bubbles must be square");
        }
        if (UltChatLayoutPreset.TELEGRAM.targetFontSize(19) != 19
                || UltChatLayoutPreset.TELEGRAM.targetBubbleRadius(12) != 12) {
            throw new AssertionError("Restore preset must preserve originals");
        }
        if (UltChatLayoutPreset.COMPACT.targetFontSize(12) != 12
                || UltChatLayoutPreset.COMFORTABLE.targetFontSize(29) != 30) {
            throw new AssertionError("Accessible native font bounds not enforced");
        }
        for (UltChatLayoutPreset preset : UltChatLayoutPreset.values()) {
            if (!preset.keepAccessibilityLabels) {
                throw new AssertionError("Accessibility labels must stay enabled");
            }
        }
        System.out.println("ULT native chat layout model passed");
    }
}

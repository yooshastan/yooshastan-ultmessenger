import com.yooshastan.ultmessenger.appearance.UltAppearancePreset;
import com.yooshastan.ultmessenger.appearance.UltCustomTheme;

public final class UltCustomThemeTest {
    private static void ok(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
    private static void invalid(String accent, String bubble) {
        boolean rejected = false;
        try {
            UltCustomTheme.edit(UltAppearancePreset.MIDNIGHT, accent, bubble);
        } catch (IllegalArgumentException expected) {
            rejected = true;
        }
        ok(rejected, "Unsafe colors must be rejected");
    }
    public static void main(String[] args) {
        ok("#E86BCB".equals(UltCustomTheme.toHex(0xFFE86BCB)), "format");
        ok(UltCustomTheme.parseRgb("#aBc123") == 0xFFABC123, "parse");
        UltAppearancePreset.Palette original =
                UltAppearancePreset.MIDNIGHT.forMode(true);
        UltAppearancePreset.Palette custom =
                UltCustomTheme.edit(UltAppearancePreset.MIDNIGHT, "#E86BCB", "#422C50");
        ok(custom.dark && custom.accentArgb == 0xFFE86BCB
                && custom.outgoingBubbleArgb == 0xFF422C50, "custom colors");
        ok(custom.textArgb == original.textArgb
                && custom.incomingBubbleArgb == original.incomingBubbleArgb,
                "preserve the original background and incoming text");
        invalid("#101010", "#422C50");
        invalid("#E86BCB", "#FFFFFF");
        invalid("E86BCB", "#422C50");
        invalid("#E86B", "#422C50");
        boolean rejected = false;
        try { UltCustomTheme.edit(UltAppearancePreset.SYSTEM, "#FFFFFF", "#000000"); }
        catch (IllegalArgumentException expected) { rejected = true; }
        ok(rejected, "system theme cannot be overwritten");
        System.out.println("ULT fixed custom theme policy passed");
    }
}

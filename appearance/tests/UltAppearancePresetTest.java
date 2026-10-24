import com.yooshastan.ultmessenger.appearance.UltAppearancePreset;

public final class UltAppearancePresetTest {
    public static void main(String[] args) {
        require(UltAppearancePreset.fromStoredValue(null) == UltAppearancePreset.SYSTEM,
                "null should restore System");
        require(UltAppearancePreset.fromStoredValue("midnight") == UltAppearancePreset.MIDNIGHT,
                "stored Alpha 0.2 preset must remain compatible");
        require(UltAppearancePreset.fromStoredValue("aurora") == UltAppearancePreset.AURORA,
                "Aurora must persist by enum name");
        require(UltAppearancePreset.fromStoredValue("invalid") == UltAppearancePreset.SYSTEM,
                "invalid preset must fail safe");
        for (UltAppearancePreset preset : UltAppearancePreset.values()) {
            require(!preset.displayName.isEmpty(), "missing visible label");
            require((preset.backgroundArgb >>> 24) == 0xFF, "background must be opaque");
            require((preset.incomingBubbleArgb >>> 24) == 0xFF, "incoming bubble must be opaque");
            require((preset.outgoingBubbleArgb >>> 24) == 0xFF, "outgoing bubble must be opaque");
        }
        require(UltAppearancePreset.AURORA.dark && UltAppearancePreset.OBSIDIAN.dark,
                "new high-contrast palettes must use native Night base");
        System.out.println("ULT visual palette checks passed");
    }

    private static void require(boolean condition, String description) {
        if (!condition) {
            throw new AssertionError(description);
        }
    }
}

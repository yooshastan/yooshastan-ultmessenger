import com.yooshastan.ultmessenger.appearance.UltAppearancePreset;

public final class UltAppearancePresetTest {
    public static void main(String[] args) {
        if (UltAppearancePreset.values().length != 5) throw new AssertionError();
        if (UltAppearancePreset.fromStoredValue("midnight") != UltAppearancePreset.MIDNIGHT)
            throw new AssertionError("case insensitive storage decode failed");
        if (UltAppearancePreset.fromStoredValue("unknown") != UltAppearancePreset.SYSTEM)
            throw new AssertionError("invalid persisted value should fall back");
        if (!UltAppearancePreset.MIDNIGHT.dark) throw new AssertionError();
        if (UltAppearancePreset.CANDY.dark) throw new AssertionError();
        System.out.println("ULT appearance preset tests passed");
    }
}

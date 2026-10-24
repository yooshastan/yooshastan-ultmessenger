#!/usr/bin/env python3
"""Inject the ULT preset chooser into pinned Telegram/Mercurygram Chat Settings.

This integration is intentionally fail-closed: updated upstream menu anchors
must be reviewed instead of silently creating broken source.
"""
import argparse
from pathlib import Path

TARGET = Path("TMessagesProj/src/main/java/org/telegram/ui/ThemeActivity.java")
IMPORT_ANCHOR = "import org.telegram.ui.ActionBar.Theme;\n"
CLASS_ANCHOR = "public class ThemeActivity extends BaseFragment implements NotificationCenter.NotificationCenterDelegate {\n"
MENU_ANCHOR = '            menuItem.addSubItem(reset_settings, R.drawable.msg_reset, getString("ThemeResetToDefaults", R.string.ThemeResetToDefaults));\n'
CLICK_ANCHOR = "                } else if (id == create_theme) {\n"
IMPORT = "import com.yooshastan.ultmessenger.appearance.UltAppearancePreset;\n"
FIELDS = """
    private static final int ULT_PRESET_MENU_ID = 0x554C54;
    private static final String ULT_PRESET_PREFERENCES = "ult_appearance";
    private static final String ULT_PRESET_KEY = "selected_preset";

    private void showUltPresetChooser() {
        final android.app.Activity activity = getParentActivity();
        if (activity == null) {
            return;
        }
        final UltAppearancePreset[] presets = UltAppearancePreset.values();
        final String[] labels = new String[presets.length];
        for (int i = 0; i < presets.length; i++) {
            labels[i] = presets[i].displayName;
        }
        String stored = activity.getSharedPreferences(ULT_PRESET_PREFERENCES, Context.MODE_PRIVATE)
                .getString(ULT_PRESET_KEY, UltAppearancePreset.SYSTEM.name());
        UltAppearancePreset current = UltAppearancePreset.fromStoredValue(stored);
        new android.app.AlertDialog.Builder(activity)
                .setTitle("ULT Appearance — presets")
                .setSingleChoiceItems(labels, current.ordinal(), (dialog, selected) -> {
                    activity.getSharedPreferences(ULT_PRESET_PREFERENCES, Context.MODE_PRIVATE)
                            .edit().putString(ULT_PRESET_KEY, presets[selected].name()).apply();
                    dialog.dismiss();
                    new android.app.AlertDialog.Builder(activity)
                            .setMessage("Saved " + presets[selected].displayName
                                    + ". Theme-engine application is coming in the next build.")
                            .setPositiveButton(android.R.string.ok, null)
                            .show();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }
"""
def replace_once(source: str, anchor: str, replacement: str) -> str:
    count = source.count(anchor)
    if count != 1:
        raise ValueError(f"Expected one upstream anchor, found {count}: {anchor[:70]!r}")
    return source.replace(anchor, replacement, 1)

def integrate(root: Path) -> Path:
    target = root / TARGET
    source = target.read_text(encoding="utf-8")
    if "ULT_PRESET_MENU_ID" in source:
        raise ValueError("ULT appearance integration already applied")
    source = replace_once(source, IMPORT_ANCHOR, IMPORT_ANCHOR + IMPORT)
    source = replace_once(source, CLASS_ANCHOR, CLASS_ANCHOR + FIELDS)
    source = replace_once(source, MENU_ANCHOR, MENU_ANCHOR +
        '            menuItem.addSubItem(ULT_PRESET_MENU_ID, R.drawable.msg_palette, "ULT Presets");\n')
    source = replace_once(source, CLICK_ANCHOR,
        "                } else if (id == ULT_PRESET_MENU_ID) {\n"
        "                    showUltPresetChooser();\n" + CLICK_ANCHOR)
    target.write_text(source, encoding="utf-8")
    model_src = Path(__file__).resolve().parents[1] / "appearance/src/com/yooshastan/ultmessenger/appearance/UltAppearancePreset.java"
    model_dst = root / "TMessagesProj/src/main/java/com/yooshastan/ultmessenger/appearance/UltAppearancePreset.java"
    model_dst.parent.mkdir(parents=True, exist_ok=True)
    model_dst.write_bytes(model_src.read_bytes())
    return target

if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("source_root", type=Path)
    args = parser.parse_args()
    print(f"Patched {integrate(args.source_root)}")

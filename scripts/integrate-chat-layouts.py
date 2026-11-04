#!/usr/bin/env python3
"""Inject opt-in ULT chat layout presets into the pinned native Chat Settings.

Applies only through Telegram's existing setFontSize/setBubbleRadius paths.
Fail closed when upstream menu anchors change.
"""
import argparse
from pathlib import Path

TARGET = Path("TMessagesProj/src/main/java/org/telegram/ui/ThemeActivity.java")
MODEL = Path("chat/src/com/yooshastan/ultmessenger/chat/UltChatLayoutPreset.java")
IMPORT_ANCHOR = "import org.telegram.ui.ActionBar.Theme;\n"
CLASS_ANCHOR = "public class ThemeActivity extends BaseFragment implements NotificationCenter.NotificationCenterDelegate {\n"
MENU_ANCHOR = '            menuItem.addSubItem(reset_settings, R.drawable.msg_reset, getString("ThemeResetToDefaults", R.string.ThemeResetToDefaults));\n'
CLICK_ANCHOR = "                } else if (id == create_theme) {\n"

FIELDS = """
    private static final int ULT_CHAT_LAYOUT_MENU_ID = 0x554c43;
    private static final String ULT_CHAT_PREFS = "ult_chat_layout";
    private static final String ULT_CHAT_SELECTED = "selected_preset";
    private static final String ULT_CHAT_ORIGINAL_FONT = "original_font_size";
    private static final String ULT_CHAT_ORIGINAL_RADIUS = "original_bubble_radius";

    private void showUltChatLayoutChooser() {
        final android.app.Activity activity = getParentActivity();
        if (activity == null) {
            return;
        }
        final SharedPreferences preferences = activity.getSharedPreferences(
                ULT_CHAT_PREFS, Context.MODE_PRIVATE);
        final UltChatLayoutPreset[] presets = UltChatLayoutPreset.values();
        final String[] labels = new String[presets.length];
        for (int index = 0; index < presets.length; index++) {
            labels[index] = presets[index].label;
        }
        UltChatLayoutPreset current = UltChatLayoutPreset.fromStoredValue(
                preferences.getString(ULT_CHAT_SELECTED, UltChatLayoutPreset.TELEGRAM.name()));
        new android.app.AlertDialog.Builder(activity)
                .setTitle("ULT Chat Layout")
                .setSingleChoiceItems(labels, current.ordinal(), (dialog, selected) -> {
                    dialog.dismiss();
                    UltChatLayoutPreset preset = presets[selected];
                    if (preset == UltChatLayoutPreset.TELEGRAM) {
                        int originalFont = preferences.getInt(ULT_CHAT_ORIGINAL_FONT, SharedConfig.fontSize);
                        int originalRadius = preferences.getInt(ULT_CHAT_ORIGINAL_RADIUS, SharedConfig.bubbleRadius);
                        setFontSize(originalFont);
                        setBubbleRadius(originalRadius, true);
                        preferences.edit()
                                .putString(ULT_CHAT_SELECTED, preset.name())
                                .remove(ULT_CHAT_ORIGINAL_FONT)
                                .remove(ULT_CHAT_ORIGINAL_RADIUS)
                                .apply();
                    } else {
                        if (!preferences.contains(ULT_CHAT_ORIGINAL_FONT)) {
                            preferences.edit()
                                    .putInt(ULT_CHAT_ORIGINAL_FONT, SharedConfig.fontSize)
                                    .putInt(ULT_CHAT_ORIGINAL_RADIUS, SharedConfig.bubbleRadius)
                                    .apply();
                        }
                        int originalFont = preferences.getInt(ULT_CHAT_ORIGINAL_FONT, SharedConfig.fontSize);
                        int originalRadius = preferences.getInt(ULT_CHAT_ORIGINAL_RADIUS, SharedConfig.bubbleRadius);
                        setFontSize(preset.targetFontSize(originalFont));
                        setBubbleRadius(preset.targetBubbleRadius(originalRadius), true);
                        preferences.edit().putString(ULT_CHAT_SELECTED, preset.name()).apply();
                    }
                    android.widget.Toast.makeText(activity, "ULT layout: " + preset.label,
                            android.widget.Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

"""

def replace_once(source: str, anchor: str, replacement: str) -> str:
    count = source.count(anchor)
    if count != 1:
        raise ValueError(f"Expected one pinned Chat Settings anchor; found {count}: {anchor[:72]!r}")
    return source.replace(anchor, replacement, 1)

def integrate(root: Path) -> Path:
    target = root / TARGET
    source = target.read_text(encoding="utf-8")
    if "ULT_CHAT_LAYOUT_MENU_ID" in source:
        raise ValueError("ULT chat layout integration was already applied")
    # This script runs AFTER integrate-appearance.py, preserving both menus.
    source = replace_once(source, IMPORT_ANCHOR,
                          IMPORT_ANCHOR + "import com.yooshastan.ultmessenger.chat.UltChatLayoutPreset;\n")
    source = replace_once(source, CLASS_ANCHOR, CLASS_ANCHOR + FIELDS)
    source = replace_once(source, MENU_ANCHOR, MENU_ANCHOR +
                          '            menuItem.addSubItem(ULT_CHAT_LAYOUT_MENU_ID, R.drawable.msg_palette, "ULT Chat Layout");\n')
    source = replace_once(source, CLICK_ANCHOR,
                          "                } else if (id == ULT_CHAT_LAYOUT_MENU_ID) {\n"
                          "                    showUltChatLayoutChooser();\n"
                          + CLICK_ANCHOR)
    # Ensure the pinned native adapter methods remain available and unchanged.
    if source.count("private boolean setFontSize(int size)") != 1:
        raise ValueError("Pinned setFontSize method changed")
    if source.count("private boolean setBubbleRadius(int size, boolean layout)") != 1:
        raise ValueError("Pinned setBubbleRadius method changed")
    model_src = Path(__file__).resolve().parents[1] / MODEL
    if not model_src.is_file():
        raise FileNotFoundError(model_src)
    model_data = model_src.read_bytes()
    target.write_text(source, encoding="utf-8")
    model_dst = root / "TMessagesProj/src/main/java/com/yooshastan/ultmessenger/chat/UltChatLayoutPreset.java"
    model_dst.parent.mkdir(parents=True, exist_ok=True)
    model_dst.write_bytes(model_data)
    return target

if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("source_root", type=Path)
    args = parser.parse_args()
    print(f"Patched {integrate(args.source_root)}")

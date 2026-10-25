#!/usr/bin/env python3
"""Add a standalone Ultgram Settings entry beside Mercurygram Settings.

This is navigation consolidation only: it does not overwrite Mercurygram,
change Telegram preferences, or activate unfinished privacy protections.
"""
import argparse
from pathlib import Path

TARGET = Path("TMessagesProj/src/main/java/org/telegram/ui/SettingsActivity.java")
MODEL = Path("settings/src/com/yooshastan/ultmessenger/settings/UltSettingsActivity.java")
DEST = Path("TMessagesProj/src/main/java/com/yooshastan/ultmessenger/settings/UltSettingsActivity.java")
ROW_ANCHOR = (
    '        items.add(SettingCell.Factory.of(100, 0xFF8F77FF, 0xFF6E5BD9, '
    'R.drawable.mg_settings, getString(R.string.MercurygramSettings), '
    'getString(R.string.MercurygramSettingsInfo)));\n'
)
CLICK_ANCHOR = (
    '            case 100:\n'
    '                presentFragment(new it.belloworld.mercurygram.ui.MercurygramSettingsActivity());\n'
    '                break;\n'
)
ULT_ROW = (
    '        items.add(SettingCell.Factory.of(101, 0xFFB471E8, 0xFF7955CC, '
    'R.drawable.settings_chat, "Ultgram Settings", '
    '"Themes, chats, tools, proxy and privacy"));\n'
)
ULT_CLICK = (
    '            case 101:\n'
    '                presentFragment(new com.yooshastan.ultmessenger.settings.UltSettingsActivity());\n'
    '                break;\n'
)


def once(source: str, anchor: str, replacement: str) -> str:
    count = source.count(anchor)
    if count != 1:
        raise ValueError(f"Expected exactly one pinned settings anchor, found {count}: {anchor[:88]!r}")
    return source.replace(anchor, replacement, 1)


def integrate(root: Path) -> Path:
    target = root / TARGET
    source = target.read_text(encoding="utf-8")
    if "new com.yooshastan.ultmessenger.settings.UltSettingsActivity()" in source:
        raise ValueError("ULT settings already integrated")
    updated = once(source, ROW_ANCHOR, ULT_ROW + ROW_ANCHOR)
    updated = once(updated, CLICK_ANCHOR, ULT_CLICK + CLICK_ANCHOR)
    model_data = (Path(__file__).resolve().parents[1] / MODEL).read_bytes()
    # Refuse changed upstream or missing model before touching generated source.
    target.write_text(updated, encoding="utf-8")
    dest = root / DEST
    dest.parent.mkdir(parents=True, exist_ok=True)
    dest.write_bytes(model_data)
    return target


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("source_root", type=Path)
    args = parser.parse_args()
    print(f"Added Ultgram Settings: {integrate(args.source_root)}")

#!/usr/bin/env python3
"""Opt-in, conservative outgoing URL sanitation at the text-send boundary.

Runs AFTER integrate-ult-settings.py copied the policy/Android preference models.
No link expansion or external network requests; rich spans remain untouched.
"""
import argparse
from pathlib import Path

CHAT = Path("TMessagesProj/src/main/java/org/telegram/ui/Components/ChatActivityEnterView.java")
METHOD = ("    public boolean processSendingText(CharSequence text, boolean notify, "
          "int scheduleDate, int scheduleRepeatPeriod, long payStars) {\n")
SEND = ("messageWebPage, isMessageWebPageSearchEnabled(), entities, null, null, "
        "notify, scheduleDate, scheduleRepeatPeriod, sendAnimationData, updateStickersOrder)")
INTRO = """        CharSequence ultSanitizedText =
                com.yooshastan.ultmessenger.settings.UltLinkSanitizer.sanitizeOutgoing(
                        ApplicationLoader.applicationContext, currentAccount, text);
        final boolean ultLinksChanged = ultSanitizedText != null
                && !ultSanitizedText.toString().contentEquals(text);
        if (ultLinksChanged) {
            text = ultSanitizedText;
        }
"""
NEW_SEND = ("(ultLinksChanged ? null : messageWebPage), "
            "(!ultLinksChanged && isMessageWebPageSearchEnabled()), "
            "entities, null, null, notify, scheduleDate, "
            "scheduleRepeatPeriod, sendAnimationData, updateStickersOrder)")


def one(source: str, needle: str, replacement: str) -> str:
    count = source.count(needle)
    if count != 1:
        raise ValueError(f"Pinned sanitizer anchor mismatch ({count}): {needle[:100]!r}")
    return source.replace(needle, replacement, 1)


def integrate(root: Path) -> None:
    target = root / CHAT
    source = target.read_text(encoding="utf-8")
    if "ultSanitizedText" in source:
        raise ValueError("Link sanitizer already integrated")
    for name in ("UltLinkSanitizer.java", "UltLinkSanitizerPolicy.java"):
        model = root / "TMessagesProj/src/main/java/com/yooshastan/ultmessenger/settings" / name
        if not model.is_file():
            raise ValueError("Missing installed sanitizer policy: " + name)
    # Validate all anchors BEFORE making any changes.
    for anchor in (METHOD, SEND):
        if source.count(anchor) != 1:
            raise ValueError("Changed upstream sanitizer target: " + anchor[:80])
    source = one(source, METHOD, METHOD + INTRO)
    source = one(source, SEND, NEW_SEND)
    target.write_text(source, encoding="utf-8")


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("source_root", type=Path)
    integrate(parser.parse_args().source_root)
    print("Installed opt-in outgoing link sanitizer")

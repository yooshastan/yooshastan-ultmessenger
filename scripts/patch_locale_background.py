#!/usr/bin/env python3
"""Move remote language-file processing off the Android UI thread.

Applies only to the pinned Mercurygram LocaleController layout. Network callbacks
may return on a background thread, but the existing upstream code forced heavy
language XML parsing and file writes onto the UI thread, triggering ANRs.
The existing saveRemoteLocaleStrings implementation already posts its successful
UI update/completion work to AndroidUtilities.runOnUIThread.
"""
import argparse
from pathlib import Path

TARGET = Path("TMessagesProj/src/main/java/org/telegram/messenger/LocaleController.java")

CALL = "saveRemoteLocaleStrings(localeInfo, (TLRPC.TL_langPackDifference) response, currentAccount, onPartlyDone)"
DISPATCH = "dispatchUltRemoteLocaleSave(localeInfo, (TLRPC.TL_langPackDifference) response, currentAccount, onPartlyDone)"

SINGLE = "AndroidUtilities.runOnUIThread(() -> " + CALL + ");"
BLOCK = (
    "AndroidUtilities.runOnUIThread(() -> {\n"
    "                            " + CALL + ";\n"
    "                        });"
)

ANCHOR = "    private int applyRemoteLanguage(LocaleInfo localeInfo, String langCode, boolean force, final int currentAccount, Runnable onDone) {"

HELPER = """    // Keep disk/XML work off the UI thread. The upstream save routine posts
    // successful theme/locale state updates and callbacks back onto the UI thread.
    // Empty responses follow the original UI path because it directly notifies
    // observers without entering the file-processing branch.
    private void dispatchUltRemoteLocaleSave(LocaleInfo localeInfo, TLRPC.TL_langPackDifference difference,
                                             int currentAccount, Runnable onDone) {
        if (difference == null || difference.strings == null || difference.strings.isEmpty()) {
            AndroidUtilities.runOnUIThread(() -> saveRemoteLocaleStrings(localeInfo, difference, currentAccount, onDone));
        } else {
            Utilities.globalQueue.postRunnable(() -> saveRemoteLocaleStrings(localeInfo, difference, currentAccount, onDone));
        }
    }

"""


def patch_source(source: str) -> str:
    if source.count(ANCHOR) != 1 or source.count(SINGLE) != 1 or source.count(BLOCK) != 3:
        raise ValueError("Pinned LocaleController anchors changed; review upstream before patching")
    updated = source.replace(SINGLE, DISPATCH + ";", 1)
    updated = updated.replace(BLOCK, DISPATCH + ";")
    return updated.replace(ANCHOR, HELPER + ANCHOR, 1)


def integrate(root: Path) -> Path:
    target = root / TARGET
    source = target.read_text(encoding="utf-8")
    target.write_text(patch_source(source), encoding="utf-8")
    return target


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("source_root", type=Path)
    args = parser.parse_args()
    print(f"Patched {integrate(args.source_root)}")

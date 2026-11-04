#!/usr/bin/env python3
"""Guarded opt-in screenshot privacy using pinned Mercurygram FlagSecureReason.

Adds a separate OR condition to Telegram passcode screenshot protection and
reinvalidates its ref-counted reason after explicit settings change / resume.
No native tgnet/proxy, DNS or networking modifications.
"""
import argparse
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
TARGET = Path("TMessagesProj/src/main/java/org/telegram/ui/LaunchActivity.java")
MODEL = Path("settings/src/com/yooshastan/ultmessenger/settings/UltScreenPrivacy.java")
DEST = Path("TMessagesProj/src/main/java/com/yooshastan/ultmessenger/settings/UltScreenPrivacy.java")
IMPORT = "import org.telegram.messenger.FlagSecureReason;\n"
ORIGINAL = (
    "flagSecureReason = new FlagSecureReason(getWindow(), "
    "() -> SharedConfig.passcodeHash.length() > 0 "
    "&& !SharedConfig.allowScreenCapture);"
)
METHOD_ANCHOR = "    private FlagSecureReason flagSecureReason;\n"
RESUME = "    protected void onResume() {\n        super.onResume();\n"
NEW_METHOD = """    /** Refresh Ultgram screen privacy without overwriting other secure reasons. */
    public void refreshUltScreenPrivacy() {
        if (flagSecureReason != null) {
            flagSecureReason.invalidate();
        }
    }

"""

def once(src: str, anchor: str, value: str):
    n = src.count(anchor)
    if n != 1:
        raise ValueError(f"Pinned LaunchActivity changed: {anchor[:90]!r} ({n})")
    return src.replace(anchor, value, 1)

def integrate(root: Path):
    target = root / TARGET
    source = target.read_text(encoding="utf-8")
    if "refreshUltScreenPrivacy()" in source:
        raise ValueError("Screenshot privacy already integrated")
    model = (ROOT / MODEL).read_bytes()
    # All upstream anchors are checked before touching the generated tree.
    for anchor in (IMPORT, ORIGINAL, METHOD_ANCHOR, RESUME):
        if source.count(anchor) != 1:
            raise ValueError(f"Pinned source mismatch: {anchor[:90]!r}")
    source = once(source, IMPORT, IMPORT +
                  "import com.yooshastan.ultmessenger.settings.UltScreenPrivacy;\n")
    source = once(source, ORIGINAL,
                  "flagSecureReason = new FlagSecureReason(getWindow(), () -> "
                  "(SharedConfig.passcodeHash.length() > 0 && !SharedConfig.allowScreenCapture) "
                  "|| UltScreenPrivacy.isEnabled(this));")
    source = once(source, METHOD_ANCHOR, METHOD_ANCHOR + NEW_METHOD)
    source = once(source, RESUME, RESUME + "        refreshUltScreenPrivacy();\n")
    target.write_text(source, encoding="utf-8")
    dst = root / DEST
    dst.parent.mkdir(parents=True, exist_ok=True)
    dst.write_bytes(model)
    return target

if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("source_root", type=Path)
    args = parser.parse_args()
    print(f"Patched {integrate(args.source_root)}")

#!/usr/bin/env python3
"""Apply temporary ULT vector branding to a Telegram-derived source checkout."""
from pathlib import Path
import re
import shutil
import sys

def main():
    if len(sys.argv) != 2:
        raise SystemExit("Usage: apply-branding.py SOURCE_DIRECTORY")
    root = Path(sys.argv[1]).resolve()
    asset = Path(__file__).resolve().parents[1] / "assets/ult_launcher.xml"
    drawable = root / "TMessagesProj/src/main/res/drawable/ult_launcher.xml"
    if not (root / "TMessagesProj/config").is_dir() or not asset.is_file():
        raise SystemExit("Missing Telegram source or ULT vector asset")
    if drawable.exists():
        raise SystemExit("Refusing to overwrite existing ULT launcher resource")
    drawable.parent.mkdir(parents=True, exist_ok=True)
    shutil.copyfile(asset, drawable)
    modified = 0
    for manifest in sorted((root / "TMessagesProj/config").glob("*/AndroidManifest*.xml")):
        original = manifest.read_text(encoding="utf-8")
        app = re.search(r"<application\b[^>]*>", original, flags=re.DOTALL)
        if not app:
            continue
        block = app.group()
        changed = block.replace('android:icon="@mipmap/ic_launcher"',
                                'android:icon="@drawable/ult_launcher"')
        changed = changed.replace('android:roundIcon="@mipmap/ic_launcher_round"',
                                  'android:roundIcon="@drawable/ult_launcher"')
        if changed != block:
            manifest.write_text(original[:app.start()] + changed + original[app.end():],
                                encoding="utf-8")
            modified += 1
    if not modified:
        raise SystemExit("Did not find a launcher icon in any manifest")
    print(f"Applied original ULT launcher to {modified} manifest(s)")

if __name__ == "__main__":
    main()

#!/usr/bin/env python3
"""Apply original Ultgram adaptive launcher branding to Telegram-derived checkouts.

Resource and manifest checks run before writing. No existing app icon is removed
from upstream; only manifest references are switched to the unique ULT name.
"""
from pathlib import Path
import re
import shutil
import sys
import xml.etree.ElementTree as ET

ASSETS = {
    "ult_launcher.xml": "mipmap-anydpi/ult_launcher.xml",
    "ult_launcher_adaptive.xml": "mipmap-anydpi-v26/ult_launcher.xml",
    "ult_launcher_foreground.xml": "drawable/ult_launcher_foreground.xml",
    "ult_launcher_background.xml": "drawable/ult_launcher_background.xml",
}


def apply_branding(root: Path, asset_dir: Path) -> int:
    root = root.resolve()
    config = root / "TMessagesProj/config"
    res = root / "TMessagesProj/src/main/res"
    if not config.is_dir():
        raise ValueError("Missing Telegram config source")

    # Reject missing or malformed resources before mutating the source.
    destinations = []
    for name, relative in ASSETS.items():
        source = asset_dir / name
        target = res / relative
        if not source.is_file():
            raise ValueError(f"Missing Ultgram icon asset: {name}")
        if target.exists():
            raise ValueError(f"Refusing to overwrite launcher asset: {target}")
        ET.parse(source)
        destinations.append((source, target))

    changes = []
    for manifest in sorted(config.glob("*/AndroidManifest*.xml")):
        original = manifest.read_text(encoding="utf-8")
        app = re.search(r"<application\b[^>]*>", original, flags=re.DOTALL)
        if not app:
            continue
        block = app.group()
        changed = block.replace('android:icon="@mipmap/ic_launcher"',
                                'android:icon="@mipmap/ult_launcher"')
        changed = changed.replace('android:roundIcon="@mipmap/ic_launcher_round"',
                                  'android:roundIcon="@mipmap/ult_launcher"')
        if changed != block:
            changes.append((manifest, original[:app.start()] + changed + original[app.end():]))
    if not changes:
        raise ValueError("Did not find a matching launcher icon in any manifest")

    for source, target in destinations:
        target.parent.mkdir(parents=True, exist_ok=True)
        shutil.copyfile(source, target)
    for manifest, changed in changes:
        manifest.write_text(changed, encoding="utf-8")
    return len(changes)


def main():
    if len(sys.argv) != 2:
        raise SystemExit("Usage: apply-branding.py SOURCE_DIRECTORY")
    root = Path(sys.argv[1])
    assets = Path(__file__).resolve().parents[1] / "assets"
    try:
        modified = apply_branding(root, assets)
    except (ValueError, ET.ParseError) as exc:
        raise SystemExit(str(exc))
    print(f"Applied Ultgram adaptive launcher to {modified} manifest(s)")


if __name__ == "__main__":
    main()

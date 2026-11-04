#!/usr/bin/env python3
"""Configure a disposable Mercurygram checkout without exposing credentials."""
import os
from pathlib import Path
import re
import sys

PACKAGE = "com.yooshastan.ultmessenger.foss"
TITLE = "Yooshastan ULT Messenger FOSS"

def main():
    if len(sys.argv) != 2:
        raise SystemExit("Usage: configure-foss.py SOURCE_DIRECTORY")
    root = Path(sys.argv[1]).resolve()
    app_id = os.environ.get("ULT_TELEGRAM_API_ID", "")
    app_hash = os.environ.get("ULT_TELEGRAM_API_HASH", "")
    if not (app_id.isdecimal() and 1 <= int(app_id) < 2**31):
        raise SystemExit("Missing or invalid ULT_TELEGRAM_API_ID")
    if not re.fullmatch(r"[0-9a-fA-F]{32}", app_hash):
        raise SystemExit("Missing or invalid ULT_TELEGRAM_API_HASH")
    props = root / "gradle.properties"
    if not props.is_file() or not (root / "TMessagesProj/build.gradle").is_file():
        raise SystemExit("Not a Mercurygram source checkout")
    original = props.read_text(encoding="utf-8")
    old = "APP_PACKAGE=it.belloworld.mercurygram"
    if original.count(old) != 1:
        raise SystemExit("Unexpected upstream APP_PACKAGE layout")
    props.write_text(original.replace(old, f"APP_PACKAGE={PACKAGE}"), encoding="utf-8")
    keys = root / "API_KEYS"
    if keys.exists():
        raise SystemExit("Refusing to overwrite existing API_KEYS")
    keys.write_text(f"APP_ID = {app_id}\nAPP_HASH = {app_hash}\n", encoding="utf-8")
    keys.chmod(0o600)
    for manifest in sorted((root / "TMessagesProj/config").glob("*/AndroidManifest*.xml")):
        data = manifest.read_text(encoding="utf-8")
        pattern = r'(<application\b[^>]*?)android:label="@string/AppName(?:Beta)?"'
        updated, n = re.subn(pattern, lambda m: m.group(1) + 'android:label="' + TITLE + '"',
                             data, count=1, flags=re.DOTALL)
        if n:
            manifest.write_text(updated, encoding="utf-8")
    print("FOSS identity configured. Do not distribute until original icons and signing are configured.")

if __name__ == "__main__":
    main()

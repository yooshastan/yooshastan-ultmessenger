#!/usr/bin/env python3
"""Apply local prototype configuration to a separately checked-out Telegram tree.

No API credentials are stored in this script. Never commit generated source files
containing real app credentials.
"""
import os
from pathlib import Path
import re
import sys

APP_PACKAGE = "com.yooshastan.ultmessenger"
APP_NAME = "Yooshastan ULT Messenger"

def replace_exact(path: Path, previous: str, replacement: str) -> None:
    data = path.read_text(encoding="utf-8")
    count = data.count(previous)
    if count != 1:
        raise RuntimeError(f"Expected exactly one occurrence in {path.name}; found {count}")
    path.write_text(data.replace(previous, replacement), encoding="utf-8")

def main() -> int:
    if len(sys.argv) != 2:
        print("Usage: configure-prototype.py PATH_TO_TELEGRAM_SOURCE", file=sys.stderr)
        return 2
    source = Path(sys.argv[1]).resolve()
    api_id = os.environ.get("ULT_TELEGRAM_API_ID", "")
    api_hash = os.environ.get("ULT_TELEGRAM_API_HASH", "")
    if not (api_id.isdecimal() and 1 <= int(api_id) < 2**31):
        print("Provide ULT_TELEGRAM_API_ID as a positive integer", file=sys.stderr)
        return 2
    if not re.fullmatch(r"[0-9a-fA-F]{32}", api_hash):
        print("Provide ULT_TELEGRAM_API_HASH as 32 hexadecimal characters", file=sys.stderr)
        return 2
    props = source / "gradle.properties"
    vars_file = source / "TMessagesProj/src/main/java/org/telegram/messenger/BuildVars.java"
    if not (props.is_file() and vars_file.is_file()):
        raise FileNotFoundError("Not a Telegram Android source checkout")

    replace_exact(props, "APP_PACKAGE=org.telegram.messenger", f"APP_PACKAGE={APP_PACKAGE}")
    replace_exact(vars_file, "public static int APP_ID = 4;", f"public static int APP_ID = {api_id};")
    replace_exact(vars_file, 'public static String APP_HASH = "014b35b6184100b085b0d0572f9b5103";',
                  f'public static String APP_HASH = "{api_hash}";')
    replace_exact(vars_file, "public static boolean SUPPORTS_PASSKEYS = true;",
                  "public static boolean SUPPORTS_PASSKEYS = false;")
    replace_exact(vars_file, "public static boolean CHECK_UPDATES = true;",
                  "public static boolean CHECK_UPDATES = false;")

    # Only touch application labels, not labels for activities/services.
    for manifest in sorted((source / "TMessagesProj/config").glob("*/AndroidManifest*.xml")):
        data = manifest.read_text(encoding="utf-8")
        pattern = r'(<application\b[^>]*?)android:label="@string/AppName(?:Beta)?"'
        updated, count = re.subn(pattern, lambda m: m.group(1) + 'android:label="' + APP_NAME + '"',
                                 data, count=1, flags=re.DOTALL)
        if count:
            manifest.write_text(updated, encoding="utf-8")

    print("Applied independent package and brand configuration.")
    print("Do not distribute until logo, signing, Firebase, and full source/license checks are complete.")
    return 0

if __name__ == "__main__":
    raise SystemExit(main())

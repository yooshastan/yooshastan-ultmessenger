#!/usr/bin/env python3
"""Read-only Android APK identity, signature and upgrade-compatibility gate.

Requires Android SDK 'aapt' and 'apksigner'. Both are read-only invocations.
--apk checks one signed APK; --previous additionally checks a possible in-place
upgrade against an APK the user has retained and trusts. It never installs,
uninstalls, signs, uploads or opens account data.
"""
import argparse
import os
from pathlib import Path
import re
import shutil
import subprocess
import sys

PACKAGE = re.compile(r"^package:\s+name='([^']+)'\s+versionCode='([0-9]+)'\s+versionName='([^']*)'", re.M)
CERT = re.compile(r"^Signer #([0-9]+) certificate SHA-256 digest:\s*([0-9a-fA-F:]+)\s*$", re.M)


def parse_aapt(output):
    match = PACKAGE.search(output)
    if not match:
        raise ValueError("Cannot read APK package/versionCode from aapt")
    name, code, version = match.groups()
    if not re.fullmatch(r"[A-Za-z][A-Za-z0-9_.]*", name):
        raise ValueError("Invalid Android package")
    return name, int(code), version


def parse_signing(output):
    matches = CERT.findall(output)
    if len(matches) != 1 or matches[0][0] != "1":
        raise ValueError("Expected exactly one APK signing certificate")
    normalized = matches[0][1].replace(":", "").lower()
    if not re.fullmatch("[0-9a-f]{64}", normalized):
        raise ValueError("Invalid signing certificate SHA-256")
    return normalized


def find_android_tool(name):
    explicit = shutil.which(name)
    if explicit:
        return explicit
    for key in ("ANDROID_HOME", "ANDROID_SDK_ROOT"):
        root = os.environ.get(key)
        if root:
            candidates = list((Path(root) / "build-tools").glob("*/" + name))
            for path in sorted(candidates, reverse=True):
                if path.is_file():
                    return str(path)
    raise FileNotFoundError(name + " not found in PATH or Android SDK build-tools")


def inspect(path):
    if not path.is_file():
        raise FileNotFoundError("APK not found: " + str(path))
    signer_tool = find_android_tool("apksigner")
    aapt_tool = find_android_tool("aapt")
    # verify raises on invalid or unsigned APK; output includes signer digest.
    signed = subprocess.run([signer_tool, "verify", "--verbose", "--print-certs",
                             str(path)], check=True, text=True,
                            capture_output=True, timeout=120)
    badge = subprocess.run([aapt_tool, "dump", "badging", str(path)],
                           check=True, text=True, capture_output=True, timeout=120)
    package, code, version = parse_aapt(badge.stdout)
    cert = parse_signing(signed.stdout)
    return package, code, version, cert


def compare(previous, candidate):
    if previous[0] != candidate[0]:
        raise ValueError("Package ID differs; not an in-place upgrade")
    if previous[3] != candidate[3]:
        raise ValueError("Signing certificate differs; do NOT uninstall working app to bypass")
    if candidate[1] < previous[1]:
        raise ValueError("Version code goes backwards; Android will reject downgrade")
    return True


def render(candidate, previous=None):
    package, code, version, cert = candidate
    lines = ["APK signature: VERIFIED with apksigner (not release certification)",
             "Package: " + package, "Version code: " + str(code),
             "Version name: " + version, "Signer SHA-256: " + cert]
    if previous is None:
        lines += ["Upgrade compatibility: NOT CHECKED (previous installed APK not supplied)"]
    else:
        compare(previous, candidate)
        lines += ["Upgrade compatibility with selected previous APK: PASS",
                  "WARNING: This does not establish what is installed on a phone."]
    lines += ["Debug-signed internal build; do not infer public-release readiness.",
              "No APK installed, modified, uploaded or uninstalled by this gate."]
    return "\n".join(lines) + "\n"


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--apk", required=True, type=Path)
    parser.add_argument("--previous", type=Path)
    parser.add_argument("--report", type=Path)
    args = parser.parse_args()
    try:
        current = inspect(args.apk)
        previous = inspect(args.previous) if args.previous is not None else None
        report = render(current, previous)
        if args.report is not None:
            args.report.parent.mkdir(parents=True, exist_ok=True)
            args.report.write_text(report, encoding="utf-8")
        print(report, end="")
    except (ValueError, FileNotFoundError, subprocess.CalledProcessError,
            subprocess.TimeoutExpired) as exc:
        print("APK release gate FAILED: " + str(exc), file=sys.stderr)
        return 1
    return 0


if __name__ == "__main__":
    sys.exit(main())

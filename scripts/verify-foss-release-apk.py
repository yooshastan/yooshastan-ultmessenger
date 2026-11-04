#!/usr/bin/env python3
"""Verify one privately signed, non-debuggable public FOSS release APK."""
import argparse
from pathlib import Path
import re
import subprocess
import sys

from apk_release_gate import find_android_tool, inspect

INTERNAL_SIGNER = "a08d7dc323ddf71ef3201944397e0d3cce7d40847263e11f328b68bbe19229ab"
PUBLIC_PACKAGE = "com.yooshastan.ultmessenger.foss"


def validate(apk: Path, expected_code: int, expected_version: str,
             expected_cert: str) -> str:
    expected_cert = expected_cert.replace(":", "").lower().strip()
    if not re.fullmatch(r"[0-9a-f]{64}", expected_cert):
        raise ValueError("Expected public signing certificate SHA-256 not configured")
    if expected_cert == INTERNAL_SIGNER:
        raise ValueError("Refusing inherited internal/upstream signing identity")
    package, code, version, actual_cert = inspect(apk)
    if package != PUBLIC_PACKAGE:
        raise ValueError("Not the permanent public FOSS package ID")
    if code != expected_code or version != expected_version:
        raise ValueError("Unexpected APK version: build metadata mismatch")
    if actual_cert == INTERNAL_SIGNER or actual_cert != expected_cert:
        raise ValueError("Release signing certificate does not match approved fingerprint")
    badging = subprocess.run(
        [find_android_tool("aapt"), "dump", "badging", str(apk)],
        check=True, text=True, capture_output=True, timeout=120,
    ).stdout
    if "application-debuggable" in badging:
        raise ValueError("Public APK must not be debuggable")
    return (
        "ULTGRAM FOSS RELEASE GATE: PASS\n"
        "Application ID: " + package + "\n"
        "Version code: " + str(code) + "\n"
        "Version name: " + version + "\n"
        "Signer certificate SHA-256: " + actual_cert + "\n"
        "Debuggable: NO\n"
        "APK signing: verified with apksigner\n"
        "Prior-device upgrade compatibility: NOT CHECKED\n"
        "No APK has been published or installed by this verification tool.\n"
    )


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--apk", type=Path, required=True)
    parser.add_argument("--version-code", type=int, required=True)
    parser.add_argument("--version-name", required=True)
    parser.add_argument("--cert-sha256", required=True)
    parser.add_argument("--report", type=Path, required=True)
    args = parser.parse_args()
    try:
        report = validate(args.apk, args.version_code, args.version_name,
                          args.cert_sha256)
        args.report.parent.mkdir(parents=True, exist_ok=True)
        args.report.write_text(report, encoding="utf-8")
        print(report)
    except (ValueError, OSError, subprocess.CalledProcessError,
            subprocess.TimeoutExpired) as exc:
        print("FOSS release gate FAILED: " + str(exc), file=sys.stderr)
        return 1
    return 0


if __name__ == "__main__":
    raise SystemExit(main())

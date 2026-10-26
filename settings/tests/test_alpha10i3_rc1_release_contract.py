#!/usr/bin/env python3
"""RC1 packaging and release-claim contracts — no Android code changes."""
from pathlib import Path
from unittest import TestCase, main

ROOT = Path(__file__).resolve().parents[2]
WORKFLOW = ROOT / ".github/workflows/android-alpha.yml"
README = ROOT / "README.md"
NOTES = ROOT / "docs/ALPHA10I3_RC1_RELEASE_NOTES.md"
NATIVE = ROOT / "scripts/integrate-native-strict.py"

class RC1Contract(TestCase):
    def test_foss_only_candidate_has_integrity_metadata(self):
        workflow = WORKFLOW.read_text(encoding="utf-8")
        for required in ("release/alpha10i3-rc1", "variant: [foss]",
                         "ult-alpha10i3-rc1-foss-arm64-debug.apk",
                         "ult-alpha10i3-rc1-foss-android",
                         "SHA256SUMS", "BUILD_INFO.txt",
                         'sha256sum ult-alpha10i3-rc1-foss-arm64-debug.apk',
                         "artifacts/*", "retention-days: 30"):
            with self.subTest(required=required):
                self.assertIn(required, workflow)
        self.assertNotIn("variant: [foss, firebase]", workflow)

    def test_documentation_does_not_assert_incomplete_privacy_or_signing(self):
        notes = NOTES.read_text(encoding="utf-8")
        readme = README.read_text(encoding="utf-8")
        for required in ("debug", "not", "DoQ", "NOT ROUTE-VERIFIED",
                         "source", "signed", "Pixel 8a", "nested draft PR stack"):
            self.assertIn(required.lower(), notes.lower())
        self.assertIn("Alpha 0.10i.3 RC1", readme)
        self.assertIn("not leak-verified", readme)
        self.assertIn("SOCKET_GUARD", NATIVE.read_text(encoding="utf-8"))

if __name__ == "__main__":
    main()

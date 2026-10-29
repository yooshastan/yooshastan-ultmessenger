#!/usr/bin/env python3
"""RC1 packaging and release-claim contracts — no Android code changes."""
from pathlib import Path
import re
from unittest import TestCase, main

ROOT = Path(__file__).resolve().parents[2]
WORKFLOW = ROOT / ".github/workflows/android-alpha.yml"
README = ROOT / "README.md"
NOTES = ROOT / "docs/ALPHA10I3_RC1_RELEASE_NOTES.md"
NATIVE = ROOT / "scripts/integrate-native-strict.py"

class RC1Contract(TestCase):
    def test_foss_only_candidate_has_integrity_metadata(self):
        workflow = WORKFLOW.read_text(encoding="utf-8")
        for required in ("variant: [foss]",
                         "SHA256SUMS", "BUILD_INFO.txt", "artifacts/*",
                         "retention-days: 30"):
            with self.subTest(required=required):
                self.assertIn(required, workflow)
        self.assertRegex(workflow, r"branches: \[release/alpha(?:11bc|12ab|12cd|13ab|13cd|14ab|14c)-combined-rc1\]")
        self.assertNotIn("variant: [foss, firebase]", workflow)
        # Release candidates and later alphas rename the debug APK. Guard
        # consistency of filename, checksum and artifact rather than RC1 spelling.
        match = re.search(r'artifacts/(ult-[a-z0-9-]+-foss-arm64-debug\.apk)', workflow)
        self.assertIsNotNone(match, "No FOSS ARM64 debug APK staged")
        apk = match.group(1)
        self.assertIn("sha256sum " + apk + " > SHA256SUMS", workflow)
        self.assertRegex(workflow, r'name: ult-[a-z0-9-]+-foss-android')

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

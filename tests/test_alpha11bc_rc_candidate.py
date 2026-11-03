#!/usr/bin/env python3
"""RC1 smoke contracts; does not replace Android compilation or capture tests."""
from pathlib import Path
import re
from unittest import TestCase, main

ROOT = Path(__file__).resolve().parents[1]
WORKFLOW = ROOT / ".github/workflows/android-alpha.yml"
CHAT = ROOT / "conversation/src/com/yooshastan/ultmessenger/conversation/UltChatView.java"
AUDIT = ROOT / "settings/src/com/yooshastan/ultmessenger/settings/UltNetworkSourceMatrix.java"
UI = ROOT / "settings/src/com/yooshastan/ultmessenger/settings/UltSettingsActivity.java"

class RC1(TestCase):
    def test_foss_artifact_and_integrity_provenance(self):
        w = WORKFLOW.read_text()
        # Check actual APK staging, not an outdated branch/artifact naming convention.
        branch = re.search(r"branches: \[release/(alpha[a-z0-9-]+)\]", w)
        self.assertIsNotNone(branch, "Expected versioned release candidate branch")
        apk = re.search(r'cp -- "\$\{apks\[0\]\}" "artifacts/ult-([a-z0-9-]+)-foss-arm64-debug\.apk"', w)
        self.assertIsNotNone(apk, "Expected a staged FOSS ARM64 APK")
        artifact_prefix = apk.group(1)
        for required in ("matrix:", "variant: [foss]", "SHA256SUMS",
                         "BUILD_INFO.txt", "scripts/integrate-native-strict.py",
                         "scripts/integrate-screen-privacy.py",
                         f"ult-{artifact_prefix}-foss-arm64-debug.apk",
                         f"ult-{artifact_prefix}-foss-android"):
            with self.subTest(required=required):
                self.assertIn(required, w)

    def test_alpha11bc_source_scope_and_caveats(self):
        self.assertIn("visibleTabs(String saved)", CHAT.read_text())
        self.assertIn("moveTab(String saved", CHAT.read_text())
        ui = UI.read_text()
        self.assertIn("showNetworkSourceMatrix(context)", ui)
        self.assertIn("showTabCustomization(context)", ui)
        self.assertIn("showOfflineTranslationStatus(context)", ui)
        audit = AUDIT.read_text()
        self.assertIn("NOT ROUTE-VERIFIED", audit)
        self.assertIn("NO ACTIVE CAPTURE", audit)
        self.assertNotIn("Socket(", audit)

if __name__ == "__main__":
    main()

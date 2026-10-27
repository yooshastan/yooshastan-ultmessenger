#!/usr/bin/env python3
"""Batching guard: native builds run at integration boundary, not every edit."""
from pathlib import Path
from unittest import TestCase, main

ROOT = Path(__file__).resolve().parents[1]
ANDROID = ROOT / ".github/workflows/android-alpha.yml"
LIGHT = ROOT / ".github/workflows/scaffold-check.yml"
PLAN = ROOT / "docs/ALPHA11_BATCHED_BUILD.md"

class BatchBuildPolicy(TestCase):
    def test_android_only_on_combined_candidate_or_dispatch(self):
        wf = ANDROID.read_text()
        triggers = wf.split("\nconcurrency:", 1)[0]
        self.assertIn("branches: [release/alpha11bc-combined-rc1]", triggers)
        self.assertNotIn("feature/alpha11bc-batched", triggers)
        self.assertIn("workflow_dispatch:", triggers)
        self.assertIn("ult-alpha11bc-combined-foss-arm64-debug.apk", wf)
        self.assertIn("ult-alpha11bc-combined-foss-android", wf)
        self.assertIn("SHA256SUMS", wf)
        self.assertIn("BUILD_INFO.txt", wf)
        self.assertIn("python3 scripts/integrate-native-strict.py", wf)
        self.assertIn("python3 scripts/integrate-screen-privacy.py", wf)

    def test_fast_checks_cover_both_stages(self):
        wf = LIGHT.read_text()
        self.assertIn("feature/alpha11bc-batched", wf)
        for check in ("test_alpha10g_path_audit.py", "test_chat_views_integration.py",
                      "test_integration.py", "UltChatViewTest.java",
                      "UltTranslationPolicyTest.java", "test_alpha11a_screen_privacy.py"):
            self.assertIn(check, wf)
        self.assertNotIn("./gradlew", wf)
        self.assertNotIn("sdkmanager", wf)
        self.assertIn("one full FOSS ARM64", PLAN.read_text())

if __name__ == "__main__":
    main()

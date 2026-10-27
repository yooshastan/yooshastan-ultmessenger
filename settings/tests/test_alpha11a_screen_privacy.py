#!/usr/bin/env python3
"""Regression contract: opt-in screenshot privacy must preserve upstream guard."""
import importlib.util
from pathlib import Path
from tempfile import TemporaryDirectory
from unittest import TestCase, main

ROOT = Path(__file__).resolve().parents[2]
MODULE = ROOT / "scripts/integrate-screen-privacy.py"
spec = importlib.util.spec_from_file_location("ult_screenshot_patch", MODULE)
patch = importlib.util.module_from_spec(spec)
spec.loader.exec_module(patch)

class ScreenPrivacyTest(TestCase):
    def test_pinned_patch_preserves_secure_reason_and_is_idempotence_guarded(self):
        with TemporaryDirectory() as tmp:
            base = Path(tmp)
            target = base / patch.TARGET
            target.parent.mkdir(parents=True)
            original = (patch.IMPORT + patch.METHOD_ANCHOR
                        + "public void test() { " + patch.ORIGINAL + " }\n"
                        + "    @Override\n" + patch.RESUME + "    }\n")
            target.write_text(original)
            patch.integrate(base)
            src = target.read_text()
            self.assertIn("|| UltScreenPrivacy.isEnabled(this)", src)
            self.assertIn("SharedConfig.passcodeHash.length() > 0 && !SharedConfig.allowScreenCapture", src)
            self.assertIn("flagSecureReason.invalidate()", src)
            self.assertIn("        refreshUltScreenPrivacy();", src)
            self.assertEqual(src.count("public void refreshUltScreenPrivacy()"), 1)
            self.assertTrue((base / patch.DEST).is_file())
            with self.assertRaises(ValueError):
                patch.integrate(base)

    def test_drift_fails_without_side_effects(self):
        with TemporaryDirectory() as tmp:
            root = Path(tmp)
            target = root / patch.TARGET
            target.parent.mkdir(parents=True)
            target.write_text("class LaunchActivity {}")
            with self.assertRaises(ValueError):
                patch.integrate(root)
            self.assertEqual(target.read_text(), "class LaunchActivity {}")
            self.assertFalse((root / patch.DEST).exists())

    def test_ui_is_off_by_default_and_can_disable_without_overriding_telegram(self):
        model = (ROOT / "settings/src/com/yooshastan/ultmessenger/settings/UltScreenPrivacy.java").read_text()
        ui = (ROOT / "settings/src/com/yooshastan/ultmessenger/settings/UltSettingsActivity.java").read_text()
        self.assertIn("getBoolean(KEY_ENABLED, false)", model)
        self.assertIn("putBoolean(KEY_ENABLED, enabled)", model)
        self.assertIn("Protect Ultgram screenshots", ui)
        self.assertIn("refreshUltScreenPrivacy()", ui)
        self.assertIn("notifications, other application windows", ui)
        self.assertNotIn("getWindow().clearFlags(", ui)

if __name__ == "__main__":
    main()

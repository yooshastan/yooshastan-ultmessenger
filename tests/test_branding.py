#!/usr/bin/env python3
"""Alpha 0.7 adaptive icon installation regression tests."""
import importlib.util
from pathlib import Path
from tempfile import TemporaryDirectory
from unittest import TestCase, main

PROJECT = Path(__file__).resolve().parents[1]
MODULE = PROJECT / "scripts/apply-branding.py"
spec = importlib.util.spec_from_file_location("ult_branding", MODULE)
branding = importlib.util.module_from_spec(spec)
spec.loader.exec_module(branding)


class BrandingTest(TestCase):
    def _fixture(self, root):
        manifest = root / "TMessagesProj/config/debug/AndroidManifest.xml"
        manifest.parent.mkdir(parents=True)
        manifest.write_text(
            '<manifest xmlns:android="http://schemas.android.com/apk/res/android">'
            '<application android:icon="@mipmap/ic_launcher" '
            'android:roundIcon="@mipmap/ic_launcher_round" /></manifest>',
            encoding="utf-8",
        )
        return manifest

    def test_installs_legacy_and_adaptive_without_overwriting_upstream(self):
        with TemporaryDirectory() as tmp:
            root = Path(tmp)
            manifest = self._fixture(root)
            self.assertEqual(branding.apply_branding(root, PROJECT / "assets"), 1)
            content = manifest.read_text(encoding="utf-8")
            self.assertIn('android:icon="@mipmap/ult_launcher"', content)
            self.assertIn('android:roundIcon="@mipmap/ult_launcher"', content)
            for relative in branding.ASSETS.values():
                self.assertTrue((root / "TMessagesProj/src/main/res" / relative).is_file())
            self.assertIn("adaptive-icon", (root / "TMessagesProj/src/main/res/mipmap-anydpi-v26/ult_launcher.xml").read_text())
            with self.assertRaises(ValueError):
                branding.apply_branding(root, PROJECT / "assets")

    def test_missing_manifest_fails_without_creating_resources(self):
        with TemporaryDirectory() as tmp:
            root = Path(tmp)
            (root / "TMessagesProj/config").mkdir(parents=True)
            with self.assertRaises(ValueError):
                branding.apply_branding(root, PROJECT / "assets")
            self.assertFalse((root / "TMessagesProj/src/main/res").exists())


if __name__ == "__main__":
    main()

#!/usr/bin/env python3
"""Fail-closed source integration smoke tests without fetching Android sources."""
import importlib.util
from pathlib import Path
from tempfile import TemporaryDirectory
from unittest import TestCase, main

MODULE = Path(__file__).resolve().parents[1] / "scripts/integrate-appearance.py"
spec = importlib.util.spec_from_file_location("ult_appearance_integration", MODULE)
module = importlib.util.module_from_spec(spec)
spec.loader.exec_module(module)

class AppearanceIntegrationTest(TestCase):
    def test_injects_single_menu_and_preset_source(self):
        with TemporaryDirectory() as tmp:
            root = Path(tmp)
            target = root / module.TARGET
            target.parent.mkdir(parents=True)
            fixture = (module.IMPORT_ANCHOR + module.CLASS_ANCHOR +
                       module.MENU_ANCHOR + module.CLICK_ANCHOR)
            target.write_text(fixture)
            module.integrate(root)
            result = target.read_text()
            self.assertIn("showUltPresetChooser()", result)
            self.assertIn("ULT Presets", result)
            self.assertIn("getSharedPreferences", result)
            self.assertTrue((root / "TMessagesProj/src/main/java/com/yooshastan/ultmessenger/appearance/UltAppearancePreset.java").is_file())
            with self.assertRaises(ValueError):
                module.integrate(root)

    def test_rejects_upstream_drift_without_writing(self):
        with TemporaryDirectory() as tmp:
            root = Path(tmp)
            target = root / module.TARGET
            target.parent.mkdir(parents=True)
            target.write_text("upstream changed")
            with self.assertRaises(ValueError):
                module.integrate(root)
            self.assertEqual(target.read_text(), "upstream changed")

if __name__ == "__main__":
    main()

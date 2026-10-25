#!/usr/bin/env python3
"""Guarded Ultgram Settings entry tests without the native Android source."""
import importlib.util
from pathlib import Path
from tempfile import TemporaryDirectory
from unittest import TestCase, main

MODULE = Path(__file__).resolve().parents[2] / "scripts/integrate-ult-settings.py"
spec = importlib.util.spec_from_file_location("ult_settings_integration", MODULE)
integration = importlib.util.module_from_spec(spec)
spec.loader.exec_module(integration)


class SettingsIntegrationTest(TestCase):
    def test_entry_keeps_mercurygram_and_adds_ult(self):
        with TemporaryDirectory() as temp:
            root = Path(temp)
            target = root / integration.TARGET
            target.parent.mkdir(parents=True)
            target.write_text(integration.ROW_ANCHOR + integration.CLICK_ANCHOR, encoding="utf-8")
            integration.integrate(root)
            src = target.read_text(encoding="utf-8")
            self.assertIn('"Ultgram Settings"', src)
            self.assertIn("new com.yooshastan.ultmessenger.settings.UltSettingsActivity()", src)
            self.assertIn("getString(R.string.MercurygramSettings)", src)
            self.assertIn("new it.belloworld.mercurygram.ui.MercurygramSettingsActivity()", src)
            model = root / integration.DEST
            self.assertTrue(model.is_file())
            self.assertIn("ULT chat layout presets", model.read_text())
            self.assertIn("Archive row visibility", model.read_text())
            self.assertIn("Experimental native proxy-only mode", model.read_text())
            self.assertIn("ConnectionsManager.setUltStrictNativeProxyEnabled", model.read_text())
            self.assertIn("Encrypted DNS / DoQ and tracker protection", model.read_text())
            self.assertIn("No encrypted-only DNS or tracker blocking is active.", model.read_text())
            with self.assertRaises(ValueError):
                integration.integrate(root)

    def test_changed_upstream_refuses_without_modification(self):
        with TemporaryDirectory() as temp:
            root = Path(temp)
            target = root / integration.TARGET
            target.parent.mkdir(parents=True)
            target.write_text(integration.ROW_ANCHOR + "modified click handler", encoding="utf-8")
            with self.assertRaises(ValueError):
                integration.integrate(root)
            self.assertEqual(target.read_text(), integration.ROW_ANCHOR + "modified click handler")


if __name__ == "__main__":
    main()

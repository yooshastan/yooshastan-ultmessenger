#!/usr/bin/env python3
"""Source integration checks for pinned native Chat Settings."""
import importlib.util
from pathlib import Path
from tempfile import TemporaryDirectory
from unittest import TestCase, main

MODULE = Path(__file__).resolve().parents[2] / "scripts/integrate-chat-layouts.py"
spec = importlib.util.spec_from_file_location("ult_chat_integration", MODULE)
integration = importlib.util.module_from_spec(spec)
spec.loader.exec_module(integration)


class ChatIntegrationTest(TestCase):
    def test_adds_ult_menu_and_preset_class(self):
        with TemporaryDirectory() as tmp:
            root = Path(tmp)
            target = root / integration.TARGET
            target.parent.mkdir(parents=True)
            source = (integration.IMPORT_ANCHOR + integration.CLASS_ANCHOR
                      + integration.MENU_ANCHOR + integration.CLICK_ANCHOR
                      + "private boolean setFontSize(int size) {}\n"
                      + "private boolean setBubbleRadius(int size, boolean layout) {}\n")
            target.write_text(source, encoding="utf-8")
            integration.integrate(root)
            result = target.read_text(encoding="utf-8")
            self.assertIn("ULT Chat Layout", result)
            self.assertIn("showUltChatLayoutChooser", result)
            self.assertIn("setFontSize(preset.targetFontSize", result)
            self.assertIn("setBubbleRadius(preset.targetBubbleRadius", result)
            self.assertIn("ULT_CHAT_ORIGINAL_FONT", result)
            self.assertTrue((root / "TMessagesProj/src/main/java/com/yooshastan/ultmessenger/chat/UltChatLayoutPreset.java").is_file())
            with self.assertRaises(ValueError):
                integration.integrate(root)

    def test_drift_is_rejected_without_mutating_upstream(self):
        with TemporaryDirectory() as tmp:
            root = Path(tmp)
            target = root / integration.TARGET
            target.parent.mkdir(parents=True)
            target.write_text("changed upstream")
            with self.assertRaises(ValueError):
                integration.integrate(root)
            self.assertEqual(target.read_text(), "changed upstream")


if __name__ == "__main__":
    main()

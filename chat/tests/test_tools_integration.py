#!/usr/bin/env python3
"""Guarded pinned ChatActivity read-only tool integration checks."""
import importlib.util
from pathlib import Path
from tempfile import TemporaryDirectory
from unittest import TestCase, main

MODULE = Path(__file__).resolve().parents[2] / "scripts/integrate-chat-tools.py"
spec = importlib.util.spec_from_file_location("ult_chat_tools_integration", MODULE)
integration = importlib.util.module_from_spec(spec)
spec.loader.exec_module(integration)


class ChatToolsIntegrationTest(TestCase):
    def test_adds_read_only_tools_and_model(self):
        with TemporaryDirectory() as tmp:
            root = Path(tmp)
            target = root / integration.TARGET
            target.parent.mkdir(parents=True)
            source = (integration.IMPORT_ANCHOR + integration.CLASS_ANCHOR
                      + integration.MENU_ANCHOR + integration.CLICK_ANCHOR)
            target.write_text(source, encoding="utf-8")
            integration.integrate(root)
            result = target.read_text(encoding="utf-8")
            self.assertIn("ULT Chat Tools", result)
            self.assertIn("showUltChatTools()", result)
            self.assertIn("UltChatTools.numericId", result)
            self.assertIn("UltChatTools.publicLink", result)
            self.assertIn("ChatObject.getPublicUsername(currentChat)", result)
            self.assertIn("AndroidUtilities.addToClipboard", result)
            self.assertNotIn("sendMessage(", result)
            self.assertTrue((root / "TMessagesProj/src/main/java/com/yooshastan/ultmessenger/chat/UltChatTools.java").is_file())
            with self.assertRaises(ValueError):
                integration.integrate(root)

    def test_rejects_modified_upstream(self):
        with TemporaryDirectory() as tmp:
            root = Path(tmp)
            target = root / integration.TARGET
            target.parent.mkdir(parents=True)
            target.write_text("moved menus")
            with self.assertRaises(ValueError):
                integration.integrate(root)
            self.assertEqual(target.read_text(), "moved menus")


if __name__ == "__main__":
    main()

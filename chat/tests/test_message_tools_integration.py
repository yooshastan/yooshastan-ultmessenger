#!/usr/bin/env python3
"""Guarded read-only long-press message tools injection checks."""
import importlib.util
from pathlib import Path
from tempfile import TemporaryDirectory
from unittest import TestCase, main

MODULE = Path(__file__).resolve().parents[2] / "scripts/integrate-message-tools.py"
spec = importlib.util.spec_from_file_location("ult_message_tools_integration", MODULE)
integration = importlib.util.module_from_spec(spec)
spec.loader.exec_module(integration)


class MessageToolsIntegrationTest(TestCase):
    def test_adds_message_and_sender_copy_without_write_operations(self):
        with TemporaryDirectory() as tmp:
            root = Path(tmp)
            target = root / integration.TARGET
            target.parent.mkdir(parents=True)
            fixture = ("    private static final int ULT_CHAT_TOOLS_MENU_ID = 0x554c55;\n"
                       "    private void test() { UltChatTools.numericId(3); }\n"
                       + integration.CONSTANT_ANCHOR
                       + integration.MENU_ANCHOR
                       + integration.HANDLER_ANCHOR)
            target.write_text(fixture, encoding="utf-8")
            integration.integrate(root)
            content = target.read_text(encoding="utf-8")
            self.assertIn("ULT: Copy reference", content)
            self.assertIn("ULT: Copy file info", content)
            self.assertEqual(content.count('items.add("ULT: Copy'), 2)
            self.assertNotIn("ULT: Copy message ID", content)
            self.assertNotIn("ULT: Copy message time", content)
            self.assertNotIn("ULT: Copy metadata (JSON)", content)
            self.assertIn("UltChatTools.messageReference(", content)
            self.assertIn("UltChatTools.mediaSummary(", content)
            self.assertIn("OPTION_ULT_COPY_MESSAGE_REFERENCE", content)
            self.assertIn("currentEncryptedChat == null", content)
            self.assertIn("!selectedObject.isSponsored()", content)
            self.assertIn("!selectedObject.messageOwner.noforwards", content)
            self.assertIn("AndroidUtilities.addToClipboard", content)
            self.assertNotIn("sendMessage(", content)
            self.assertNotIn("forwardMessages(", content)
            with self.assertRaises(ValueError):
                integration.integrate(root)

    def test_rejects_upstream_drift_without_modifying_source(self):
        with TemporaryDirectory() as tmp:
            root = Path(tmp)
            target = root / integration.TARGET
            target.parent.mkdir(parents=True)
            fixture = "    private static final int ULT_CHAT_TOOLS_MENU_ID = 0x554c55;\n"
            target.write_text(fixture, encoding="utf-8")
            with self.assertRaises(ValueError):
                integration.integrate(root)
            self.assertEqual(target.read_text(encoding="utf-8"), fixture)

    def test_requires_alpha05(self):
        with TemporaryDirectory() as tmp:
            root = Path(tmp)
            target = root / integration.TARGET
            target.parent.mkdir(parents=True)
            fixture = (integration.CONSTANT_ANCHOR +
                       integration.MENU_ANCHOR +
                       integration.HANDLER_ANCHOR)
            target.write_text(fixture, encoding="utf-8")
            with self.assertRaises(ValueError):
                integration.integrate(root)


if __name__ == "__main__":
    main()

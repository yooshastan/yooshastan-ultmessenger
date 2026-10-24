#!/usr/bin/env python3
"""Offline boundary and per-chat translation integration regression tests."""
import importlib.util
from pathlib import Path
from tempfile import TemporaryDirectory
from unittest import TestCase, main

SCRIPT = Path(__file__).resolve().parents[2] / "scripts/integrate-translation.py"
spec = importlib.util.spec_from_file_location("ult_translation_integration", SCRIPT)
integration = importlib.util.module_from_spec(spec)
spec.loader.exec_module(integration)

class TranslationIntegrationTest(TestCase):
    def fixture(self, root):
        chat = root / integration.CHAT
        dispatcher = root / integration.DISPATCHER
        chat.parent.mkdir(parents=True, exist_ok=True)
        dispatcher.parent.mkdir(parents=True, exist_ok=True)
        chat.write_text("".join((
            integration.IMPORT, integration.CLASS, integration.MENU, integration.CLICK
        )), encoding="utf-8")
        dispatcher.write_text("".join((
            integration.OFFLINE_FAILURE, integration.MISSING_PROVIDER, integration.CREDIT
        )), encoding="utf-8")
        return chat, dispatcher

    def test_chat_controls_require_consent_and_keep_local_provider_strict(self):
        with TemporaryDirectory() as directory:
            root = Path(directory)
            chat, dispatch = self.fixture(root)
            integration.integrate(root)
            c = chat.read_text(encoding="utf-8")
            d = dispatch.read_text(encoding="utf-8")
            self.assertIn("ULT Chat Translation", c)
            self.assertIn("Stop translating this chat", c)
            self.assertIn("Translate to Korean", c.replace('languageNames = {"English", "Korean"', "Translate to Korean"))
            self.assertIn("requiresRemoteConsent", c)
            self.assertIn("Send chat text for translation?", c)
            self.assertIn("DialogObject.isEncryptedDialog(getDialogId())", c)
            self.assertIn("setDialogTranslateTo", c)
            self.assertNotIn("setHideTranslateDialog", c)
            self.assertIn("ULT strict offline", d)
            self.assertNotIn(integration.OFFLINE_FAILURE, d)
            self.assertNotIn(integration.MISSING_PROVIDER, d)
            self.assertIn("runAlternative(text, fromLng, toLng, null, done)", d)
            self.assertTrue((root / integration.DEST).exists())
            with self.assertRaises(ValueError):
                integration.integrate(root)

    def test_upstream_drift_fails_without_partial_modification(self):
        with TemporaryDirectory() as directory:
            root = Path(directory)
            chat, dispatch = self.fixture(root)
            dispatch.write_text(dispatch.read_text().replace(integration.OFFLINE_FAILURE, "changed"))
            before_chat = chat.read_bytes()
            before_dispatch = dispatch.read_bytes()
            with self.assertRaises(ValueError):
                integration.integrate(root)
            self.assertEqual(before_chat, chat.read_bytes())
            self.assertEqual(before_dispatch, dispatch.read_bytes())
            self.assertFalse((root / integration.DEST).exists())

    def test_duplicate_anchor_fails(self):
        with TemporaryDirectory() as directory:
            root = Path(directory)
            chat, _ = self.fixture(root)
            chat.write_text(chat.read_text() + integration.MENU)
            with self.assertRaises(ValueError):
                integration.integrate(root)
            self.assertNotIn("ULT_TRANSLATION_MENU_ID", chat.read_text())

if __name__ == "__main__":
    main()

#!/usr/bin/env python3
"""Guard opt-in media, appearance, and no-false-guarantee network audit."""
from pathlib import Path
from unittest import TestCase
ROOT = Path(__file__).resolve().parents[2]
UI = ROOT / "settings/src/com/yooshastan/ultmessenger/settings/UltSettingsActivity.java"
MENU = ROOT / "scripts/integrate-message-tools.py"
MODEL = ROOT / "chat/src/com/yooshastan/ultmessenger/chat/UltChatTools.java"
STRICT = ROOT / "scripts/integrate-native-strict.py"
class CombinedContract(TestCase):
    def test_media_is_existing_storage_control_and_explicit_clipboard(self):
        u = UI.read_text()
        m = MENU.read_text()
        self.assertIn("new org.telegram.ui.CacheControlActivity()", u)
        self.assertIn("ULT: Copy file summary", m)
        self.assertIn("FileLoader.getDocumentFileName(document)", m)
        self.assertIn("selectedObject.getDocument() != null", m)
        self.assertIn("replaceAll(", MODEL.read_text())
        self.assertNotIn("downloadFile(", m)
        self.assertNotIn("sendMessage(", m)
    def test_appearance_reuses_upstream_telegram_setting(self):
        u = UI.read_text()
        self.assertIn("SharedConfig.setUseThreeLinesLayout(selected == 1)", u)
        self.assertIn("SharedConfig.useThreeLinesLayout", u)
        self.assertIn("Two-line preview (compact)", u)
        self.assertNotIn('setMessage("Uses Telegram', u)
        self.assertIn("Three-line preview (more text)", u)
    def test_audit_is_diagnostic_only_and_native_guard_is_unmodified(self):
        u = UI.read_text()
        for marker in ("STATIC CODE AUDIT", "NOT FULLY AUDITED",
                       "NOT ROUTE-VERIFIED", "DoQ backend NOT INSTALLED",
                       "No new packet capture", "phone-edge/gateway"):
            self.assertIn(marker, u)
        self.assertNotIn("startVpn(", u)
        self.assertIn("SOCKET_GUARD", STRICT.read_text())

if __name__ == "__main__":
    from unittest import main
    main()

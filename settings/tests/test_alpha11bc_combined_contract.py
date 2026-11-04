#!/usr/bin/env python3
"""Combined 0.11bc native-source integration contract (no Android build)."""
from pathlib import Path
from unittest import TestCase, main
ROOT = Path(__file__).resolve().parents[2]
UI = ROOT / "settings/src/com/yooshastan/ultmessenger/settings/UltSettingsActivity.java"
CHAT = ROOT / "scripts/integrate-chat-views.py"
MODEL = ROOT / "conversation/src/com/yooshastan/ultmessenger/conversation/UltChatView.java"
COPY = ROOT / "scripts/integrate-ult-settings.py"
POLICY = ROOT / "scripts/integrate-native-strict.py"
AUDIT = ROOT / "settings/src/com/yooshastan/ultmessenger/settings/UltNetworkSourceMatrix.java"

class Alpha11BC(TestCase):
    def test_native_folder_bar_is_kept_and_local_sorting_stays_read_only(self):
        ui = UI.read_text()
        chat = CHAT.read_text()
        model = MODEL.read_text()
        self.assertIn("showChatViewPicker(context)", ui)
        self.assertIn("showOfflineTranslationStatus(context)", ui)
        self.assertIn("showNetworkPathCoverage(context)", ui)
        self.assertIn("isUltChatViewActive()", chat)
        self.assertIn("UltChatView.compareUnreadFirst(", chat)
        self.assertIn("tabConfigKey(int account)", model)
        self.assertNotIn("installUltCategoryBar(", chat)
        self.assertNotIn("ultSwipeTarget", chat)
        self.assertIn("getString(key, UltChatView.defaultTabConfig())", ui)

    def test_audit_model_is_copied_and_contains_no_active_monitor(self):
        copy = COPY.read_text()
        matrix = AUDIT.read_text()
        self.assertIn("UltNetworkSourceMatrix.java", copy)
        self.assertIn("NO ACTIVE CAPTURE", matrix)
        self.assertIn("NOT ROUTE-VERIFIED", matrix)
        self.assertNotIn("java.net.Socket(", matrix)
        self.assertIn("SOCKET_GUARD", POLICY.read_text())

if __name__ == "__main__":
    main()

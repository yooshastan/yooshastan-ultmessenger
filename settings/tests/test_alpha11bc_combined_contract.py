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
    def test_chat_tab_configuration_is_local_and_restorable(self):
        ui = UI.read_text()
        chat = CHAT.read_text()
        model = MODEL.read_text()
        for feature in ("showTabCustomization(context)", "showTabOrder(context",
                        "showOfflineTranslationStatus(context)",
                        "showNetworkSourceMatrix(context)"):
            self.assertIn(feature, ui)
        self.assertIn("UltChatView.visibleTabs(", chat)
        self.assertIn("ultCategoryRow.removeAllViews()", chat)
        self.assertIn("UltChatView.nextVisibleTab(", chat)
        self.assertIn("tabConfigKey(int account)", model)
        self.assertIn("modes.add(ALL)", model)
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

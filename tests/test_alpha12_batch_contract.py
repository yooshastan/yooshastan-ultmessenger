#!/usr/bin/env python3
"""Alpha 0.12a+b lightweight source contracts. No native Android build."""
from pathlib import Path
from unittest import TestCase

ROOT = Path(__file__).resolve().parents[1]


class Alpha12BatchContract(TestCase):
    def test_category_bar_density_is_per_account_and_reversible(self):
        model = (ROOT / "conversation/src/com/yooshastan/ultmessenger/conversation/UltChatView.java").read_text()
        patch = (ROOT / "scripts/integrate-chat-views.py").read_text()
        settings = (ROOT / "settings/src/com/yooshastan/ultmessenger/settings/UltSettingsActivity.java").read_text()
        for expected in ("barStyleKey(int account)", "BAR_COMPACT", "BAR_COMFORTABLE",
                         "BAR_SPACIOUS", "barHeightDp(", "chipHeightDp(", "chipPaddingDp("):
            self.assertIn(expected, model)
        for expected in ("UltChatView.barHeightDp(barStyle)", "UltChatView.chipHeightDp(barStyle)",
                         "UltChatView.chipPaddingDp(barStyle)", "ultCategoryRow.removeAllViews()"):
            self.assertIn(expected, patch)
        self.assertIn("showCategoryBarAppearance(context)", settings)
        self.assertIn('prefs.edit().remove(key).apply()', settings)
        self.assertNotIn("setDialogFilter(", patch)

    def test_theme_previews_are_read_only_and_offer_both_modes(self):
        settings = (ROOT / "settings/src/com/yooshastan/ultmessenger/settings/UltSettingsActivity.java").read_text()
        self.assertIn("showUltThemePreview(context)", settings)
        self.assertIn("preset.forMode(night)", settings)
        self.assertIn("new boolean[] {false, true}", settings)
        self.assertIn("colors.incomingBubbleArgb", settings)
        self.assertIn("colors.outgoingBubbleArgb", settings)
        preview = settings[settings.index("private void showUltThemePreview("):
                           settings.index("private void showChatListDensity(")]
        self.assertNotIn("saveTheme(", preview)
        self.assertNotIn("applyTheme(", preview)
        self.assertNotIn("setUseThreeLinesLayout", preview)

    def test_explicit_metadata_copy_has_no_private_permalink_or_network_request(self):
        model = (ROOT / "chat/src/com/yooshastan/ultmessenger/chat/UltChatTools.java").read_text()
        integration = (ROOT / "scripts/integrate-message-tools.py").read_text()
        self.assertIn("messageReference(", model)
        self.assertIn("publicMessageLink(publicUsername, messageId)", model)
        self.assertIn("ULT: Copy message reference", integration)
        self.assertIn("currentEncryptedChat == null && currentChat != null", integration)
        self.assertIn("!selectedObject.isSponsored()", integration)
        self.assertIn("UltChatTools.messageReference(", integration)
        for forbidden in ("sendMessage(", "forwardMessages(", "new java.net.", "new Socket("):
            self.assertNotIn(forbidden, integration)

    def test_fast_branch_compilation_boundary(self):
        light = (ROOT / ".github/workflows/scaffold-check.yml").read_text()
        android = (ROOT / ".github/workflows/android-alpha.yml").read_text()
        self.assertIn("feature/alpha12-nullgram-nekogram", light)
        self.assertIn("test_alpha12_batch_contract.py", light)
        self.assertNotIn("./gradlew", light)
        self.assertNotIn("feature/alpha12-nullgram-nekogram", android)
        self.assertIn("python3 scripts/integrate-native-strict.py", android)


if __name__ == "__main__":
    import unittest
    unittest.main()

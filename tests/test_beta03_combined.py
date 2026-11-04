"""One beta 0.3 candidate: navigation + native search + recovery guidance."""
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]

def read(path):
    return (ROOT / path).read_text(encoding="utf-8")

class Beta03Contracts(unittest.TestCase):
    def test_native_folder_bar_unobstructed(self):
        source = read("scripts/integrate-chat-views.py")
        self.assertNotIn("installUltCategoryBar(", source)
        self.assertNotIn("ultCategoryBar =", source)
        self.assertNotIn("ultSwipeTarget", source)
        self.assertNotIn("ULT: Next Chat Tab", source)
        self.assertNotIn("ULT: Show category bar", source)
        self.assertIn("UltChatView.compareUnreadFirst(", source)
        self.assertIn("ULT: Chat Views", source)
        self.assertIn("setSingleChoiceItems(UltChatView.LABELS", source)

    def test_advanced_search_uses_native_full_filters(self):
        source = read("scripts/integrate-chat-views.py")
        self.assertIn("ULT: Advanced search", source)
        self.assertIn("showSearch(true, false, true, true)", source)
        self.assertIn("fragmentSearchFieldWatcher.toggleSearch(true)", source)
        self.assertIn("AndroidUtilities.showKeyboard", source)
        self.assertNotIn("new java.net.", source)
        self.assertNotIn("sendMessage(", source)

    def test_context_menu_is_concise_and_safe(self):
        source = read("scripts/integrate-message-tools.py")
        menu = source.split('MENU = """', 1)[1].split('HANDLERS = """', 1)[0]
        self.assertEqual(menu.count("items.add("), 2)
        self.assertIn('"ULT: Copy reference"', menu)
        self.assertIn('"ULT: Copy file info"', menu)
        self.assertNotIn("Copy message time", menu)
        self.assertNotIn("Copy metadata (JSON)", menu)
        self.assertIn("!selectedObject.messageOwner.noforwards", source)
        self.assertIn("currentEncryptedChat == null", source)
        self.assertIn("UltChatTools.messageReference(", source)
        self.assertIn("UltChatTools.mediaSummary(", source)

    def test_diagnostics_are_read_only_and_explicit(self):
        policy = read("settings/src/com/yooshastan/ultmessenger/settings/UltConnectionGuidance.java")
        privacy = read("settings/src/com/yooshastan/ultmessenger/settings/UltPrivacyLabActivity.java")
        installer = read("scripts/integrate-ult-settings.py")
        self.assertIn("UltConnectionGuidance.java", installer)
        self.assertIn("UltConnectionGuidance.label(connection)", privacy)
        self.assertIn("UltConnectionGuidance.advice(", privacy)
        self.assertIn("does not independently", policy)
        for forbidden in ("setUltStrictNativeProxyEnabled(", "java.net.Socket(", "new Socket("):
            self.assertNotIn(forbidden, policy + privacy)

    def test_stable_signing_and_beta_version_are_distinct(self):
        workflow = read(".github/workflows/android-beta.yml")
        self.assertIn("16.0.0-beta03.2", workflow)
        self.assertIn("7183014", workflow)
        self.assertIn("ult-beta03-2-foss-arm64-debug.apk", workflow)
        self.assertIn("scripts/integrate-native-strict.py", workflow)
        self.assertIn("scripts/integrate-translation.py", workflow)
        self.assertIn("scripts/apk_release_gate.py", workflow)
        self.assertIn("feature/beta01-navigation-message-polish", workflow)

if __name__ == "__main__":
    unittest.main()

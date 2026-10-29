from pathlib import Path
from unittest import TestCase
R=Path(__file__).resolve().parents[1]
def read(p):return (R/p).read_text(encoding="utf-8")
class UnifiedFeatureBatch(TestCase):
    def test_user_selected_verification_is_guarded(self):
        s=read("settings/src/com/yooshastan/ultmessenger/settings/UltDownloadsActivity.java")
        for x in ("Verify saved copy…", "Intent.ACTION_OPEN_DOCUMENT", "VERIFY_DOCUMENT_REQUEST",
                  "clearPendingVerify()", "isExportAllowed(message, account)",
                  "account != currentAccount", "UltFileInspection.sameContent(original, copy)",
                  "openInputStream(selectedUri)", "source.length() != expected",
                  "new Thread(", "showSecureDialog(", "FLAG_GRANT_READ_URI_PERMISSION"):
            self.assertIn(x,s)
        self.assertNotIn("new java.net.",s)
        self.assertNotIn("sendMessage(",s)
    def test_native_appearance_preferences_not_fake_preview(self):
        s=read("settings/src/com/yooshastan/ultmessenger/settings/UltSettingsActivity.java")
        for x in ("Message font and bubble corners", "SharedConfig.fontSize = f",
                  'putInt("fons_size", f)', "SharedConfig.bubbleRadius = r",
                  'putInt("bubbleRadius", r)', "Theme.createCommonMessageResources()",
                  "rebuildAllFragmentViews(true, true)", "UltChatAppearancePolicy.font("):
            self.assertIn(x,s)
    def test_local_privacy_and_manual_evidence_are_honest(self):
        s=read("settings/src/com/yooshastan/ultmessenger/settings/UltSettingsActivity.java")
        for x in ("Local account identity cover", "UltIdentityCover.summary(",
                  "Manual network egress evidence", "UltNetworkEvidence.render(",
                  "does not hide your identity from Telegram",
                  "No path is independently route-verified"):
            self.assertIn(x,s)
        evidence=read("settings/src/com/yooshastan/ultmessenger/settings/UltNetworkEvidence.java")
        self.assertIn("NOT ROUTE-VERIFIED",evidence)
        self.assertNotIn("new java.net.Socket(",s+evidence)
    def test_all_sources_copy_and_one_release_build(self):
        install=read("scripts/integrate-ult-settings.py")
        for name in ("UltChatAppearancePolicy.java","UltIdentityCover.java","UltNetworkEvidence.java"):
            self.assertIn(name,install)
        fast=read(".github/workflows/scaffold-check.yml")
        full=read(".github/workflows/android-alpha.yml")
        self.assertIn("feature/alpha14def-unified-media-appearance-privacy",fast)
        self.assertNotIn("feature/alpha14def-unified-media-appearance-privacy",full)

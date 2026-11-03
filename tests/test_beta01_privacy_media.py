"""Privacy Lab + Media Studio internal beta candidate contracts."""
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]

def read(file):
    return (ROOT / file).read_text(encoding="utf-8")

class Beta01Contract(unittest.TestCase):
    def test_screens_are_installed_and_routed(self):
        integrate = read("scripts/integrate-ult-settings.py")
        settings = read("settings/src/com/yooshastan/ultmessenger/settings/UltSettingsActivity.java")
        downloads = read("settings/src/com/yooshastan/ultmessenger/settings/UltDownloadsActivity.java")
        for name in ("UltPrivacyLabActivity", "UltPrivacySession",
                     "UltMediaStudioActivity", "UltMediaIndex"):
            self.assertIn(f"{name}.java", integrate)
        self.assertIn("new UltPrivacyLabActivity()", settings)
        self.assertIn("new UltMediaStudioActivity()", settings)
        self.assertIn("new UltMediaStudioActivity()", downloads)

    def test_privacy_is_an_observer_not_proxy_rewrite(self):
        privacy = read("settings/src/com/yooshastan/ultmessenger/settings/UltPrivacyLabActivity.java")
        model = read("settings/src/com/yooshastan/ultmessenger/settings/UltPrivacySession.java")
        self.assertIn("native_isStrictProxyModeEnabled(currentAccount)", privacy)
        self.assertIn("getConnectionState()", privacy)
        self.assertIn("TRANSPORT_VPN", privacy)
        self.assertIn("onPause()", privacy)
        self.assertIn("handler.removeCallbacks(sampler)", privacy)
        self.assertIn("MAX_EVENTS = 40", model)
        self.assertIn("NOT packet-route", model)
        self.assertNotIn("proxyAddress", model + privacy)
        self.assertNotIn("setUltStrictNativeProxyEnabled(", privacy)

    def test_media_index_only_native_allowed_cached_items(self):
        media = read("settings/src/com/yooshastan/ultmessenger/settings/UltMediaStudioActivity.java")
        model = read("settings/src/com/yooshastan/ultmessenger/settings/UltMediaIndex.java")
        self.assertIn("recentDownloadingFiles", media)
        self.assertIn("!file.isFile() || !file.canRead()", media)
        self.assertIn("message.messageOwner.noforwards", media)
        self.assertIn("chat == null || chat.noforwards", media)
        self.assertIn("DialogObject.isEncryptedDialog", media)
        self.assertIn("UltMediaIndex.filter(", media)
        self.assertIn("Kind.PHOTO", model)
        self.assertIn("Kind.VIDEO", model)
        self.assertIn("Kind.AUDIO", model)
        self.assertNotIn("MANAGE_EXTERNAL_STORAGE", media)
        self.assertNotIn("getExternalStorageDirectory", media)
        self.assertNotIn("delete(", media)
        self.assertNotIn("loadFile(", media)

    def test_existing_strict_translation_unmodified(self):
        workflow = read(".github/workflows/android-beta.yml")
        self.assertIn("scripts/integrate-native-strict.py", workflow)
        self.assertIn("scripts/integrate-translation.py", workflow)
        self.assertIn("feature/beta01-privacy-media", workflow)
        self.assertIn("7183011", workflow)
        self.assertIn("16.0.0-beta01.1", workflow)

if __name__ == "__main__":
    unittest.main()

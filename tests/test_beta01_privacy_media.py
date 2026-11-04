"""Quality-first beta: one download home, honest network diagnostics."""
import re
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]

def read(path):
    return (ROOT / path).read_text(encoding="utf-8")

class Beta01QualityContract(unittest.TestCase):
    def test_main_screen_is_concise_and_single_home(self):
        settings = read("settings/src/com/yooshastan/ultmessenger/settings/UltSettingsActivity.java")
        main = settings.split("addSection(context, content, \"ULTGRAM\");", 1)[1].split(
            "fragmentView = scroll;", 1)[0]
        self.assertEqual(main.count("addLink(context, content,"), 11)
        self.assertEqual(main.count('"Downloads",'), 1)
        self.assertNotIn('new UltMediaStudioActivity()', main)
        for label in ('"Proxy manager"', '"Strict proxy (native sockets)"',
                      '"Screenshot protection"', '"External link confirmation"',
                      '"Local identity cover"', '"Network diagnostics"'):
            self.assertIn(label, main)
        for unfinished in ('"DNS-over-QUIC configuration (preview)"',
                           '"Tracker domain rules (preview)"',
                           '"Network privacy dashboard"', '"Network test checklist"'):
            self.assertNotIn(unfinished, main)

    def test_one_native_download_manager_without_studio_loop(self):
        downloads = read("settings/src/com/yooshastan/ultmessenger/settings/UltDownloadsActivity.java")
        integrate = read("scripts/integrate-ult-settings.py")
        self.assertIn("new SearchDownloadsContainer(this, currentAccount)", downloads)
        self.assertIn("UltDownloadActions.pauseAll(currentAccount)", downloads)
        self.assertIn("UltDownloadActions.resumeNext(currentAccount)", downloads)
        self.assertIn("UltDownloadActions.resumeAll(currentAccount)", downloads)
        self.assertIn("showMoreDownloadsActions()", downloads)
        self.assertIn("showQueueStatus()", downloads)
        self.assertIn("confirmClearHistory()", downloads)
        self.assertIn("lastSaveKey(currentAccount)", downloads)
        self.assertNotIn("new UltMediaStudioActivity()", downloads)
        self.assertNotIn("UltMediaStudioActivity.java", integrate)
        self.assertNotIn("UltMediaIndex.java", integrate)
        self.assertNotIn("MANAGE_EXTERNAL_STORAGE", downloads)

    def test_privacy_diagnostics_are_read_only(self):
        privacy = read("settings/src/com/yooshastan/ultmessenger/settings/UltPrivacyLabActivity.java")
        model = read("settings/src/com/yooshastan/ultmessenger/settings/UltPrivacySession.java")
        settings = read("settings/src/com/yooshastan/ultmessenger/settings/UltSettingsActivity.java")
        self.assertIn("new UltPrivacyLabActivity()", settings)
        self.assertIn("native_isStrictProxyModeEnabled(currentAccount)", privacy)
        self.assertIn("getConnectionState()", privacy)
        self.assertIn("TRANSPORT_VPN", privacy)
        self.assertIn("handler.removeCallbacks(sampler)", privacy)
        self.assertIn("MAX_EVENTS = 40", model)
        self.assertIn("NOT packet-route", model)
        self.assertNotIn("proxyAddress", model + privacy)
        self.assertNotIn("setUltStrictNativeProxyEnabled(", privacy)

    def test_existing_strict_translation_build_chain_unmodified(self):
        beta = read(".github/workflows/android-beta.yml")
        alpha = read(".github/workflows/android-alpha.yml")
        self.assertIn("scripts/integrate-native-strict.py", beta)
        self.assertIn("scripts/integrate-translation.py", beta)
        self.assertIn("scripts/integrate-native-strict.py", alpha)
        self.assertIn("scripts/integrate-translation.py", alpha)
        self.assertIn("7183014", beta)
        self.assertIn("16.0.0-beta03.2", beta)
        self.assertIn("feature/beta01-quality-trim", beta)

if __name__ == "__main__":
    unittest.main()

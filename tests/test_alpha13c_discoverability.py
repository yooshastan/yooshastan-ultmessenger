from pathlib import Path
from unittest import TestCase

ROOT = Path(__file__).resolve().parents[1]

class Discoverability(TestCase):
    def test_changelog_is_visible_and_honest(self):
        s = (ROOT / "settings/src/com/yooshastan/ultmessenger/settings/UltSettingsActivity.java").read_text()
        self.assertIn('addSection(context, content, "START HERE")', s)
        self.assertIn('"What\'s new in Ultgram 0.13"', s)
        self.assertIn('() -> showUltWhatsNew(context)', s)
        self.assertIn('"Downloads — open queue"', s)
        self.assertIn('() -> presentFragment(new UltDownloadsActivity())', s)
        self.assertIn('getPackageInfo(context.getPackageName(), 0)', s)
        self.assertIn('getLongVersionCode()', s)
        self.assertIn('not a new downloader', s)
        self.assertIn('COMING LATER', s)
        self.assertIn('No identity spoofing', s)

    def test_no_fake_network_feature(self):
        s = (ROOT / "settings/src/com/yooshastan/ultmessenger/settings/UltSettingsActivity.java").read_text()
        self.assertIn('tracker filtering or new enforced DNS protection', s)
        self.assertNotIn('new java.net.Socket(', s)

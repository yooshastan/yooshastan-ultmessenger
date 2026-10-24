import unittest

from scripts.patch_locale_background import ANCHOR, BLOCK, SINGLE, DISPATCH, patch_source


class LocaleBackgroundPatchTests(unittest.TestCase):
    def make_source(self):
        return "\n".join([ANCHOR, SINGLE, BLOCK, BLOCK, BLOCK])

    def test_all_four_callbacks_stop_parsing_on_ui_thread(self):
        output = patch_source(self.make_source())
        self.assertEqual(4, output.count(DISPATCH))
        self.assertIn("Utilities.globalQueue.postRunnable", output)
        self.assertIn("AndroidUtilities.runOnUIThread(() -> saveRemoteLocaleStrings(localeInfo, difference", output)
        self.assertNotIn(SINGLE, output)
        self.assertNotIn(BLOCK, output)

    def test_upstream_drift_fails_closed(self):
        with self.assertRaisesRegex(ValueError, "anchors changed"):
            patch_source(self.make_source().replace(BLOCK, "", 1))


if __name__ == "__main__":
    unittest.main()

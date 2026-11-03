"""Alpha 0.15 download queue UI wiring regression checks.

These are fast source contracts; native Android compilation and device tests are
still required before an APK is considered ready.
"""
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
ACTIONS = (ROOT / "settings/src/com/yooshastan/ultmessenger/settings/UltDownloadActions.java").read_text()
SCREEN = (ROOT / "settings/src/com/yooshastan/ultmessenger/settings/UltDownloadsActivity.java").read_text()


class DownloadQueueContract(unittest.TestCase):
    def test_resume_next_resumes_only_one(self):
        method = ACTIONS.split("public static boolean resumeNext(int account)", 1)[1].split(
            "public static int[] queueCounts", 1
        )[0]
        self.assertIn("for (MessageObject message : queue(account))", method)
        self.assertIn("!loader.isLoadingFile(message.getFileName())", method)
        self.assertIn("loader.loadFile(document, message, FileLoader.PRIORITY_LOW, 0)", method)
        self.assertIn("return true;", method)
        self.assertIn("return false;", method)
        self.assertNotIn("resumeAll(", method)

    def test_status_is_per_account_and_metadata_only(self):
        method = ACTIONS.split("public static int[] queueCounts(int account)", 1)[1].split(
            "public static boolean pauseOne", 1
        )[0]
        self.assertIn("queue(account)", method)
        self.assertIn("recentCount(account)", method)
        self.assertIn("return new int[]{running, paused, recentCount(account)};", method)
        self.assertNotIn("getPathToMessage", method)

    def test_ui_exposes_controls(self):
        self.assertIn('"Resume next"', SCREEN)
        self.assertIn("UltDownloadActions.resumeNext(currentAccount)", SCREEN)
        self.assertIn('"Queue status"', SCREEN)
        self.assertIn("UltDownloadActions.queueCounts(currentAccount)", SCREEN)
        self.assertIn("showSecureDialog(activity,", SCREEN)


if __name__ == "__main__":
    unittest.main()

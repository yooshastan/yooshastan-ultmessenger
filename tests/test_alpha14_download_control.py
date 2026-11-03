"""Download control center safety, native queue and file-history contracts."""
from pathlib import Path
from unittest import TestCase
ROOT=Path(__file__).resolve().parents[1]
def src(p): return (ROOT/p).read_text(encoding="utf-8")

class DownloadCenter(TestCase):
    def test_visible_native_queue_actions(self):
        ui=src("settings/src/com/yooshastan/ultmessenger/settings/UltDownloadsActivity.java")
        for value in ("Pause all", "Resume all", "Network rules", "Clear history",
                      "Pause this download", "Resume this download", "Move to top of queue",
                      "ULT download controls", "UltDownloadActions.isQueued(",
                      "UltDownloadActions.moveToTop(", "setItems(labels.toArray",
                      "DataAutoDownloadActivity(choice)"):
            self.assertIn(value,ui)
        self.assertIn('This only removes history entries. It does NOT delete',ui)
        self.assertIn("Clear recent download history?",ui)
        self.assertNotIn("deleteRecentFiles(",ui)
        self.assertNotIn("clearRecentDownloadedFiles(",ui)
        self.assertNotIn("loadFile(",ui)  # old inspect/export safety contract
    def test_native_loader_and_no_disk_delete(self):
        actions=src("settings/src/com/yooshastan/ultmessenger/settings/UltDownloadActions.java")
        for value in ("DownloadController.getInstance(account).downloadingFiles",
                      "FileLoader.getInstance(account)", "loader.cancelLoadFile(document)",
                      "loader.loadFile(document, message, FileLoader.PRIORITY_LOW, 0)",
                      "controller.swapLoadingPriority(", "updateFilesLoadingPriority()",
                      "clearRecentDownloadedFiles()"):
            self.assertIn(value,actions)
        for dangerous in ("new java.net.", "File.delete(", "deleteRecursively(", "new Thread(",
                          "deleteRecentFiles(message", "MANAGE_EXTERNAL_STORAGE"):
            self.assertNotIn(dangerous,actions)
    def test_install_and_source_scope(self):
        installer=src("scripts/integrate-ult-settings.py")
        self.assertIn("UltDownloadActions.java",installer)
        full=src(".github/workflows/android-alpha.yml")
        self.assertNotIn("feature/alpha14-download-control-center",full)
        settings=src("settings/src/com/yooshastan/ultmessenger/settings/UltSettingsActivity.java")
        self.assertIn("One queue, history and file tools",settings)

"""Verify strict, idempotent integration on pinned source-shaped fixtures."""
import importlib.util
import tempfile
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
SCRIPT = ROOT / "scripts/integrate-video-links.py"
spec = importlib.util.spec_from_file_location("ult_video_integration", SCRIPT)
module = importlib.util.module_from_spec(spec)
spec.loader.exec_module(module)

class IntegrationTest(unittest.TestCase):
    def test_pinned_hooks_and_models(self):
        with tempfile.TemporaryDirectory() as temp:
            root = Path(temp)
            app, ui, strict = root / module.APP, root / module.DOWNLOADS, root / module.STRICT
            gradle, manifest = root / module.GRADLE, root / module.MANIFEST
            gradle.parent.mkdir(parents=True, exist_ok=True)
            gradle.write_text("dependencies {\n" + module.GRADLE_ANCHOR + "}\n")
            manifest.parent.mkdir(parents=True, exist_ok=True)
            manifest.write_text("<manifest>" + module.MANIFEST_ANCHOR + "</application></manifest>")
            strict.parent.mkdir(parents=True, exist_ok=True)
            strict.write_text("before\n" + module.STRICT_ANCHOR + "after\n")
            app.parent.mkdir(parents=True, exist_ok=True)
            ui.parent.mkdir(parents=True, exist_ok=True)
            app.write_text("before\n" + module.APP_ANCHOR + "after\n")
            ui.write_text("before\n" + module.TOOLBAR_ANCHOR + module.METHOD_ANCHOR + "after\n")
            module.integrate(root)
            self.assertIn("UltVideoAutoSave.install()", app.read_text())
            self.assertIn("showVideoLinkSettings", ui.read_text())
            self.assertIn("Video links", ui.read_text())
            self.assertIn("cancelPendingForStrict", strict.read_text())
            self.assertIn("youtubedl-android:library:0.18.1", gradle.read_text())
            self.assertIn("extractNativeLibs", manifest.read_text())
            for name in ("UltVideoAutoSave.java", "UltVideoLinkRules.java", "UltVideoSiteDownloader.java"):
                self.assertTrue((root / module.DEST / name).exists())
            with self.assertRaises(ValueError):
                module.integrate(root)

    def test_fail_closed_when_pinned_source_changes(self):
        with tempfile.TemporaryDirectory() as temp:
            root = Path(temp)
            app, ui, strict = root / module.APP, root / module.DOWNLOADS, root / module.STRICT
            gradle, manifest = root / module.GRADLE, root / module.MANIFEST
            gradle.parent.mkdir(parents=True, exist_ok=True)
            gradle.write_text("dependencies {\n" + module.GRADLE_ANCHOR + "}\n")
            manifest.parent.mkdir(parents=True, exist_ok=True)
            manifest.write_text("<manifest>" + module.MANIFEST_ANCHOR + "</application></manifest>")
            strict.parent.mkdir(parents=True, exist_ok=True)
            strict.write_text("before\n" + module.STRICT_ANCHOR + "after\n")
            app.parent.mkdir(parents=True, exist_ok=True)
            ui.parent.mkdir(parents=True, exist_ok=True)
            app.write_text(module.APP_ANCHOR)
            ui.write_text("upstream changed")
            with self.assertRaises(ValueError):
                module.integrate(root)
            self.assertEqual(app.read_text(), module.APP_ANCHOR)

    def test_no_telegram_proxy_bypass_in_strict_mode(self):
        worker = (ROOT / "video/src/com/yooshastan/ultmessenger/video/UltVideoAutoSave.java").read_text()
        self.assertIn('getBoolean("ult_strict_native_proxy", false)', worker)
        self.assertIn("strictModeRequested()", worker)
        self.assertIn("message.messageOwner.noforwards", worker)
        self.assertIn("DialogObject.isEncryptedDialog", worker)
        self.assertIn("DownloadManager.Request", worker)
        self.assertIn("NotificationCenter.messageReceivedByServer", worker)
        self.assertIn("queueLinks(dialogId, sent)", worker)
        self.assertIn("cancelPendingForStrict()", worker)
        self.assertNotIn("java.net.Socket(", worker)

if __name__ == "__main__":
    unittest.main()

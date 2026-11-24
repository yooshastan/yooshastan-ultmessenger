"""Verify strict, idempotent video integration and new direct-settings contracts."""
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
    def prepare(self, root):
        app, strict = root / module.APP, root / module.STRICT
        gradle, manifest = root / module.GRADLE, root / module.MANIFEST
        for path in (app, strict, gradle, manifest):
            path.parent.mkdir(parents=True, exist_ok=True)
        app.write_text("before\n" + module.APP_ANCHOR + "after\n")
        strict.write_text("before\n" + module.STRICT_ANCHOR + "after\n")
        gradle.write_text("dependencies {\n" + module.GRADLE_ANCHOR + "}\n")
        manifest.write_text("<manifest>" + module.MANIFEST_ANCHOR + "</application></manifest>")
        return app, strict, gradle, manifest

    def test_pinned_hooks_and_models(self):
        with tempfile.TemporaryDirectory() as temp:
            root = Path(temp)
            app, strict, gradle, manifest = self.prepare(root)
            module.integrate(root)
            self.assertIn("UltVideoAutoSave.install()", app.read_text())
            self.assertIn("cancelPendingForStrict", strict.read_text())
            self.assertIn("youtubedl-android:library:0.18.1", gradle.read_text())
            self.assertIn("extractNativeLibs", manifest.read_text())
            for name in ("UltVideoAutoSave.java", "UltVideoLinkRules.java",
                         "UltVideoSiteDownloader.java", "UltVideoChatPost.java",
                         "UltVideoFailure.java"):
                self.assertTrue((root / module.DEST / name).exists(), name)
            with self.assertRaises(ValueError):
                module.integrate(root)

    def test_fail_closed_when_pinned_source_changes(self):
        with tempfile.TemporaryDirectory() as temp:
            root = Path(temp)
            app, strict, _, _ = self.prepare(root)
            strict.write_text("upstream changed")
            before = app.read_bytes()
            with self.assertRaises(ValueError):
                module.integrate(root)
            self.assertEqual(app.read_bytes(), before)

    def test_outgoing_is_distinct_and_strict_mode_blocks(self):
        worker = (ROOT / "video/src/com/yooshastan/ultmessenger/video/UltVideoAutoSave.java").read_text()
        site = (ROOT / "video/src/com/yooshastan/ultmessenger/video/UltVideoSiteDownloader.java").read_text()
        chat = (ROOT / "video/src/com/yooshastan/ultmessenger/video/UltVideoChatPost.java").read_text()
        self.assertIn('getBoolean("ult_strict_native_proxy", false)', worker)
        self.assertIn("message.messageOwner.noforwards", worker)
        self.assertIn("DialogObject.isEncryptedDialog", worker)
        self.assertIn("DownloadManager.Request", worker)
        self.assertIn("queueLinks(dialogId, sent, true)", worker)
        self.assertIn("queueLinks(dialogId, message.messageOwner, false)", worker)
        self.assertIn("isPostToChat(int account)", worker)
        self.assertIn("noteAttempt(account, url)", worker)
        self.assertIn("completedSiteSaves(int account)", worker)
        self.assertIn("lastSourceCategory(int account)", worker)
        self.assertIn("Executors.newSingleThreadExecutor()", worker)
        self.assertIn("sourceCategory(String url)",
            (ROOT / "video/src/com/yooshastan/ultmessenger/video/UltVideoLinkRules.java").read_text())
        self.assertIn("outgoingLink && UltVideoAutoSave.isPostToChat(account)", site)
        self.assertIn("UltVideoChatPost.enqueue(", site)
        self.assertIn("SendMessagesHelper.prepareSendingVideo(", chat)
        self.assertIn("!UltVideoAutoSave.canAutoPost(account)", chat)
        self.assertIn("DialogObject.isChatDialog(dialogId)", chat)
        self.assertIn("ChatObject.getSendAsPeerId(", chat)
        self.assertIn("MessageObject.getPeerId(original.from_id)", chat)
        self.assertIn("originalSender == activeSender", chat)
        self.assertIn("chat.forum || chat.monoforum", chat)
        self.assertIn("ChatObject.canSendVideo(chat)", chat)
        self.assertIn("getSendPaidMessagesStars(dialogId)", chat)
        self.assertIn("new MessageObject(account, original, false, false)", chat)
        self.assertIn("UltVideoChatPost.safeDestination(account, dialogId, original)", site)
        self.assertNotIn("auto-post paused for group/anonymous sender safety", site)
        self.assertIn("UltVideoFailure.publicReason(", site)
        self.assertIn("MAX_EXTRACTION_MS = TimeUnit.MINUTES.toMillis(4)", site)
        self.assertIn("NO_PROGRESS_MS = TimeUnit.SECONDS.toMillis(90)", site)
        self.assertIn("SystemClock.elapsedRealtime()", site)
        self.assertIn("stopReason.compareAndSet(null, reason)", site)
        self.assertIn("destroyProcessById(processId)", site)
        self.assertIn("lastProgress.set(SystemClock.elapsedRealtime())", site)
        self.assertIn("progress.longValue() / 10L", site)
        self.assertNotIn("(long) progress", site)
        self.assertIn('Extractor stalled for 90 seconds; no media progress', site)
        self.assertIn('Extractor timed out after 4 minutes; video not saved', site)
        self.assertNotIn('UltVideoAutoSave.noteResult(account, line)', site)
        self.assertNotIn("java.net.Socket(", worker)

if __name__ == "__main__":
    unittest.main()

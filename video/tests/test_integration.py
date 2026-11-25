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
        chat = root / module.CHAT
        chat.parent.mkdir(parents=True, exist_ok=True)
        chat.write_text(module.CHAT_CONSTANT_ANCHOR + module.CHAT_MENU_ANCHOR + module.CHAT_HANDLER_ANCHOR)
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
            chat = (root / module.CHAT).read_text()
            for label in ("ULT: Add video to chat", "ULT: Add audio to chat", "ULT: Add media with options", "ULT: Save to device", "ULT: Copy sanitized link"):
                self.assertIn(label, chat)
            self.assertIn("currentChat.noforwards", chat)
            for name in ("UltVideoAutoSave.java", "UltVideoLinkRules.java",
                         "UltVideoSiteDownloader.java", "UltVideoChatPost.java",
                         "UltVideoFailure.java", "UltExtractorPolicy.java", "UltExtractorMaintenance.java",
                         "UltMediaOptions.java", "UltManualMediaMenu.java", "UltChatMediaDraft.java"):
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

    def test_site_controls_and_confirmed_history(self):
        base = ROOT / "video/src/com/yooshastan/ultmessenger/video"
        worker = (base / "UltVideoAutoSave.java").read_text()
        site = (base / "UltVideoSiteDownloader.java").read_text()
        ui = (ROOT / "settings/src/com/yooshastan/ultmessenger/settings/UltVideoLinksSettingsActivity.java").read_text()
        self.assertIn("siteLimitMiB(account)", site)
        self.assertIn("totalBytes(work, maxBytes) > maxBytes", site)
        self.assertIn("stopped.get() || !allowed(account, options, ownerId, requestCancelled) || copied + n > maxBytes", site)
        self.assertIn("cancelSiteJobs(currentAccount)", ui)
        self.assertIn('versionRequest.addOption("--version")', site)
        self.assertIn(".getOut().trim()", site)
        self.assertNotIn("versionName(context)", site)
        self.assertIn('request.addOption("--ignore-config")', site)
        self.assertIn("extractionUrl(url)", site)
        self.assertLess(site.index("resolver.update(dest, visible"), site.index("recordSiteSave(account"))
        self.assertIn('row.put("uri", uri.toString())', worker)
        self.assertNotIn('row.put("url"', worker)
        self.assertIn("clearSiteHistory(currentAccount)", ui)
        self.assertIn("statusHandler.removeCallbacks(statusTick)", ui)

    def test_network_cancellation_reasons(self):
        base = ROOT / "video/src/com/yooshastan/ultmessenger/video"
        worker = (base / "UltVideoAutoSave.java").read_text()
        site = (base / "UltVideoSiteDownloader.java").read_text()
        for reason in ("Network disconnected", "Network metered; unmetered-only enabled",
                       "Native strict proxy enabled", "Automatic downloads disabled"):
            self.assertIn(reason, worker)
        self.assertIn("return networkBlockReason(account) == null", worker)
        self.assertIn("return UltVideoAutoSave.requestBlockReason(account, options.manual, ownerId, requestCancelled) == null", site)
        self.assertIn("Download stopped: cancelled by user", site)
        self.assertIn("STOP_REASONS.remove(processId)", site)
        self.assertNotIn("network or strict-proxy protection changed", site)

    def test_maintenance_uses_shared_download_lock_and_strict_policy(self):
        base = ROOT / "video/src/com/yooshastan/ultmessenger/video"
        updater = (base / "UltExtractorMaintenance.java").read_text()
        site = (base / "UltVideoSiteDownloader.java").read_text()
        worker = (base / "UltVideoAutoSave.java").read_text()
        self.assertIn("UltExtractorMaintenance.beginDownload()", site)
        self.assertIn("UltExtractorMaintenance.endDownload()", site)
        self.assertIn("UltExtractorMaintenance.cancelForStrict()", worker)
        self.assertIn("LOCK.tryLock()", updater)
        self.assertIn("strictModeRequested()", updater)
        self.assertIn("UltExtractorPolicy.checksum(", updater)
        self.assertLess(updater.index("expected.equals(UltExtractorPolicy.sha256(staging))"), updater.index("probe(c, staging, output)"))
        self.assertLess(updater.index("probe(c, staging, output)"), updater.index("UltExtractorPolicy.replace(staging"))
        self.assertNotIn("updateYoutubeDL(", updater)

    def test_media_controls_and_offline_version(self):
        base = ROOT / "video/src/com/yooshastan/ultmessenger/video"
        updater = (base / "UltExtractorMaintenance.java").read_text()
        site = (base / "UltVideoSiteDownloader.java").read_text()
        worker = (base / "UltVideoAutoSave.java").read_text()
        ui = (ROOT / "settings/src/com/yooshastan/ultmessenger/settings/UltVideoLinksSettingsActivity.java").read_text()
        self.assertIn('start(c, "inspect")', updater)
        self.assertIn('"inspect".equals(action)', updater)
        self.assertIn('remote = "check".equals(action) || "update".equals(action)', updater)
        self.assertIn('request.addOption("--audio-format", options.audioFormat)', site)
        self.assertIn('!audioOnly && outgoingLink', site)
        self.assertIn('String mime = options.mime()', site)
        self.assertIn('ticket.key, 0L, null, false, ticket.options, ticket.ownerId', worker)
        self.assertIn('ticket.ownerId != UserConfig.getInstance(account).getClientUserId()', worker)
        self.assertIn('15L * 60 * 1000', worker)
        self.assertIn('!metadataProbeEnabled(account)) continue', worker)
        self.assertIn('if (siteBlockReason(account) != null) return null', worker)
        self.assertIn('String[] names = {"Manager", "Preferences", "Automation", "Extractor", "History"}', ui)

    def test_manual_media_is_local_and_uses_shared_queue(self):
        base = ROOT / "video/src/com/yooshastan/ultmessenger/video"
        worker = (base / "UltVideoAutoSave.java").read_text()
        site = (base / "UltVideoSiteDownloader.java").read_text()
        menu = (base / "UltManualMediaMenu.java").read_text()
        self.assertIn("!manual && !isEnabled(account)", worker)
        self.assertIn("if (strictModeRequested())", worker)
        self.assertIn("SITE_JOBS.size() >= MAX_SITE_JOBS", worker)
        self.assertIn("IN_FLIGHT.add(key)", worker)
        self.assertIn("0L, null, false, options, ownerId", worker)
        self.assertIn("!options.manual && !audioOnly && outgoingLink", site)
        self.assertIn("message.messageOwner.noforwards", menu)
        self.assertIn("DialogObject.isEncryptedDialog", menu)
        self.assertIn("AndroidUtilities.addToClipboard(UltLinkSanitizerPolicy.cleanUrl(url))", menu)
        self.assertNotIn("setEnabled(", menu)
        self.assertNotIn("setMaxVideoHeight(", menu)

    def test_chat_attachment_handoff_requires_review(self):
        base = ROOT / "video/src/com/yooshastan/ultmessenger/video"
        draft = (base / "UltChatMediaDraft.java").read_text()
        site = (base / "UltVideoSiteDownloader.java").read_text()
        worker = (base / "UltVideoAutoSave.java").read_text()
        menu = (base / "UltManualMediaMenu.java").read_text()
        self.assertIn('if (options.toChat)', site)
        self.assertLess(site.index("UltChatMediaDraft.stage("), site.index("if (!publish("))
        self.assertIn("queueManualChat(target, url, options)", menu)
        self.assertIn("queueManualSite(account", menu)
        self.assertIn("if (!options.toChat) markSiteCompleted", worker)
        self.assertIn("!options.toChat && !job.cancelled.get()", worker)
        self.assertIn('setPositiveButton("Send", null)', draft)
        self.assertLess(draft.index("getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener"), draft.index("SendMessagesHelper.prepareSendingDocument("))
        self.assertIn("SendMessagesHelper.prepareSendingVideo(", draft)
        self.assertIn("t.sender != t.owner", draft)
        self.assertIn("chat.noforwards || chat.forum || chat.monoforum", draft)
        self.assertIn("getSendPaidMessagesStars(t.dialog)", draft)
        self.assertIn("getLastFragment() == fragment", draft)
        self.assertNotIn("MediaStore", draft)
        self.assertIn("READY.remove(t.key, draft)", draft)

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

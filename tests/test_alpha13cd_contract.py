#!/usr/bin/env python3
"""Source contracts for Alpha 0.13c+d; not a substitute for full Android build."""
from pathlib import Path
from unittest import TestCase
ROOT = Path(__file__).resolve().parents[1]
def source(path):
    return (ROOT / path).read_text(encoding="utf-8")

class Alpha13CD(TestCase):
    def test_real_version_and_discoverability(self):
        settings = source("settings/src/com/yooshastan/ultmessenger/settings/UltSettingsActivity.java")
        notes = source("settings/src/com/yooshastan/ultmessenger/settings/UltReleaseNotes.java")
        chats = source("scripts/integrate-chat-views.py")
        installer = source("scripts/integrate-ult-settings.py")
        for needle in ("UltReleaseNotes.seenKey", "getPackageInfo(",
                       "getLongVersionCode()", "showUltWhatsNew(",
                       "What's New / build information", "new UltDownloadsActivity()"):
            self.assertIn(needle, settings)
        self.assertIn("ULT: Downloads", chats)
        self.assertIn("UltDownloadsActivity()", chats)
        self.assertIn("UltReleaseNotes.java", installer)
        self.assertIn("0.13c+d", notes)
    def test_save_as_is_explicit_saf_and_fail_closed(self):
        downloads = source("settings/src/com/yooshastan/ultmessenger/settings/UltDownloadsActivity.java")
        for needle in ("Intent.ACTION_CREATE_DOCUMENT", "Intent.CATEGORY_OPENABLE",
                       "UltScreenPrivacy.isEnabled(", "DialogObject.isEncryptedDialog(",
                       "message.messageOwner.noforwards", "chat.noforwards",
                       "source.isFile()", "FileLoader.getInstance(currentAccount).getPathToMessage(",
                       "getContentResolver()", "FileInputStream(source)",
                       "new Thread(", "onActivityResultFragment", "CREATE_DOCUMENT_REQUEST"):
            self.assertIn(needle, downloads)
        for forbidden in ("READ_EXTERNAL_STORAGE", "MANAGE_EXTERNAL_STORAGE",
                          "sendMessage(", "new java.net.Socket(", "loadFile("):
            self.assertNotIn(forbidden, downloads)

    def test_apk_metadata_shell_quoting(self):
        # The 0.13c+d Gradle build previously succeeded but artifact staging
        # failed because the apostrophe in What's New broke a single-quoted printf.
        ci = source(".github/workflows/android-alpha.yml")
        self.assertIn('printf \'%s\\n\' "Candidate: Ultgram Alpha 0.14c visible eligible story', ci)
        self.assertNotIn("printf 'Candidate: Ultgram Alpha 0.13c+d What's New", ci)
        self.assertIn("ult-alpha14c-combined-foss-arm64-debug.apk", ci)
        self.assertIn("SHA256SUMS", ci)
        self.assertIn("BUILD_INFO.txt", ci)

    def test_no_full_android_build_on_feature_push(self):
        ci = source(".github/workflows/scaffold-check.yml")
        full = source(".github/workflows/android-alpha.yml")
        self.assertIn("feature/alpha13cd-discoverability-saveas", ci)
        self.assertIn("UltReleaseNotesTest", ci)
        self.assertNotIn("feature/alpha13cd-discoverability-saveas", full)

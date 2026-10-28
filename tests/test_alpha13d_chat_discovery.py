#!/usr/bin/env python3
"""Source contracts for safe, easy-to-find main Chats release navigation."""
from pathlib import Path
from unittest import TestCase
ROOT=Path(__file__).resolve().parents[1]

def read(path): return (ROOT/path).read_text(encoding="utf-8")

class ChatDiscovery(TestCase):
    def test_native_chats_integration_and_replay(self):
        patch=read("scripts/integrate-chat-views.py")
        for required in ('ULT: What\'s New', 'ULT: Downloads', 'maybeShowUltReleaseNotes();',
                         'UltReleaseNotes.summary()', 'UltReleaseNotes.SEEN_KEY',
                         'setOnDismissListener', 'UltDownloadsActivity',
                         'initialDialogsType == DIALOGS_TYPE_DEFAULT'):
            self.assertIn(required,patch)
        self.assertIn('if (ultReleaseNoteScheduled || !isUltMainChatScreen()',patch)
        self.assertIn('getBoolean(UltReleaseNotes.SEEN_KEY, false)',patch)
        self.assertNotIn('new java.net.',patch)
    def test_shared_copy_and_no_false_privacy_claims(self):
        notes=read("settings/src/com/yooshastan/ultmessenger/settings/UltReleaseNotes.java")
        settings=read("settings/src/com/yooshastan/ultmessenger/settings/UltSettingsActivity.java")
        script=read("scripts/integrate-ult-settings.py")
        self.assertIn("UltReleaseNotes.summary()",settings)
        self.assertIn("UltReleaseNotes.java",script)
        for required in ("SHORTCUT ONLY", "NOT INCLUDED", "no new downloader", "No identity"):
            self.assertIn(required.lower(),notes.lower().replace("not a new downloader","no new downloader"))

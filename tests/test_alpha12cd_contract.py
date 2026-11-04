#!/usr/bin/env python3
"""Source-level Alpha 0.12c+d non-regression contracts; not an Android build."""
from pathlib import Path
from unittest import TestCase

ROOT = Path(__file__).resolve().parents[1]

def source(path):
    return (ROOT / path).read_text(encoding="utf-8")

class Alpha12CDContract(TestCase):
    def test_no_duplicate_bottom_category_bar(self):
        activity = source("scripts/integrate-chat-views.py")
        settings = source("settings/src/com/yooshastan/ultmessenger/settings/UltSettingsActivity.java")
        self.assertNotIn("ultSwipeTarget", activity)
        self.assertNotIn("installUltCategoryBar(", activity)
        self.assertIn("showSearch(true, false, true, true)", activity)
        self.assertIn("Chat views and sorting", settings)
        self.assertNotIn("addDialogFilter", activity)
        self.assertNotIn("deleteDialog(", activity)

    def test_file_and_message_copy_is_opt_in_and_metadata_only(self):
        helper = source("chat/src/com/yooshastan/ultmessenger/chat/UltChatTools.java")
        patch = source("scripts/integrate-message-tools.py")
        for token in ("safeFileName(", "mediaCitation(", "messageReference(", "publicMessageLink("):
            self.assertIn(token, helper)
        for token in ('ULT: Copy reference', 'ULT: Copy file info',
                      'selectedObject.getDocument() != null', '!selectedObject.isSponsored()',
                      'currentEncryptedChat == null'):
            self.assertIn(token, patch)
        self.assertNotIn('ULT: Copy message time (ISO UTC)', patch)
        for forbidden in ("sendMessage(", "forwardMessages(", "new java.net.", "new Socket("):
            self.assertNotIn(forbidden, patch)

    def test_no_heavy_feature_branch_build(self):
        ci = source(".github/workflows/scaffold-check.yml")
        android = source(".github/workflows/android-alpha.yml")
        self.assertIn("feature/alpha12cd-swipe-media-ux", ci)
        self.assertIn("test_alpha12cd_contract.py", ci)
        self.assertNotIn("./gradlew", ci)
        self.assertNotIn("feature/alpha12cd-swipe-media-ux", android)
        self.assertIn("python3 scripts/integrate-native-strict.py", android)

#!/usr/bin/env python3
"""Source-level Alpha 0.12c+d non-regression contracts; not an Android build."""
from pathlib import Path
from unittest import TestCase

ROOT = Path(__file__).resolve().parents[1]

def source(path):
    return (ROOT / path).read_text(encoding="utf-8")

class Alpha12CDContract(TestCase):
    def test_swipes_are_opt_in_dedicated_and_local(self):
        activity = source("scripts/integrate-chat-views.py")
        model = source("conversation/src/com/yooshastan/ultmessenger/conversation/UltChatView.java")
        settings = source("settings/src/com/yooshastan/ultmessenger/settings/UltSettingsActivity.java")
        for token in ("ultSwipeTarget.setOnTouchListener", "ultSwipeTarget.setVisibility",
                      "stepUltCategoryMode(dx < 0 ? 1 : -1)", "AndroidUtilities.dp(24)",
                      "setContentDescription", "stepVisibleTab("):
            self.assertIn(token, activity + model)
        self.assertIn('prefs.getBoolean(UltChatView.swipePadKey(currentAccount), false)', activity)
        self.assertIn('showSwipeNavigator(context)', settings)
        self.assertIn('showCategoryTypography(context)', settings)
        self.assertIn('barFontSp(', activity)
        self.assertNotIn('addDialogFilter', activity)
        self.assertNotIn('deleteDialog(', activity)

    def test_media_tools_are_user_triggered_and_metadata_only(self):
        helper = source("chat/src/com/yooshastan/ultmessenger/chat/UltChatTools.java")
        patch = source("scripts/integrate-message-tools.py")
        for token in ('safeFileName(', 'mediaCitation(', 'messageReference(', 'publicMessageLink('):
            self.assertIn(token, helper)
        for token in ('ULT: Copy safe file name', 'ULT: Copy media citation',
                      'selectedObject.getDocument() != null', '!selectedObject.isSponsored()',
                      'currentEncryptedChat == null && currentChat != null'):
            self.assertIn(token, patch)
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

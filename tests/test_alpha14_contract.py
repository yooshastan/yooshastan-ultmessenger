#!/usr/bin/env python3
"""Alpha 0.14 local-only new-feature contracts. No APK or network claims."""
from pathlib import Path
from unittest import TestCase

ROOT = Path(__file__).resolve().parents[1]
def read(path): return (ROOT / path).read_text(encoding="utf-8")

class Alpha14Contract(TestCase):
    def test_media_inspector_is_user_triggered_and_protected(self):
        s=read("settings/src/com/yooshastan/ultmessenger/settings/UltDownloadsActivity.java")
        d=read("settings/src/com/yooshastan/ultmessenger/settings/UltFileInspection.java")
        for needle in ("showDocumentActions(item)", "Inspect file and SHA-256",
                       "Save As…", "isExportAllowed(message, currentAccount)",
                       "DialogObject.isEncryptedDialog", "message.messageOwner.noforwards",
                       "chat.noforwards", "getPathToMessage(message.messageOwner)",
                       "new FileInputStream(source)", "new Thread(", "UltFileInspection.digest(",
                       "Copy SHA-256", "Context.CLIPBOARD_SERVICE"):
            self.assertIn(needle,s)
        self.assertIn('SHA-256 (local file)', d)
        self.assertIn('MAX_INSPECT_BYTES', d)
        self.assertIn('This does not verify', d)
        # UltSettingsActivity's builder is a private nested class. The
        # Downloads screen instead owns its secure windows so the Android
        # compiler can resolve every dialog reference.
        self.assertNotIn('new UltSecureDialogBuilder(', s)
        self.assertIn('showSecureDialog(activity, new android.app.AlertDialog.Builder(activity)', s)
        self.assertIn('private static void showSecureDialog(', s)
        self.assertIn('UltScreenPrivacy.isEnabled(activity)', s)
        self.assertIn('SharedConfig.passcodeHash', s)
        self.assertIn('WindowManager.LayoutParams.FLAG_SECURE', s)
        self.assertEqual(s.count('showSecureDialog(activity, new android.app.AlertDialog.Builder(activity)'), 3)
        self.assertNotIn('new java.net.', s+d)
        self.assertNotIn('sendMessage(', s)
    def test_non_destructive_theme_preview(self):
        s=read("settings/src/com/yooshastan/ultmessenger/settings/UltSettingsActivity.java")
        region=s.split("private void showUltCustomThemeEditor(",1)[1].split("private void showNativeMediaDownloadSettings(",1)[0]
        for needle in ("Preview colors (does not apply)", "UltCustomTheme.edit(", 
                       "Color preview — no changes applied", "previewPalette.incomingBubbleArgb",
                       "previewPalette.outgoingBubbleArgb", "previewPalette.accentArgb",
                       'setPositiveButton("Apply fixed theme"'):
            self.assertIn(needle,region)
        self.assertEqual(region.count("UltAppearanceThemeAdapter.applyCustom("),1)
        self.assertIn("UltSecureDialogBuilder",region)
    def test_generated_source_and_lightweight_workflow(self):
        script=read("scripts/integrate-ult-settings.py")
        wf=read(".github/workflows/scaffold-check.yml")
        full=read(".github/workflows/android-alpha.yml")
        self.assertIn("UltFileInspection.java",script)
        self.assertIn("feature/alpha14-media-inspector-theme-preview",wf)
        self.assertNotIn("feature/alpha14-media-inspector-theme-preview",full)

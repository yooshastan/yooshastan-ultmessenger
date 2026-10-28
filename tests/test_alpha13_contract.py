#!/usr/bin/env python3
"""Alpha 0.13a source contracts; does not substitute Android or device QA."""
from pathlib import Path
from unittest import TestCase

ROOT = Path(__file__).resolve().parents[1]

def source(path):
    return (ROOT / path).read_text(encoding="utf-8")

class Alpha13Contract(TestCase):
    def test_original_theme_is_left_untouched(self):
        adapter = source("appearance/src/com/yooshastan/ultmessenger/appearance/UltAppearanceThemeAdapter.java")
        model = source("appearance/src/com/yooshastan/ultmessenger/appearance/UltCustomTheme.java")
        script = source("scripts/integrate-appearance.py")
        self.assertIn('applyInternal(context, preset, followSystem, null)', adapter)
        self.assertIn('custom != null ? "-custom" : ""', adapter)
        self.assertIn('UltCustomTheme.edit(', adapter)
        self.assertIn('private static boolean applyInternal(', adapter)
        self.assertIn('contrast(base.textArgb, outgoing) < 4.5', model)
        self.assertIn('contrast(accent, base.backgroundArgb) < 3.0', model)
        self.assertIn('"UltCustomTheme.java"', script)
    def test_theme_editor_and_native_media_controls(self):
        settings = source("settings/src/com/yooshastan/ultmessenger/settings/UltSettingsActivity.java")
        for token in ('"Custom ULT theme colors"', 'showUltCustomThemeEditor(',
                      'UltAppearanceThemeAdapter.applyCustom(', 'UltSecureDialogBuilder',
                      'DataAutoDownloadActivity(which)', 'CacheControlActivity',
                      '"Identity and privacy limitations"'):
            self.assertIn(token, settings)
        self.assertNotIn('new android.app.AlertDialog.Builder(context)', settings[
            settings.index('private void showUltCustomThemeEditor'):settings.index('private void showUltThemePreview')])
    def test_upstream_download_queue_reused_with_account_scope(self):
        activity = source("settings/src/com/yooshastan/ultmessenger/settings/UltDownloadsActivity.java")
        settings = source("settings/src/com/yooshastan/ultmessenger/settings/UltSettingsActivity.java")
        script = source("scripts/integrate-ult-settings.py")
        for expected in ("new SearchDownloadsContainer(this, currentAccount)",
                         "FilteredSearchView.UiCallback",
                         "downloads.setUiCallback(this)",
                         "actionModeShowing()", "getFolderId()"):
            self.assertIn(expected, activity)
        self.assertIn("Download queue and recent files", settings)
        self.assertIn("new UltDownloadsActivity()", settings)
        self.assertIn("UltDownloadsActivity.java", script)
        self.assertNotIn("new java.net.", activity)
        self.assertNotIn("FileOutputStream", activity)
        self.assertNotIn("sendMessage(", activity)

    def test_native_proxy_and_download_integrations_not_replaced(self):
        android = source(".github/workflows/android-alpha.yml")
        scaffold = source(".github/workflows/scaffold-check.yml")
        self.assertIn('scripts/integrate-native-strict.py', android)
        self.assertIn('scripts/integrate-screen-privacy.py', android)
        self.assertIn('feature/alpha13-custom-theme-media', scaffold)
        self.assertNotIn('feature/alpha13-custom-theme-media', android)

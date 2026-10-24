#!/usr/bin/env python3
"""Regression guard for fixed ULT presets on system-night Android devices.

The Android Gradle job compiles the actual adapter. These source-contract checks
catch accidental loss of the snapshot/restore order or UI refresh integration.
"""
import importlib.util
from pathlib import Path
from tempfile import TemporaryDirectory
from unittest import TestCase, main

ROOT = Path(__file__).resolve().parents[2]
ADAPTER = ROOT / "appearance/src/com/yooshastan/ultmessenger/appearance/UltAppearanceThemeAdapter.java"
INTEGRATOR = ROOT / "scripts/integrate-appearance.py"


class ThemeStateContractTest(TestCase):
    def test_snapshot_precedes_install_and_auto_night_is_paused_after_success(self):
        text = ADAPTER.read_text(encoding="utf-8")
        before_import = text.index("Theme.ThemeInfo oldDay = Theme.getCurrentTheme();")
        import_theme = text.index("Theme.ThemeInfo installedTheme = Theme.applyThemeFile(")
        persist_state = text.index("editor.putInt(PREVIOUS_AUTO_NIGHT, oldAutoNight);")
        pause_night = text.index("Theme.selectedAutoNightType = Theme.AUTO_NIGHT_TYPE_NONE;", import_theme)
        self.assertLess(before_import, import_theme)
        self.assertLess(import_theme, persist_state)
        self.assertLess(persist_state, pause_night)
        self.assertIn("Theme.cancelAutoNightThemeCallbacks();", text)
        self.assertIn("Theme.applyTheme(installed);", text)
        self.assertIn("if (installedTheme == null) {", text)

    def test_system_restores_day_night_and_auto_night_without_resetting_unset(self):
        text = ADAPTER.read_text(encoding="utf-8")
        system_branch = text[text.index("if (preset == UltAppearancePreset.SYSTEM) {"):
                             text.index("Theme.ThemeInfo base =")]
        self.assertIn("if (!prefs.contains(PREVIOUS_THEME))", system_branch)
        self.assertIn("Theme.setCurrentNightTheme(previousNight);", system_branch)
        self.assertLess(system_branch.index("Theme.applyTheme(previousDay);"),
                        system_branch.index("Theme.checkAutoNightThemeConditions(true);"))
        self.assertIn("Theme.saveAutoNightThemeConfig();", system_branch)
        for field in ("PREVIOUS_THEME", "PREVIOUS_NIGHT_THEME", "PREVIOUS_AUTO_NIGHT"):
            self.assertIn(".remove(" + field + ")", system_branch)

    def test_paired_install_creates_both_slots_then_activates_system_mode(self):
        text = ADAPTER.read_text(encoding="utf-8")
        self.assertIn('writeThemeFile(context, preset, preset.forMode(false), "-day")', text)
        self.assertIn('writeThemeFile(context, preset, preset.forMode(true), "-night")', text)
        self.assertIn('Theme.applyThemeFile(dayFile,', text)
        self.assertIn('Theme.applyThemeFile(nightFile,', text)
        night_index = text.index('Theme.setCurrentNightTheme(pairedNight);')
        day_index = text.index('Theme.applyTheme(pairedDay);')
        system_index = text.index('Theme.selectedAutoNightType = Theme.AUTO_NIGHT_TYPE_SYSTEM;')
        self.assertLess(night_index, day_index)
        self.assertLess(day_index, system_index)
        self.assertIn('Theme.saveAutoNightThemeConfig(); // includes the night theme key', text)
        self.assertIn('if (pairedNight == null) {', text)
        self.assertIn('Theme.applyTheme(oldDay);', text)
        self.assertIn('Build.VERSION_CODES.Q', text)

    def test_manual_sun_moon_switch_uses_ult_companion_keys_and_restores_originals(self):
        text = ADAPTER.read_text(encoding="utf-8")
        self.assertIn('getSharedPreferences("themeconfig", Context.MODE_PRIVATE)', text)
        self.assertIn('togglePrefs.contains("lastDayTheme")', text)
        self.assertIn('togglePrefs.contains("lastDarkTheme")', text)
        self.assertIn('editor.putBoolean(PREVIOUS_TOGGLE_DAY_PRESENT, present)', text)
        self.assertIn('editor.putBoolean(PREVIOUS_TOGGLE_NIGHT_PRESENT, present)', text)
        self.assertIn('.putString("lastDayTheme", pairedDay.getKey())', text)
        self.assertIn('.putString("lastDarkTheme", pairedNight.getKey())', text)
        self.assertIn('restoreManualToggleTargets(prefs, togglePrefs);', text)
        self.assertIn('editor.remove("lastDayTheme");', text)
        self.assertIn('editor.remove("lastDarkTheme");', text)
        # Each stored old key must be cleared on System, not after each mode change.
        system_branch = text[text.index("if (preset == UltAppearancePreset.SYSTEM) {"):
                             text.index("if (followSystem && Build.VERSION.SDK_INT")]
        for field in ("PREVIOUS_TOGGLE_DAY", "PREVIOUS_TOGGLE_NIGHT",
                      "PREVIOUS_TOGGLE_DAY_PRESENT", "PREVIOUS_TOGGLE_NIGHT_PRESENT"):
            self.assertIn(".remove(" + field + ")", system_branch)
        paired_start = text.index('Theme.setCurrentNightTheme(pairedNight);')
        toggle_update = text.index('.putString("lastDayTheme", pairedDay.getKey())')
        auto_mode = text.index('Theme.selectedAutoNightType = Theme.AUTO_NIGHT_TYPE_SYSTEM;')
        self.assertLess(paired_start, toggle_update)
        self.assertLess(toggle_update, auto_mode)

    def test_chooser_preserves_options_and_refreshes_visible_fragment(self):
        spec = importlib.util.spec_from_file_location("ult_appearance_integration", INTEGRATOR)
        integration = importlib.util.module_from_spec(spec)
        spec.loader.exec_module(integration)
        with TemporaryDirectory() as tmp:
            root = Path(tmp)
            target = root / integration.TARGET
            target.parent.mkdir(parents=True)
            target.write_text(integration.IMPORT_ANCHOR + integration.CLASS_ANCHOR +
                              integration.MENU_ANCHOR + integration.CLICK_ANCHOR,
                              encoding="utf-8")
            integration.integrate(root)
            result = target.read_text(encoding="utf-8")
            self.assertIn(".setSingleChoiceItems(labels, current.ordinal()", result)
            self.assertIn("Follow phone light/dark", result)
            self.assertIn("Keep fixed colors", result)
            self.assertIn("if (preset == UltAppearancePreset.SYSTEM)", result)
            self.assertIn(".setNeutralButton(\"Theme behavior\"", result)
            self.assertIn("parentLayout.rebuildAllFragmentViews(true, true);", result)
            self.assertIn("System restores the original day theme", result)


if __name__ == "__main__":
    main()

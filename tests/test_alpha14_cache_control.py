"""Cache precision integration: native prefs, exact display, fail-closed patch."""
from pathlib import Path
from unittest import TestCase
import importlib.util
ROOT=Path(__file__).resolve().parents[1]
def source(path):return (ROOT/path).read_text(encoding="utf-8")
class CacheControl(TestCase):
    def test_real_telegram_preference_and_explicit_save_only(self):
        s=source("settings/src/com/yooshastan/ultmessenger/settings/UltSettingsActivity.java")
        for item in ("Custom maximum cache size", "STORAGE AND CACHE",
                     "showUltCacheLimit(context)", "new android.widget.SeekBar(context)",
                     'getInt("cache_limit", Integer.MAX_VALUE)',
                     'putInt("cache_limit", value).apply()', "No Limit",
                     '"Cancel"', '"Apply"', "UltCacheLimitPolicy.MIN_GB"):
            self.assertIn(item,s)
        self.assertIn("does not immediately delete existing files",s)
        self.assertNotIn('deleteRecursively(',s)
    def test_pinned_source_patch_inserts_only_current_selected_value(self):
        script=source("scripts/integrate-cache-limit.py")
        path=ROOT/"scripts/integrate-cache-limit.py"
        spec=importlib.util.spec_from_file_location("ultcache",path)
        mod=importlib.util.module_from_spec(spec)
        spec.loader.exec_module(mod)
        fixture="before\n"+mod.INSERT_BEFORE+"after\n"
        out=mod.patch(fixture)
        for needle in ('getInt("cache_limit", Integer.MAX_VALUE)',
                       "selectedCustomLimit >= 2", "selectedCustomLimit <= 32",
                       "!options.contains(selectedCustomLimit)",
                       "options.add(ultCacheInsertPosition, selectedCustomLimit)"):
            self.assertIn(needle,out)
        # Pinned upstream onCreateViewHolder() already declares "index" in
        # an enclosing switch scope. Shadowing it caused the native Java
        # compilation failure in Actions run #41231831464.
        self.assertIn("int ultCacheInsertPosition = 0;", out)
        self.assertNotIn("int index =", out)
        self.assertLess(out.index("options.add(ultCacheInsertPosition, selectedCustomLimit)"),
                        out.index("options.add(Integer.MAX_VALUE)"))
        with self.assertRaises(ValueError):mod.patch(out)
        with self.assertRaises(ValueError):mod.patch("no pinned anchor")
        self.assertNotIn("removeCache", script)
    def test_integration_only_during_android_build(self):
        script=source("scripts/integrate-ult-settings.py")
        workflow=source(".github/workflows/android-alpha.yml")
        fast=source(".github/workflows/scaffold-check.yml")
        self.assertIn("UltCacheLimitPolicy.java",script)
        self.assertIn('python3 scripts/integrate-cache-limit.py "$source_dir"', workflow)
        self.assertIn("feature/alpha14-cache-size-control",fast)
        self.assertNotIn("feature/alpha14-cache-size-control",workflow)

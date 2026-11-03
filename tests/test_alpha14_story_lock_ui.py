"""Distinguish native Premium lock from ULT's own eligible story menu."""
from pathlib import Path
from unittest import TestCase
ROOT=Path(__file__).resolve().parents[1]
def src(p):return (ROOT/p).read_text(encoding="utf-8")
class StoryLockUi(TestCase):
    def test_custom_menu_is_independent_and_diagnosable(self):
        patch=src("scripts/integrate-story-save.py")
        self.assertIn("ULT: Download story",patch)
        self.assertIn("ULT: Download unavailable",patch)
        self.assertIn("MENU_VIEW + MENU_ANCHOR",patch)
        self.assertIn("ultStoryUnavailableReason()",patch)
        self.assertIn("ultDownloadAndSaveStory();",patch)
        self.assertIn("Only your own or public/shareable stories",patch)
        self.assertIn("This story restricts saving or forwarding",patch)
        self.assertIn("chat == null || chat.noforwards",patch)
        self.assertNotIn("setPremium(",patch)
        self.assertNotIn("new java.net.",patch)
    def test_native_lock_unmodified(self):
        patch=src("scripts/integrate-story-save.py")
        self.assertIn('MENU_ANCHOR = "                        if (!unsupported && allowShare',patch)
        self.assertNotIn("getString(R.string.SaveStoryToGalleryPremiumHint)",patch)

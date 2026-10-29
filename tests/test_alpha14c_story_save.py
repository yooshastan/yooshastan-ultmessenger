"""Source contracts for pinned Telegram Story save integration."""
from pathlib import Path
import importlib.util
from unittest import TestCase

ROOT=Path(__file__).resolve().parents[1]
def get(path): return (ROOT/path).read_text(encoding="utf-8")

class StorySaveContract(TestCase):
    def test_ui_uses_existing_native_save_and_live_guard(self):
        script=get("scripts/integrate-story-save.py")
        self.assertIn("PeerStoriesView.java", script)
        self.assertIn("UltStorySavePolicy", script)
        self.assertIn("Save eligible story to gallery", script)
        self.assertIn("saveToGallery();", script)
        self.assertIn("canUltSaveStory()", script)
        self.assertIn("ultStorySaveButton.setVisibility", script)
        self.assertIn("item instanceof TL_stories.TL_storyItemSkipped", script)
        self.assertIn("item instanceof TL_stories.TL_storyItemDeleted", script)
        self.assertIn("chat == null || chat.noforwards", script)
        self.assertIn("currentStory.allowScreenshots()", script)
        self.assertIn("UserConfig.getInstance(currentAccount).isPremium()", script)
        self.assertIn("allowShare, protectedPeer", script)
        self.assertNotIn("loadFile(", script)
        self.assertNotIn("MediaController.saveFile(", script)
        self.assertNotIn("new java.net.", script)

    def test_patch_fail_closed_and_rejects_duplicate(self):
        path=ROOT/"scripts/integrate-story-save.py"
        spec=importlib.util.spec_from_file_location("integrate_story_save",path)
        module=importlib.util.module_from_spec(spec)
        spec.loader.exec_module(module)
        # A toy fixture built from the *exact* anchors; upstream still needs
        # native CI to catch source drift/compile errors.
        source="".join([module.IMPORT_ANCHOR,module.FIELD_ANCHOR,
                        module.VIEW_ANCHOR,module.VISIBILITY_ANCHOR,module.METHOD_ANCHOR])
        patched=module.patch_story_source(source)
        self.assertIn("ultStorySaveButton",patched)
        self.assertIn("saveToGallery()",patched)
        with self.assertRaises(ValueError):
            module.patch_story_source(patched)
        with self.assertRaises(ValueError):
            module.patch_story_source(source.replace(module.VIEW_ANCHOR,""))

    def test_build_is_late_and_policy_is_source_owned(self):
        android=get(".github/workflows/android-alpha.yml")
        scaffold=get(".github/workflows/scaffold-check.yml")
        self.assertIn('scripts/integrate-story-save.py "$source_dir"',android)
        self.assertIn("feature/alpha14c-story-save-quick-action",scaffold)
        self.assertNotIn("feature/alpha14c-story-save-quick-action",android)
        policy=get("stories/src/com/yooshastan/ultmessenger/stories/UltStorySavePolicy.java")
        self.assertIn("premium && publicShareAllowed",policy)
        self.assertIn("!peerProtected",policy)

#!/usr/bin/env python3
"""Inject a visible, policy-gated story Gallery save shortcut into pinned Mercurygram.

This reuses PeerStoriesView.saveToGallery(), which delegates to the upstream
MediaController for already-downloaded media. It does not unlock Premium,
override noforwards, download missing story data or change network behavior.
"""
import argparse
from pathlib import Path

TARGET = Path("TMessagesProj/src/main/java/org/telegram/ui/Stories/PeerStoriesView.java")
MODEL = Path("stories/src/com/yooshastan/ultmessenger/stories/UltStorySavePolicy.java")
DEST = Path("TMessagesProj/src/main/java/com/yooshastan/ultmessenger/stories/UltStorySavePolicy.java")

IMPORT_ANCHOR = "import org.telegram.ui.Stories.recorder.StoryRecorder;\n"
FIELD_ANCHOR = "    private final ImageView optionsIconView;\n"
VIEW_ANCHOR = (
    "        storyContainer.addView(optionsIconView, "
    "LayoutHelper.createFrame(40, 40, Gravity.RIGHT | Gravity.TOP, 2, 15, 2, 0));\n"
)
VISIBILITY_ANCHOR = (
    "        if (optionsIconView != null) {\n"
    "            optionsIconView.setVisibility(isBotsPreview() && !isEditBotsPreview() "
    "&& (currentStory == null || !currentStory.isVideo) ? View.GONE : View.VISIBLE);\n"
    "        }\n"
)
METHOD_ANCHOR = "    private void saveToGallery() {\n"

SAVE_VIEW = """        // Visible Ultgram save shortcut. Upstream SaveToGallery remains the
        // sole media copy path; do not bypass screenshots/Premium restrictions.
        ultStorySaveButton = new ImageView(context);
        ultStorySaveButton.setImageResource(R.drawable.msg_gallery);
        ultStorySaveButton.setColorFilter(Color.WHITE);
        ultStorySaveButton.setPadding(dp(8), dp(8), dp(8), dp(8));
        ultStorySaveButton.setBackground(Theme.createSelectorDrawable(Color.WHITE));
        ultStorySaveButton.setContentDescription("Save eligible story to gallery");
        ultStorySaveButton.setVisibility(View.GONE);
        ultStorySaveButton.setOnClickListener(v -> {
            if (canUltSaveStory()) {
                saveToGallery();
            }
        });
        storyContainer.addView(ultStorySaveButton,
                LayoutHelper.createFrame(40, 40, Gravity.RIGHT | Gravity.TOP, 2, 15, 42, 0));
"""

SAVE_METHOD = """    private boolean canUltSaveStory() {
        if (currentStory == null) return false;
        final TL_stories.StoryItem item = currentStory.storyItem;
        final boolean available = item != null && item.media != null;
        boolean protectedPeer = false;
        if (dialogId < 0) {
            final TLRPC.Chat chat = MessagesController.getInstance(currentAccount)
                    .getChat(-dialogId);
            // If protection state is unavailable, fail closed.
            protectedPeer = chat == null || chat.noforwards;
        }
        return UltStorySavePolicy.eligible(available,
                item instanceof TL_stories.TL_storyItemSkipped
                        || item instanceof TL_stories.TL_storyItemDeleted,
                unsupported, currentStory.isLive, currentStory.allowScreenshots(),
                isSelf, UserConfig.getInstance(currentAccount).isPremium(),
                allowShare, protectedPeer);
    }

"""


def once(text: str, anchor: str, replacement: str) -> str:
    matches = text.count(anchor)
    if matches != 1:
        raise ValueError(f"Pinned story source drift; expected one anchor, got {matches}: {anchor[:90]!r}")
    return text.replace(anchor, replacement, 1)


def patch_story_source(source: str) -> str:
    if "ultStorySaveButton" in source:
        raise ValueError("Story save shortcut already integrated; refusing a duplicate")
    source = once(source, IMPORT_ANCHOR, IMPORT_ANCHOR
                  + "import com.yooshastan.ultmessenger.stories.UltStorySavePolicy;\n")
    source = once(source, FIELD_ANCHOR,
                  FIELD_ANCHOR + "    private final ImageView ultStorySaveButton;\n")
    source = once(source, VIEW_ANCHOR, VIEW_ANCHOR + SAVE_VIEW)
    source = once(source, VISIBILITY_ANCHOR, VISIBILITY_ANCHOR +
                  "        ultStorySaveButton.setVisibility(canUltSaveStory() ? View.VISIBLE : View.GONE);\n")
    source = once(source, METHOD_ANCHOR, SAVE_METHOD + METHOD_ANCHOR)
    return source


def integrate(root: Path) -> Path:
    target = root / TARGET
    original = target.read_text(encoding="utf-8")
    modified = patch_story_source(original)
    model = Path(__file__).resolve().parents[1] / MODEL
    policy_bytes = model.read_bytes()
    destination = root / DEST
    target.write_text(modified, encoding="utf-8")
    destination.parent.mkdir(parents=True, exist_ok=True)
    destination.write_bytes(policy_bytes)
    return target


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("source_dir", type=Path)
    args = parser.parse_args()
    print(integrate(args.source_dir))

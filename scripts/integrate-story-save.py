#!/usr/bin/env python3
"""Inject a visible, policy-gated story Gallery save shortcut into pinned Mercurygram.

Uses native FileLoader for media already accessible to the current account,
then Telegram's MediaController to save. It does not impersonate Premium,
remove noforwards restrictions, access private stories or add new endpoints.
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
MENU_ANCHOR = "                        if (!unsupported && allowShare && !currentStory.isLive) {\n"
DETACH_ANCHOR = (
    "    @Override\n"
    "    protected void onDetachedFromWindow() {\n"
    "        super.onDetachedFromWindow();\n"
    "        attachedToWindow = false;\n"
)

SAVE_VIEW = """        // Visible Ultgram save shortcut for locally accessible, unprotected
        // own or public/shareable stories. Protected media stays unavailable.
        ultStorySaveButton = new ImageView(context);
        ultStorySaveButton.setImageResource(R.drawable.msg_gallery);
        ultStorySaveButton.setColorFilter(Color.WHITE);
        ultStorySaveButton.setPadding(dp(8), dp(8), dp(8), dp(8));
        ultStorySaveButton.setBackground(Theme.createSelectorDrawable(Color.WHITE));
        ultStorySaveButton.setContentDescription("Download and save eligible story");
        ultStorySaveButton.setVisibility(View.GONE);
        ultStorySaveButton.setOnClickListener(v -> {
            if (canUltSaveStory()) {
                ultDownloadAndSaveStory();
            }
        });
        storyContainer.addView(ultStorySaveButton,
                LayoutHelper.createFrame(40, 40, Gravity.RIGHT | Gravity.TOP, 2, 15, 42, 0));
"""

MENU_VIEW = """                        // ULT download is distinct from Telegram's Premium-only
                        // SaveToGallery menu. Keep the original locked menu and
                        // every native restriction intact; explain ineligible cases.
                        if (!currentStory.isLive && currentStory.storyItem != null) {
                            ActionBarMenuItem.addItem(popupLayout, R.drawable.msg_gallery,
                                    canUltSaveStory()
                                            ? "ULT: Download story"
                                            : "ULT: Download unavailable",
                                    false, resourcesProvider).setOnClickListener(v -> {
                                if (popupMenu != null) popupMenu.dismiss();
                                if (canUltSaveStory()) {
                                    ultDownloadAndSaveStory();
                                } else {
                                    android.widget.Toast.makeText(getContext(),
                                            ultStoryUnavailableReason(),
                                            android.widget.Toast.LENGTH_LONG).show();
                                }
                            });
                        }
"""

SAVE_METHOD = """    private NotificationCenter.NotificationCenterDelegate ultStoryFileObserver;
    private int ultStoryFileAccount = -1;

    private void ultStopStoryDownloadObserver() {
        if (ultStoryFileObserver != null && ultStoryFileAccount >= 0) {
            NotificationCenter.getInstance(ultStoryFileAccount)
                    .removeObserver(ultStoryFileObserver, NotificationCenter.fileLoaded);
            NotificationCenter.getInstance(ultStoryFileAccount)
                    .removeObserver(ultStoryFileObserver, NotificationCenter.fileLoadFailed);
        }
        ultStoryFileObserver = null;
        ultStoryFileAccount = -1;
    }

    /**
     * Reuses the current account's native FileLoader, never a new network
     * endpoint. The user selects the story; no background story harvesting.
     */
    private void ultDownloadAndSaveStory() {
        if (!canUltSaveStory()) return;
        final File cached = currentStory.getPath();
        if (cached != null && cached.isFile() && cached.length() > 0) {
            saveToGallery();
            return;
        }
        final TL_stories.StoryItem selected = currentStory.storyItem;
        if (selected == null || selected.media == null) return;
        final TLRPC.Document document = selected.media.getDocument();
        final TLRPC.PhotoSize photoSize = document == null && selected.media.photo != null
                ? FileLoader.getClosestPhotoSizeWithSize(selected.media.photo.sizes,
                        Integer.MAX_VALUE) : null;
        final String filename = document != null
                ? FileLoader.getAttachFileName(document)
                : photoSize != null ? FileLoader.getAttachFileName(photoSize) : null;
        if (filename == null || filename.length() == 0) {
            showDownloadAlert();
            return;
        }
        ultStopStoryDownloadObserver();
        final int selectedAccount = currentAccount;
        final long selectedDialog = dialogId;
        final boolean selectedVideo = currentStory.isVideo();
        ultStoryFileAccount = selectedAccount;
        ultStoryFileObserver = new NotificationCenter.NotificationCenterDelegate() {
            @Override
            public void didReceivedNotification(int id, int account, Object... args) {
                if (account != selectedAccount || args.length < 1
                        || !filename.equals(args[0])) return;
                final File received = id == NotificationCenter.fileLoaded
                        && args.length > 1 && args[1] instanceof File
                        ? (File) args[1] : null;
                ultStopStoryDownloadObserver();
                // Never save a different story after fast account/story switching.
                if (!attachedToWindow || currentAccount != selectedAccount
                        || dialogId != selectedDialog || currentStory.storyItem != selected
                        || !canUltSaveStory()) return;
                if (id == NotificationCenter.fileLoaded) {
                    final File output = received != null ? received : currentStory.getPath();
                    if (output != null && output.isFile() && output.length() > 0) {
                        MediaController.saveFile(output.toString(), getContext(),
                                selectedVideo ? 1 : 0, null, null, uri -> {
                                    if (attachedToWindow && currentAccount == selectedAccount
                                            && currentStory.storyItem == selected) {
                                        BulletinFactory.createSaveToGalleryBulletin(
                                                storyContainer, selectedVideo, resourcesProvider)
                                                .show();
                                    }
                                });
                    } else {
                        showDownloadAlert();
                    }
                } else if (id == NotificationCenter.fileLoadFailed) {
                    android.widget.Toast.makeText(getContext(),
                            "Story download failed. Check connection and retry.",
                            android.widget.Toast.LENGTH_LONG).show();
                }
            }
        };
        final NotificationCenter center = NotificationCenter.getInstance(selectedAccount);
        center.addObserver(ultStoryFileObserver, NotificationCenter.fileLoaded);
        center.addObserver(ultStoryFileObserver, NotificationCenter.fileLoadFailed);
        try {
            if (document != null) {
                FileLoader.getInstance(selectedAccount).loadFile(document, selected,
                        FileLoader.PRIORITY_HIGH, 0);
            } else {
                FileLoader.getInstance(selectedAccount).loadFile(
                        ImageLocation.getForPhoto(photoSize, selected.media.photo),
                        selected, "jpg", FileLoader.PRIORITY_HIGH, 0);
            }
            android.widget.Toast.makeText(getContext(),
                    "Downloading story for Gallery…", android.widget.Toast.LENGTH_SHORT).show();
        } catch (RuntimeException error) {
            ultStopStoryDownloadObserver();
            showDownloadAlert();
        }
    }

    private String ultStoryUnavailableReason() {
        if (currentStory == null || currentStory.storyItem == null
                || currentStory.storyItem.media == null) {
            return "The story is unavailable or its media has not loaded";
        }
        if (unsupported || currentStory.isLive
                || currentStory.storyItem instanceof TL_stories.TL_storyItemSkipped
                || currentStory.storyItem instanceof TL_stories.TL_storyItemDeleted) {
            return "Live, expired or unsupported stories cannot be saved";
        }
        if (!currentStory.allowScreenshots()) {
            return "This story restricts saving or forwarding";
        }
        if (dialogId < 0) {
            final TLRPC.Chat chat = MessagesController.getInstance(currentAccount)
                    .getChat(-dialogId);
            if (chat == null || chat.noforwards) {
                return "This channel restricts saving or its protections are unknown";
            }
        }
        if (!isSelf && !allowShare) {
            return "Only your own or public/shareable stories can be downloaded";
        }
        return "This story is not eligible for download";
    }

    private boolean canUltSaveStory() {
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
    source = once(source, MENU_ANCHOR, MENU_VIEW + MENU_ANCHOR)
    source = once(source, DETACH_ANCHOR,
                  DETACH_ANCHOR + "        ultStopStoryDownloadObserver();\n")
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

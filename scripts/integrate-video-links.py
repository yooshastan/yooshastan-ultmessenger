#!/usr/bin/env python3
"""Install opt-in HTTP(S) video autosave without changing Telegram's own media queue."""
import argparse
from pathlib import Path

APP = Path("TMessagesProj/src/main/java/org/telegram/messenger/ApplicationLoader.java")
STRICT = Path("TMessagesProj/src/main/java/org/telegram/tgnet/ConnectionsManager.java")
GRADLE = Path("TMessagesProj/build.gradle")
MANIFEST = Path("TMessagesProj_App/src/main/AndroidManifest.xml")
CHAT = Path("TMessagesProj/src/main/java/org/telegram/ui/ChatActivity.java")
DEST = Path("TMessagesProj/src/main/java/com/yooshastan/ultmessenger/video")
GRADLE_ANCHOR = "    implementation 'org.nanohttpd:nanohttpd:2.3.1'\n"
MANIFEST_ANCHOR = '<application android:name="org.telegram.messenger.ApplicationLoaderImpl" tools:replace="name">'
CHAT_CONSTANT_ANCHOR = "    public final static int OPTION_COPY = 3;\n"
CHAT_MENU_ANCHOR = "        if (showWelcomeMessageRevertOption(primaryMessage)) {\n"
CHAT_HANDLER_ANCHOR = "            case OPTION_COPY: {\n"
CHAT_CONSTANTS = """    public final static int OPTION_ULT_MEDIA_VIDEO = 0x554c70;
    public final static int OPTION_ULT_MEDIA_AUDIO = 0x554c71;
    public final static int OPTION_ULT_MEDIA_OPTIONS = 0x554c72;
    public final static int OPTION_ULT_MEDIA_COPY = 0x554c73;
    public final static int OPTION_ULT_MEDIA_SAVE = 0x554c74;
    public final static int OPTION_ULT_MEDIA_REVIEW = 0x554c75;
"""
CHAT_MENU = """        if (com.yooshastan.ultmessenger.video.UltManualMediaMenu.eligible(selectedObject,
                currentEncryptedChat != null || (currentChat != null && currentChat.noforwards))) {
            if (!com.yooshastan.ultmessenger.video.UltVideoAutoSave.strictModeRequested()) {
                items.add("ULT: Add video to chat"); options.add(OPTION_ULT_MEDIA_VIDEO); icons.add(R.drawable.msg_copy);
                items.add("ULT: Add audio to chat"); options.add(OPTION_ULT_MEDIA_AUDIO); icons.add(R.drawable.msg_copy);
                items.add("ULT: Add media with options"); options.add(OPTION_ULT_MEDIA_OPTIONS); icons.add(R.drawable.msg_copy);
                items.add("ULT: Save to device"); options.add(OPTION_ULT_MEDIA_SAVE); icons.add(R.drawable.msg_copy);
                if (com.yooshastan.ultmessenger.video.UltChatMediaDraft.hasReady(currentAccount, selectedObject)) {
                    items.add("ULT: Review ready attachment"); options.add(OPTION_ULT_MEDIA_REVIEW); icons.add(R.drawable.msg_copy);
                }
            }
            items.add("ULT: Copy sanitized link"); options.add(OPTION_ULT_MEDIA_COPY); icons.add(R.drawable.msg_copy);
        }
"""
CHAT_HANDLERS = """            case OPTION_ULT_MEDIA_VIDEO: {
                com.yooshastan.ultmessenger.video.UltManualMediaMenu.open(this, currentAccount,
                        selectedObject, currentEncryptedChat != null || (currentChat != null && currentChat.noforwards), 0);
                break;
            }
            case OPTION_ULT_MEDIA_AUDIO: {
                com.yooshastan.ultmessenger.video.UltManualMediaMenu.open(this, currentAccount,
                        selectedObject, currentEncryptedChat != null || (currentChat != null && currentChat.noforwards), 1);
                break;
            }
            case OPTION_ULT_MEDIA_OPTIONS: {
                com.yooshastan.ultmessenger.video.UltManualMediaMenu.open(this, currentAccount,
                        selectedObject, currentEncryptedChat != null || (currentChat != null && currentChat.noforwards), 2);
                break;
            }
            case OPTION_ULT_MEDIA_SAVE: {
                com.yooshastan.ultmessenger.video.UltManualMediaMenu.open(this, currentAccount,
                        selectedObject, currentEncryptedChat != null || (currentChat != null && currentChat.noforwards), 4);
                break;
            }
            case OPTION_ULT_MEDIA_REVIEW: {
                com.yooshastan.ultmessenger.video.UltManualMediaMenu.open(this, currentAccount,
                        selectedObject, currentEncryptedChat != null || (currentChat != null && currentChat.noforwards), 5);
                break;
            }
            case OPTION_ULT_MEDIA_COPY: {
                com.yooshastan.ultmessenger.video.UltManualMediaMenu.open(this, currentAccount,
                        selectedObject, currentEncryptedChat != null || (currentChat != null && currentChat.noforwards), 3);
                break;
            }
"""
SOURCE = Path("video/src/com/yooshastan/ultmessenger/video")
APP_ANCHOR = "        it.belloworld.mercurygram.folders.MgFolderSync.startAll(); // Mercurygram: folder sync via Saved Messages\n"
STRICT_ANCHOR = '                .putBoolean(ULT_STRICT_NATIVE_PROXY_KEY, enabled).apply();\n'



def replace_once(source: str, anchor: str, replacement: str) -> str:
    count = source.count(anchor)
    if count != 1:
        raise ValueError(f"Expected one pinned video-link anchor, found {count}: {anchor[:90]!r}")
    return source.replace(anchor, replacement, 1)

def integrate(root: Path) -> None:
    app_file, strict_file = root / APP, root / STRICT
    chat_file = root / CHAT
    chat = chat_file.read_text(encoding="utf-8")
    if "OPTION_ULT_MEDIA_VIDEO" in chat:
        raise ValueError("Manual media already integrated")
    chat_new = replace_once(chat, CHAT_CONSTANT_ANCHOR, CHAT_CONSTANTS + CHAT_CONSTANT_ANCHOR)
    chat_new = replace_once(chat_new, CHAT_MENU_ANCHOR, CHAT_MENU + CHAT_MENU_ANCHOR)
    chat_new = replace_once(chat_new, CHAT_HANDLER_ANCHOR, CHAT_HANDLERS + CHAT_HANDLER_ANCHOR)
    app = app_file.read_text(encoding="utf-8")
    strict = strict_file.read_text(encoding="utf-8")
    gradle_file, manifest_file = root / GRADLE, root / MANIFEST
    gradle = gradle_file.read_text(encoding="utf-8")
    manifest = manifest_file.read_text(encoding="utf-8")
    if "UltVideoAutoSave.install()" in app:
        raise ValueError("Video links already integrated")
    app_new = replace_once(app, APP_ANCHOR,
        "        com.yooshastan.ultmessenger.video.UltVideoAutoSave.install();\n" + APP_ANCHOR)
    strict_new = replace_once(strict, STRICT_ANCHOR, STRICT_ANCHOR
        + "        if (enabled) {\n"
        + "            com.yooshastan.ultmessenger.video.UltVideoAutoSave.cancelPendingForStrict();\n"
        + "        }\n")
    gradle_new = replace_once(gradle, GRADLE_ANCHOR, GRADLE_ANCHOR
        + "    implementation 'io.github.junkfood02.youtubedl-android:library:0.18.1'\n"
        + "    implementation 'io.github.junkfood02.youtubedl-android:ffmpeg:0.18.1'\n")
    manifest_new = replace_once(manifest, MANIFEST_ANCHOR,
        '<application android:name="org.telegram.messenger.ApplicationLoaderImpl" '
        'android:extractNativeLibs="true" tools:replace="name,android:extractNativeLibs">')
    source = Path(__file__).resolve().parents[1] / SOURCE
    models = {name: (source / name).read_bytes()
              for name in ("UltVideoLinkRules.java", "UltVideoAutoSave.java", "UltVideoSiteDownloader.java",
                           "UltVideoChatPost.java", "UltVideoFailure.java", "UltExtractorPolicy.java",
                           "UltExtractorMaintenance.java", "UltMediaOptions.java", "UltManualMediaMenu.java", "UltChatMediaDraft.java")}
    chat_file.write_text(chat_new, encoding="utf-8")
    app_file.write_text(app_new, encoding="utf-8")
    strict_file.write_text(strict_new, encoding="utf-8")
    gradle_file.write_text(gradle_new, encoding="utf-8")
    manifest_file.write_text(manifest_new, encoding="utf-8")
    dest = root / DEST
    dest.mkdir(parents=True, exist_ok=True)
    for name, data in models.items():
        (dest / name).write_bytes(data)

if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("source_root", type=Path)
    integrate(parser.parse_args().source_root)
    print("Installed opt-in video link auto-save")

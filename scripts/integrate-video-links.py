#!/usr/bin/env python3
"""Install opt-in HTTP(S) video autosave without changing Telegram's own media queue."""
import argparse
from pathlib import Path

APP = Path("TMessagesProj/src/main/java/org/telegram/messenger/ApplicationLoader.java")
STRICT = Path("TMessagesProj/src/main/java/org/telegram/tgnet/ConnectionsManager.java")
GRADLE = Path("TMessagesProj/build.gradle")
PROGUARD = Path("TMessagesProj/proguard-rules.pro")
ZIP_KEEP = """# Ultgram: preserve Commons Compress ZIP extra fields instantiated by reflection.
-keep class org.apache.commons.compress.archivers.zip.** { *; }
"""
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
"""
CHAT_MENU = """        if (com.yooshastan.ultmessenger.video.UltManualMediaMenu.eligible(selectedObject,
                currentEncryptedChat != null || (currentChat != null && currentChat.noforwards))) {
            if (!com.yooshastan.ultmessenger.video.UltVideoAutoSave.strictModeRequested()) {
                items.add("ULT: Download video to chat"); options.add(OPTION_ULT_MEDIA_VIDEO); icons.add(R.drawable.ult_media_video);
                items.add("ULT: Download audio to chat"); options.add(OPTION_ULT_MEDIA_AUDIO); icons.add(R.drawable.ult_media_audio);
                items.add("ULT: Add media with options"); options.add(OPTION_ULT_MEDIA_OPTIONS); icons.add(R.drawable.ult_media_options);
                items.add("ULT: Save to device"); options.add(OPTION_ULT_MEDIA_SAVE); icons.add(R.drawable.ult_media_save);
            }
            items.add("ULT: Copy sanitized link"); options.add(OPTION_ULT_MEDIA_COPY); icons.add(R.drawable.ult_media_link);
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
            case OPTION_ULT_MEDIA_COPY: {
                com.yooshastan.ultmessenger.video.UltManualMediaMenu.open(this, currentAccount,
                        selectedObject, currentEncryptedChat != null || (currentChat != null && currentChat.noforwards), 3);
                break;
            }
"""
SOURCE = Path("video/src/com/yooshastan/ultmessenger/video")
# Owned vector drawables avoid depending on unverified upstream icon names.
ICON_PATHS = {
    "ult_media_video": "M4,5 H15 Q17,5 17,7 V9 L22,6 V18 L17,15 V17 Q17,19 15,19 H4 Q2,19 2,17 V7 Q2,5 4,5 Z",
    "ult_media_audio": "M9,3 V15 A4,4 0,1 0,11 18 V7 L20,5 V3 Z",
    "ult_media_options": "M3,5 H21 V7 H3 Z M3,11 H21 V13 H3 Z M3,17 H21 V19 H3 Z M7,3 H10 V9 H7 Z M14,9 H17 V15 H14 Z M8,15 H11 V21 H8 Z",
    "ult_media_save": "M11,2 H13 V13 L17,9 L19,11 L12,18 L5,11 L7,9 L11,13 Z M3,17 H5 V20 H19 V17 H21 V22 H3 Z",
    "ult_media_review": "M4,3 H20 V21 H4 Z M9,7 V17 L17,12 Z",
    "ult_media_link": "M10,7 L13,4 Q17,0 21,4 Q25,8 21,12 L18,15 L16,13 L19,10 Q21,8 19,6 Q17,4 15,6 L12,9 Z M14,17 L11,20 Q7,24 3,20 Q-1,16 3,12 L6,9 L8,11 L5,14 Q3,16 5,18 Q7,20 9,18 L12,15 Z M8,14 L14,8 L16,10 L10,16 Z",
}

def install_icons(root):
    dest = root / "TMessagesProj/src/main/res/drawable"
    dest.mkdir(parents=True, exist_ok=True)
    for name, path in ICON_PATHS.items():
        (dest / (name + ".xml")).write_text(
            '<vector xmlns:android="http://schemas.android.com/apk/res/android" '
            'android:width="24dp" android:height="24dp" android:viewportWidth="24" android:viewportHeight="24">'
            '<path android:fillColor="#FFFFFFFF" android:fillType="evenOdd" android:pathData="' + path + '"/>'
            '</vector>\n', encoding="utf-8")

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
    proguard_file = root / PROGUARD
    proguard = proguard_file.read_text(encoding="utf-8")
    if ZIP_KEEP in proguard:
        raise ValueError("Video ZIP keep rules already integrated")
    proguard_new = proguard.rstrip() + "\n\n" + ZIP_KEEP
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
    install_icons(root)
    chat_file.write_text(chat_new, encoding="utf-8")
    app_file.write_text(app_new, encoding="utf-8")
    strict_file.write_text(strict_new, encoding="utf-8")
    gradle_file.write_text(gradle_new, encoding="utf-8")
    proguard_file.write_text(proguard_new, encoding="utf-8")
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

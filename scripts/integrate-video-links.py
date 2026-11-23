#!/usr/bin/env python3
"""Install opt-in HTTP(S) video autosave without changing Telegram's own media queue."""
import argparse
from pathlib import Path

APP = Path("TMessagesProj/src/main/java/org/telegram/messenger/ApplicationLoader.java")
DOWNLOADS = Path("TMessagesProj/src/main/java/com/yooshastan/ultmessenger/settings/UltDownloadsActivity.java")
STRICT = Path("TMessagesProj/src/main/java/org/telegram/tgnet/ConnectionsManager.java")
GRADLE = Path("TMessagesProj/build.gradle")
MANIFEST = Path("TMessagesProj_App/src/main/AndroidManifest.xml")
DEST = Path("TMessagesProj/src/main/java/com/yooshastan/ultmessenger/video")
GRADLE_ANCHOR = "    implementation 'org.nanohttpd:nanohttpd:2.3.1'\n"
MANIFEST_ANCHOR = '<application android:name="org.telegram.messenger.ApplicationLoaderImpl" tools:replace="name">'
SOURCE = Path("video/src/com/yooshastan/ultmessenger/video")
APP_ANCHOR = "        it.belloworld.mercurygram.folders.MgFolderSync.startAll(); // Mercurygram: folder sync via Saved Messages\n"
TOOLBAR_ANCHOR = '        addToolbarButton(context, toolbar, "Network rules", this::showAutoDownloadRules);\n'
METHOD_ANCHOR = "    private void showAutoDownloadRules() {\n"
STRICT_ANCHOR = '                .putBoolean(ULT_STRICT_NATIVE_PROXY_KEY, enabled).apply();\n'

SETTINGS_METHOD = """    private void showVideoLinkSettings() {
        final Activity activity = getParentActivity();
        if (activity == null) return;
        final int account = currentAccount;
        final boolean enabled = com.yooshastan.ultmessenger.video.UltVideoAutoSave.isEnabled(account);
        final boolean wifiOnly = com.yooshastan.ultmessenger.video.UltVideoAutoSave.isWifiOnly(account);
        showSecureDialog(activity, new android.app.AlertDialog.Builder(activity)
                .setTitle("Auto-save video links")
                .setMessage("New chat messages with public HTTPS video URLs can be saved to "
                        + "Downloads/Ultgram. This uses Android DownloadManager, not Telegram's "
                        + "media queue. Maximum declared file size: 100 MiB. Direct videos and "
                        + "web pages with standard OG metadata are supported. "
                        + "An experimental on-device extractor also handles public "
                        + "YouTube, Instagram and other supported video-site URLs. "
                        + "Some URLs, DRM and login-only media may still fail. "
                        + "External sites are contacted directly, OUTSIDE Telegram MTProxy; "
                        + "they can see your network IP. Automatically paused when Ultgram "
                        + "native strict proxy is enabled. Existing messages are not scanned. "
                        + "With Wi-Fi-only selected, links received on metered networks are skipped.")
                .setItems(new String[]{
                        "Automatic saving: " + (enabled ? "ON" : "OFF"),
                        "Wi-Fi/unmetered only: " + (wifiOnly ? "ON" : "OFF"),
                        "Downloads queued/saved this installation: "
                                + com.yooshastan.ultmessenger.video.UltVideoAutoSave.queuedCount(account),
                        "Last video attempt: "
                                + com.yooshastan.ultmessenger.video.UltVideoAutoSave.lastResult(account)
                }, (dialog, choice) -> {
                    if (choice == 0) {
                        if (enabled) {
                            com.yooshastan.ultmessenger.video.UltVideoAutoSave.setEnabled(account, false);
                            showDownloadToast("External video auto-save disabled");
                        } else {
                            showSecureDialog(activity, new android.app.AlertDialog.Builder(activity)
                                    .setTitle("Enable external video downloads?")
                                    .setMessage("Ultgram will automatically contact external websites "
                                            + "from new messages without opening links manually. "
                                            + "External HTTP(S) requests do NOT use Telegram's proxy. "
                                            + "Only enable this if you accept that privacy and "
                                            + "data/storage cost. Strict MTProxy mode pauses it.")
                                    .setNegativeButton("Cancel", null)
                                    .setPositiveButton("Enable", (d, w) -> {
                                        com.yooshastan.ultmessenger.video.UltVideoAutoSave.setEnabled(account, true);
                                        showDownloadToast("Auto-save enabled for new video links");
                                    }));
                        }
                    } else if (choice == 1) {
                        com.yooshastan.ultmessenger.video.UltVideoAutoSave.setWifiOnly(account, !wifiOnly);
                        showDownloadToast(!wifiOnly ? "Unmetered only enabled" : "Mobile data allowed");
                    }
                })
                .setPositiveButton("Done", null));
    }

"""

def replace_once(source: str, anchor: str, replacement: str) -> str:
    count = source.count(anchor)
    if count != 1:
        raise ValueError(f"Expected one pinned video-link anchor, found {count}: {anchor[:90]!r}")
    return source.replace(anchor, replacement, 1)

def integrate(root: Path) -> None:
    app_file, downloads_file, strict_file = root / APP, root / DOWNLOADS, root / STRICT
    app, downloads = app_file.read_text(encoding="utf-8"), downloads_file.read_text(encoding="utf-8")
    strict = strict_file.read_text(encoding="utf-8")
    gradle_file, manifest_file = root / GRADLE, root / MANIFEST
    gradle = gradle_file.read_text(encoding="utf-8")
    manifest = manifest_file.read_text(encoding="utf-8")
    if "UltVideoAutoSave.install()" in app or "showVideoLinkSettings()" in downloads:
        raise ValueError("Video links already integrated")
    app_new = replace_once(app, APP_ANCHOR,
        "        com.yooshastan.ultmessenger.video.UltVideoAutoSave.install();\n" + APP_ANCHOR)
    downloads_new = replace_once(downloads, TOOLBAR_ANCHOR,
        TOOLBAR_ANCHOR + '        addToolbarButton(context, toolbar, "Video links", this::showVideoLinkSettings);\n')
    downloads_new = replace_once(downloads_new, METHOD_ANCHOR, SETTINGS_METHOD + METHOD_ANCHOR)
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
              for name in ("UltVideoLinkRules.java", "UltVideoAutoSave.java", "UltVideoSiteDownloader.java")}
    app_file.write_text(app_new, encoding="utf-8")
    downloads_file.write_text(downloads_new, encoding="utf-8")
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

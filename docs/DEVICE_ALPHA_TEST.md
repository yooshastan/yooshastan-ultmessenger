# Ultgram Alpha 0.7 preview — visual identity device test checklist

## Download the APK

1. Open [Android Alpha 0.7 visual identity preview](https://github.com/yooshastan/yooshastan-ultmessenger/actions/workflows/android-alpha.yml?query=branch%3Afeature%2Falpha07-visual-identity).
2. Choose a successful run on `feature/alpha07-visual-identity`.
3. Download `ult-alpha07-preview-foss-android` under **Artifacts**, extract its ARM64 debug APK and verify that it came from your Yooshastan repository.
4. Install on your Pixel 8a or Samsung test device.

Debug artifacts expire in 14 days. CI debug signing keys may differ from your previous install; **do not uninstall a working messenger without planning for reauthentication and any local data**.

## Alpha 0.7 — Ultgram visual identity (new)

1. Inspect the launcher icon on the Pixel 8a home screen and app drawer in both light and dark modes. Confirm the violet folded-wing U, central plane and small stars are recognizable at 48dp, and the outer halo is not clipped by circular masks.
2. Confirm the updated icon appears in Android App Info, Settings and recents; test a second device/launcher if available. Adaptive and legacy variants must both load.
3. Open **Settings → Chat Settings → ⋮ → ULT Presets** and apply **ULT Aurora**. Check the lavender accent, dark chat list, incoming bubbles, outgoing bubbles and text contrast.
4. Repeat with **ULT Obsidian**. Inspect chat list, dialogs, media, long messages, group headers, pinned messages, read receipts and accessibility font scaling.
5. Switch to **Candy Pink**, **Neon Stage** and **ULT Midnight**. Old stored presets must still load and colors should remain legible.
6. Choose **System** and confirm your previously selected Telegram theme returns. Restart the app and verify the chosen preset remains stable.
7. Verify no change to account identity, network behavior or protected content handling.

## Alpha 0.6 — ULT Message Tools (regression)

1. Long-press a normal text or media message in a private conversation and select **ULT: Copy message ID**. Verify the clipboard holds a numeric ID, not the message text.
2. Long-press an incoming group message and select **ULT: Copy sender peer ID**. Telegram uses signed peer IDs: negative IDs can represent channels/chats, and the ID is not a phone number.
3. Test your own outgoing message, a channel post, a forum-topic message, and a media caption. Verify menu entries appear where Telegram exposes metadata.
4. Check a sponsored message: the added message-ID entries should not appear there.
5. Cancel the long-press menu: the clipboard should remain unchanged. Neither action should send, forward, delete, or edit anything.
6. Verify sender-peer copy is not offered when upstream reports no sender peer (`0`), and note that some imported/special messages may expose different identities.
7. Test once in a secret chat if you use one. IDs are local metadata; they are not public links and may differ across devices.

## Alpha 0.5 — ULT Chat Tools (regression)

1. Open a private conversation and choose **⋮ → ULT Chat Tools**.
2. Use **Copy dialog ID** and **Copy user ID**; verify values are copied only when tapped.
3. In a group or channel, check **Copy chat ID**. Signed dialog IDs may be negative; numeric chat IDs may be positive.
4. If the conversation has a public username, **Copy public t.me link** should appear and open that public chat/profile. Private chats without a public username must not show that link.
5. In a forum topic, check **Copy topic ID**. It should not appear when there is no topic.
6. In a secret chat, confirm that no public-link action is offered.
7. Leave the menu without selecting anything: no clipboard write or network action should occur.

These are read-only utilities. They don't forward messages, bypass Premium, expose secret chat content or change Telegram identity. Clipboard content can be read by other apps depending on Android permissions and behavior; treat IDs and links as potentially sensitive.

## Existing-feature regression tests

- Telegram login, text and photo exchange, background notifications and media scrolling.
- **Settings → Chat Settings → ⋮ → ULT Presets**: Aurora, Obsidian, Midnight, Candy Pink, Neon Stage, Minimal, and System restore.
- **Settings → Chat Settings → ⋮ → ULT Chat Layout**: Compact, Comfortable, Minimal Bubbles, then Telegram Default; verify original font size and bubble radius return after restarting.
- Check long messages, captions, stickers, group replies, RTL layouts, large system font and TalkBack.
- **Settings → Data and Storage → Proxy Settings**: ULT Proxy Manager profile naming/editing, proxy availability/ping and privacy dialog. If using Tor mode, verify its selection guard.
- Switch Telegram languages and back; observe for previous language-pack UI freezes.

## Fedora ADB installation and logs

```bash
adb devices -l
adb install -r /absolute/path/to/ult-alpha07-preview-foss-arm64-debug.apk
adb logcat -c
adb logcat -v threadtime -s AndroidRuntime:E ActivityManager:I > ult-alpha07-debug.log
```

Stop logging with Ctrl+C. Review logs for private messages, IDs, phone numbers and proxy endpoints before sharing.

## Still planned

- Per-message actions are limited to local ID copying; advanced message diagnostics remain future work.
- Advanced proxy fail-closed routing and SMS/MMS/RCS companion are not part of Alpha 0.7.
- Firebase-compatible variant requires optional `ULT_FIREBASE_JSON_B64`.
- ULT adds no analytics or advertising; upstream sponsored Telegram content may still exist.

# Ultgram Alpha 0.5 preview — device test checklist

## Download the APK

1. Open [Android Alpha 0.5 chat tools preview](https://github.com/yooshastan/yooshastan-ultmessenger/actions/workflows/android-alpha.yml?query=branch%3Afeature%2Falpha05-chat-tools).
2. Choose a successful run on `feature/alpha05-chat-tools`.
3. Download `ult-alpha05-preview-foss-android` under **Artifacts**, extract its ARM64 debug APK and verify that it came from your Yooshastan repository.
4. Install on your Pixel 8a or Samsung test device.

Debug artifacts expire in 14 days. CI debug signing keys may differ from your previous install; **do not uninstall a working messenger without planning for reauthentication and any local data**.

## Alpha 0.5 — ULT Chat Tools

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
- **Settings → Chat Settings → ⋮ → ULT Presets**: Midnight, Candy Pink, Neon Stage, Minimal, and System restore.
- **Settings → Chat Settings → ⋮ → ULT Chat Layout**: Compact, Comfortable, Minimal Bubbles, then Telegram Default; verify original font size and bubble radius return after restarting.
- Check long messages, captions, stickers, group replies, RTL layouts, large system font and TalkBack.
- **Settings → Data and Storage → Proxy Settings**: ULT Proxy Manager profile naming/editing, proxy availability/ping and privacy dialog. If using Tor mode, verify its selection guard.
- Switch Telegram languages and back; observe for previous language-pack UI freezes.

## Fedora ADB installation and logs

```bash
adb devices -l
adb install -r /absolute/path/to/ult-alpha05-preview-foss-arm64-debug.apk
adb logcat -c
adb logcat -v threadtime -s AndroidRuntime:E ActivityManager:I > ult-alpha05-debug.log
```

Stop logging with Ctrl+C. Review logs for private messages, IDs, phone numbers and proxy endpoints before sharing.

## Still planned

- A fuller message-specific tools menu (e.g. copy message ID) requires separate interaction tests.
- Advanced proxy fail-closed routing and SMS/MMS/RCS companion are not part of Alpha 0.5.
- Firebase-compatible variant requires optional `ULT_FIREBASE_JSON_B64`.
- ULT adds no analytics or advertising; upstream sponsored Telegram content may still exist.

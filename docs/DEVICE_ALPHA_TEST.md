# Ultgram Alpha 0.10a preview — local chat-view device test checklist

## Samsung A36 theme regression fix (new preview)

This branch is a **candidate fix, not yet device-confirmed**. Telegram's theme importer installs a ULT palette into the **day** slot even if it is dark; Samsung/Android system dark mode may immediately replace it with Telegram's separate **night** slot. The ULT chooser previously did not rebuild existing fragment views after import.

1. Download the successful FOSS artifact from [Samsung A36 theme fix builds](https://github.com/yooshastan/yooshastan-ultmessenger/actions/workflows/android-alpha.yml?query=branch%3Afix%2Fsamsung-a36-ult-themes): `ult-alpha10a-themefix-preview-foss-android`. It contains `ult-alpha10a-themefix-preview-foss-arm64-debug.apk`.
2. On **Samsung A36**, enable Android **Settings → Display → Dark mode**. Note your Telegram **Settings → Chat Settings → Auto-Night Mode** and existing day/night themes.
3. Open **Settings → Chat Settings → ⋮ → ULT Presets**. Confirm the choices and the **Theme behavior** explanation. Choose **ULT Aurora**, then **ULT Obsidian**. Each should recolor the chat list and conversation immediately.
4. Restart Ultgram. The preset should remain. Toggle Samsung dark mode off/on; the fixed preset should **not** be replaced. Test a light preset such as Candy Pink while Samsung dark mode is on.
5. Choose **System**. Verify the previous Telegram **day theme, night theme and auto-night setting** return; Android dark-mode changes should again switch Telegram themes where configured.
6. Repeat on **Pixel 8a/GrapheneOS**. If possible, test scheduled/brightness auto-night too. Selecting multiple ULT presets should not overwrite the original snapshot.
7. Confirm Telegram native theme chooser, dialogs, media and notifications. If a toast says Applied but the theme does not change, record **preset, Android light/dark, Telegram Auto-Night Mode, and a redacted screenshot**.

**Important:** A chosen ULT preset pauses Telegram's **in-app** automatic day/night switching; **System** restores the previous configuration. Samsung's OS-wide dark mode is not modified.

## Download the APK

1. Open [Android Alpha 0.10a chat views preview](https://github.com/yooshastan/yooshastan-ultmessenger/actions/workflows/android-alpha.yml?query=branch%3Afeature%2Falpha10-chat-view-presets).
2. Choose a successful run on `feature/alpha10-chat-view-presets`.
3. Download `ult-alpha10a-preview-foss-android` under **Artifacts**, extract its ARM64 debug APK and verify that it came from your Yooshastan repository.
4. Install on your Pixel 8a or Samsung test device.

Debug artifacts expire in 14 days. CI debug signing keys may differ from your previous install; **do not uninstall a working messenger without planning for reauthentication and any local data**.

## Alpha 0.10a — ULT Chat Views (new)

1. In **Chats → ⋮ → ULT: Chat Views**, verify the single-choice list contains **All**, **Unread**, **Groups**, **Channels**, **Bots**, **Unread First**. Default should be **All**.
2. Select **Unread only**. Chats with unread messages or manual unread marks should remain; read conversations should not. Confirm the original order returns after selecting **All**.
3. Select **Groups only** and verify normal groups and megagroups appear but broadcast channels do not. **Channels only** should show broadcast channels; **Bots only** should show bot conversations.
4. Select **Unread first**. Unread conversations should appear ahead of other unpinned chats while the Archive entry and already-pinned chats remain above the rest. Return to All and verify Telegram's normal order.
5. **Archive regression:** enable Alpha 0.8's **Hide Archive row**, cycle through the six views, and open the archive via **ULT: Open Archive**. Verify archived chats remain intact, unread counts persist and no chat was moved.
6. Test native Telegram folder tabs, search, conversation selection/forwarding, reordering pins, swipe gestures, sharing, notifications and new-message arrivals. ULT presets must never alter a different Telegram folder, archive or picker.
7. Enable a non-default view on account A; verify account B remains on All. Restart the app; choices should persist independently. Return both to All.
8. Check display on Pixel 8a/GrapheneOS and Samsung devices with long names, large font, accessibility and no Play Services. All six views must remain responsive.

**Scope:** This is a menu-based filter/sort preview, **not yet a swipeable tab bar**. A native tabs presentation and advanced ordering controls are future steps. The feature does not change Telegram account data.

## Alpha 0.9.1 — ULT Chat Translation (chooser fix + regression)

1. In a chat with messages, open **⋮ → ULT Chat Translation**. The language-choice list **must be visible** (previous Alpha 0.9 showed only the engine explanation and Cancel). Tap **Engine info** to see the engine disclosure, then reopen the menu. Choose **Translate to Korean**, English or another language and verify the built-in translate bar appears. Use the bar to switch between original and translated text. Check a non-English incoming message.
2. If the translation engine is **Telegram default/cloud** or **Alternative HTTP**, expect a confirmation explaining that text leaves the device. Tap **Cancel**: no new translation should start. Repeat and opt in to verify that translation starts.
3. Select **Stop translating this chat**, confirm original text returns, then reopen the chat. Verify the chat-level choice persists appropriately.
4. Test two separate chats: enabling one should not enable the other. Change the target language in one chat and confirm it takes effect.
5. In the Mercurygram translation settings, select **Offline** with **no usable AIDL provider**. Translation should fail closed, clearly report missing local provider, and **never send text through alternative HTTP**, even when the upstream auto-fallback option is enabled.
6. With an installed/bindable offline AIDL provider, confirm offline translation works, and simulate provider failure if possible; no network fallback should occur.
7. Open a **secret chat**: network engines must be blocked, and the offline mode should only work with a usable local provider. Do not share real secret-chat contents in test logs.
8. Verify the Google-free FOSS APK works with no Play Services/microG, including in GrapheneOS. Repeat standard messaging, proxy and archive visibility regressions.

**Privacy caveat:** The new ULT menu requires explicit confirmation for remote engines; the inherited upstream translation controls remain available and can have different disclosure flows. No offline translation model is bundled with this fork. Choosing **Offline** now means strict device-only behavior, which may show unavailable until a compatible provider is installed.

## Alpha 0.8 — Hide Archive row (regression)

1. Open **Chats → ⋮ → ULT: Hide Archive row**. Its checkmark should initially be off.
2. Enable it. The Archive folder row should disappear from the **All Chats** list immediately. Existing chats and their archive state must not change.
3. Reopen **Chats → ⋮**. Choose **ULT: Open Archive**; verify archived conversations and unread messages are still there.
4. Return to All Chats. Verify archived chats have **not** been unarchived, and new messages/notifications still behave normally.
5. Repeat on a second account (if available); the preference must remain independent per account.
6. Restart the app. The preference must persist and the Archive row must remain hidden on that account.
7. Disable **ULT: Hide Archive row** and verify the Archive row returns to the normal list; reopen it and verify contents unchanged.
8. Test custom folders, forwarding/select-chat pickers, search, swipe gestures and pin reordering with both toggle states. The setting should affect only the main All Chats row, not these other workflows.

This is a **local visibility preference**, not an archive deletion, privacy feature or message-security boundary. Archived conversations remain accessible via the menu.

## Alpha 0.7 — Ultgram visual identity (regression)

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
adb install -r /absolute/path/to/ult-alpha10a-preview-foss-arm64-debug.apk
adb logcat -c
adb logcat -v threadtime -s AndroidRuntime:E ActivityManager:I > ult-alpha10a-debug.log
```

Stop logging with Ctrl+C. Review logs for private messages, IDs, phone numbers and proxy endpoints before sharing.

## Still planned

- Per-message actions are limited to local ID copying; advanced message diagnostics remain future work.
- Advanced proxy fail-closed routing, on-device provider packaging and SMS/MMS/RCS companion remain future work; Alpha 0.9 adds per-chat translation UI and strict offline fallback policy.
- Firebase-compatible variant requires optional `ULT_FIREBASE_JSON_B64`.
- ULT adds no analytics or advertising; upstream sponsored Telegram content may still exist.

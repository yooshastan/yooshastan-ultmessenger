# Ultgram Alpha 0.14d+ — one combined feature batch

Baseline: successful, user-accepted Alpha 0.14c FOSS ARM64 `release/alpha14c-combined-rc1`, commit `001ff1c40e77d839574a9ae47267ccbf66c51f71`. The previous release APK is not modified.

## Advanced media management
ULT Downloads long-press on an already downloaded, permitted file adds **Verify saved copy…** beside SHA-256 inspection and Save As. A user-selected Android `ACTION_OPEN_DOCUMENT` file is compared byte-for-byte in effect using SHA-256 and file length to the local source. Streams are bounded to 512 MiB; hashing is on a worker thread; no automatic uploads, broad storage permissions or content-protection bypass. No claim to have verified a destination until the user explicitly selects it. Android document providers can be cloud-backed; the user chooses which file to open.

## Actual appearance controls
Ultgram Settings > Appearance > **Message font and bubble corners** uses the pinned native Telegram `SharedConfig.fontSize`, `SharedConfig.bubbleRadius`, `mainconfig/fons_size` and `MessagesController.getGlobalMainSettings()/bubbleRadius` persistence; calls `Theme.createCommonMessageResources` and rebuilds views. Font 12–30 sp, radius 0–17 dp, Cancel/Apply/Reset. Native ThemeActivity remains available.

## Privacy / local identity and egress capture auditing
**Local account identity cover** displays masked phone/username inside Ultgram Settings by default; opt-in reveal and per-account preference. This is **not** identity masking across Telegram chats, notifications, the drawer, server records or network peers.

**Manual network egress evidence** offers a six-path Wi-Fi/cellular/DNS/CDN/WebView/call capture checklist with copyable redacted status. Marked checks are user-reported evidence, **never** proof of whole-app proxy isolation. The app does not capture packets, enforce DoQ/DoH3, intercept DNS, or change existing native strict proxy behavior.

## QA
Combined debug ARM64 build follows source/JVM checks; check Pixel 8a GrapheneOS and Samsung A36. Verify both matching and deliberately different file hashes, cancel and protected media, theme adjustments/restart, identity cover default/reveal/account isolation, evidence report no secrets, screen privacy, strict proxy good/bad, story downloads, translations and account switching. Preserve original 0.14c APK. Debug signing, source-license audit and public release are separate.

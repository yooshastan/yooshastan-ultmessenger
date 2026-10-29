# Ultgram Alpha 0.14d+ — combined internal RC1

One FOSS ARM64 test build from `release/alpha14def-combined-rc1`, based on user-accepted Alpha 0.14c. Only a **successful full** Android Actions run plus uploaded `ult-alpha14def-combined-foss-android` proves APK packaging. Required contents: `ult-alpha14def-combined-foss-arm64-debug.apk`, `SHA256SUMS`, `BUILD_INFO.txt`. Debug-signed test APK, not public release. Preserve the existing 0.14c installation/data and verify signing certificate before updating.

## Combined Pixel 8a / Samsung A36 QA

1. **Media:** Chats → ULT Downloads → long-press an already downloaded permitted document → Verify saved copy. Select identical file via Android file picker, confirm matching size+SHA-256. Select a deliberately changed copy, confirm mismatch. Cancel picker, clear cache/source, test files above 512 MiB, protected/noforwards/secret chats, provider failure, account changes, screen rotation. Neither file should be edited.
2. **Appearance:** Ultgram Settings → Message font and bubble corners. Change font 12–30 sp, bubble corner 0–17 dp, Apply; inspect real chat messages and app restart. Reset returns to ordinary values; Cancel does not write. Existing paired day/night themes and custom theme preview must survive.
3. **Identity:** Ultgram Settings → Local account identity cover. Confirm masked username/phone by default. Reveal once (local card only), re-cover, switch accounts. This does not hide usernames elsewhere, Telegram servers, or notifications.
4. **Network:** Ultgram Settings → Manual network egress evidence. Mark test categories; copy report. Ensure it says USER-MARKED and NOT ROUTE-VERIFIED, contains no account IDs/credentials/IPs, and carries no false claim of real packet capture. Preserve strict native proxy state and test invalid proxy reconnection and Wi-Fi/cellular.
5. Regression: story Gallery save (eligible only), original Save As/inspect, category navigation, translations, external links, screenshot FLAG_SECURE, multiple accounts, notifications, media and calls where available.

## Security limitations
The app does **not** claim whole-app leak-proof strict proxy routing, DNS protection or DoQ/DoH3, tracker filtering, identity spoofing, story entitlement bypass, bulk exports or verified public-release signing. Manual evidence is not automatic verification.

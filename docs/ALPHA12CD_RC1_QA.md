# Ultgram Alpha 0.12c+d RC1 — combined internal ARM64 device QA

- Source branch: `release/alpha12cd-combined-rc1`
- Baseline: working Alpha 0.12a+b RC1 on Samsung A36 + Pixel 8a, and preserved Alpha 0.11 privacy/proxy implementation.
- Expected artifact after a successful run: `ult-alpha12cd-combined-foss-android`, containing `ult-alpha12cd-combined-foss-arm64-debug.apk`, `SHA256SUMS`, `BUILD_INFO.txt`.
- This is a **debug-signed internal test**. It may not update an installation signed with another key; do not uninstall without protecting local data.

## Acceptance matrix
1. Launch with strict native proxy ON, force close and reopen, send a message and download a photo. Check that invalid proxy settings do not silently fall back.
2. With category bar visible, open Ultgram Settings → Swipe category navigator → Enabled; return to Chats. Swipe left on **↔** and verify next enabled category; swipe right for previous; tap for next.
3. Verify the swipe surface is absent when disabled or category bar hidden. Long-press/swipe chat rows, Telegram folder tabs and use Android edge-back to ensure they still behave normally. Check hidden/reordered categories and per-account isolation.
4. In Category typography select small and large; verify labels change while Android font scaling still applies. Reopen and check persistence, then restore Normal.
5. Long-press a document in a public channel: Copy safe file name should sanitize delimiters; Copy media citation should contain ID, valid UTC and permitted public link, MIME and bytes.
6. In a private chat or secret chat, Copy media citation must **not** invent a public t.me link. The actions must not auto-download or export the file. Clipboard can be exposed to Android/keyboard; use nonsensitive test content.
7. Confirm Alpha 0.12a+b category density and light/dark previews, Alpha 0.11 screenshot privacy dialogs and translation restrictions continue to work.
8. Repeat core launch, navigation, media copy and proxy checks on **Pixel 8a** and **Samsung A36**.

## Excluded / known limitations
No whole-chat-list swipe takeover; the gesture target is dedicated to Ultgram's floating strip. This build does not implement a full download queue manager, local file exports, a custom theme editor, DoQ/DoH3 transport, tracker enforcement, identity anonymization, or premium-service bypass. Native Telegram protocol path remains only partially audited. Maintain prior privacy caveats.

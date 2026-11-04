# Ultgram Alpha 0.13c+d RC1 — Android test candidate

Expected only after a **successful** FOSS ARM64 workflow:
- Branch: `release/alpha13cd-combined-rc1`
- Artifact: `ult-alpha13cd-combined-foss-android`
- Required files: `ult-alpha13cd-combined-foss-arm64-debug.apk`, `SHA256SUMS`, `BUILD_INFO.txt`
- APK: internal debug-signed test candidate, not a production-signed release. Do not uninstall working Alpha 0.13a+b without preserving local data. Cross-signature installs may fail.

## Pixel 8a GrapheneOS + Samsung A36 checklist
1. Open Ultgram Settings after update: the What's New overlay should appear once per installed version; reopen explicitly with top "What's New / build information"; report installed version name/code and candidate source track separately.
2. From the normal Chats overflow menu, choose ULT: Downloads. Confirm correct account's active/recent native transfers and normal open/download/cancel behavior; do not intercept Telegram folder gestures.
3. Long-press a fully downloaded, permitted document; choose the Android picker destination for Save As. Verify byte-for-byte copy, user-cancel no output, failure message and no new broad file permission.
4. Verify incomplete download is rejected; protected/noforwards messages, inaccessible chat restrictions, secret chats and sponsored messages cannot be saved via this path.
5. Rotate or background app in file picker; if selection isn't retained, export should abort safely, not leak another document.
6. Check custom fixed theme colors, original day/night preset restore, category tabs and swipe control, translator, screenshot protection, strict proxy good/bad network behavior, and private message/link handling.
7. Check signature and package version upgrade continuity before any public build; test a clean install only after preserving local data.

## Not included
Whole-app kill switch, verified leak-free DNS/CDN/calls/WebView routing, DoQ/DoH3, tracker enforcement, full file browser or bulk export, identity spoofing, Premium server-side features. The Android Storage Access Framework picker can offer third-party/cloud destinations chosen by user.

# Ultgram Alpha 0.14a+b RC1 — FOSS ARM64 QA

Branch `release/alpha14ab-combined-rc1`; base is user-tested 0.13c+d (unchanged). Artifact **only on successful full Android CI**: `ult-alpha14ab-combined-foss-android`, containing `ult-alpha14ab-combined-foss-arm64-debug.apk`, `SHA256SUMS`, `BUILD_INFO.txt`.

## Test on Pixel 8a GrapheneOS and Samsung A36
1. Preserve the working app and local data. Check APK signing certificate/versionCode compatibility before upgrading; this is an internal debug-signed APK.
2. Open Ultgram Settings > What's New / build information: check Alpha 0.14a+b and verify actual Android package version/code displayed separately.
3. Open Ultgram Settings > Custom ULT theme colors, select preset and valid #RRGGBB values, then **Preview colors (does not apply)**. Check header plus incoming/outgoing bubble samples. Return to chat: active theme must remain unchanged until explicitly applying. Test invalid/low-contrast values and original System/day-night restore.
4. From Chats overflow > ULT: Downloads, long-press a **fully downloaded, permitted document**. Choose **Inspect file and SHA-256** or existing **Save As**. Hash stays local; Copy is explicit. Compare against `sha256sum` of the same test file with user consent. No upload or automatic sharing.
5. Incomplete, protected/noforwards, secret-chat and sponsored messages must be rejected. Hash limit is 512 MiB; try a small file and empty file, and verify limit messaging for a larger file.
6. Confirm Save As picker still works, cancel and errors are safe, and original native open/download/cancel remain unchanged; no broad filesystem permission prompts or bulk export.
7. Regression: category tabs/swipe, FLAG_SECURE, translation, proxy good/bad network, media, notifications, account switching.

## Boundaries
Local-file checksum does **not** verify a separate export into an Android document provider. Clipboard copies are exposed to Android. No whole-app traffic firewall, verified DNS/CDN/call isolation, live DoQ/DoH3, identity masking or tracker enforcement. Public release signing, source/license review and distribution remain separate.

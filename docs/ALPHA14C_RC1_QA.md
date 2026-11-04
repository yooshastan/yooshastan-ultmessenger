# Ultgram Alpha 0.14c RC1 — story save acceptance

Branch `release/alpha14c-combined-rc1` is based on the Alpha 0.14a+b source, retaining previous download inspector, protected Save As, theme preview, chat navigation and strict proxy integration. This is an internal **debug-signed** test candidate, not public release signing.

Only a successful full FOSS ARM64 CI workflow with artifact `ult-alpha14c-combined-foss-android` and files `ult-alpha14c-combined-foss-arm64-debug.apk`, `SHA256SUMS` and `BUILD_INFO.txt` establishes an installable RC.

## Pixel 8a GrapheneOS / Samsung A36

1. Open your own saved photo and video stories; find Gallery save icon immediately to the left of ⋮ in Story Viewer. Tap to save to system Gallery. Confirm file visible, contents intact, and no duplicate or repeated action from one tap.
2. When testing another user's public shareable story, check that the icon appears only if the Telegram account is already Premium and the story permits screenshots/forwards. Non-Premium viewers must not gain new Premium entitlement or bypass server limits.
3. For screenshot-disabled/noforwards stories, unknown protected channels, live video, deleted/skipped/unsupported media, or private/nonshareable other-user stories, no icon should appear; tapping stale targets must do nothing. Check quick switches between stories and multiple accounts.
4. If media is not fully cached, expect Telegram's existing request to download before saving. No new download engine or broad storage permission is added.
5. Repeat original Alpha 0.14a+b QA: Save As, SHA-256 inspector, custom theme preview, chat tabs, translations, FLAG_SECURE and native strict-proxy good/bad connection behavior.
6. Check installed APK signing/versionCode before upgrading; retain a working fallback. The icon uses native Telegram MediaController and must respect the original copyright/content permission settings.

## Not implemented
Unrestricted saving of other people's stories without Premium, protected-media bypasses, mass story archiving, automatic background capture, invisible downloads, DoQ/DoH3, whole-app leak-proof proxy or identity masking.

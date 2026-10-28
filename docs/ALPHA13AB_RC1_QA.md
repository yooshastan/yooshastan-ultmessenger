# Ultgram Alpha 0.13a+b RC1 — Internal ARM64 device test plan

Branch `release/alpha13ab-combined-rc1`. Base: Alpha 0.12c+d RC1 (user-reported functioning), preserves previous native tgnet proxy guard and screen privacy.

Expected artifact **only if CI succeeds**: `ult-alpha13ab-combined-foss-android` with `ult-alpha13ab-combined-foss-arm64-debug.apk`, `SHA256SUMS` and `BUILD_INFO.txt`.

## Acceptance
1. Back up app data if needed. This is a **debug-signed internal test**, not stable, and may fail to upgrade across signing keys. Do not uninstall an existing app without protecting local data.
2. In Ultgram Settings → Custom ULT theme colors, select an original preset, edit **accent** and **outgoing message bubble** `#RRGGBB`, and apply. Verify the chat/action-bar colors change and text stays readable. Reject low contrast / invalid colors with a clear message and keep previous theme.
3. Reopen and verify custom theme persistence. Select an unchanged original ULT preset and confirm the original palette still applies. Check Android night toggle and both original paired presets; System must restore previous theme behavior.
4. Open Ultgram Settings → Download queue and recent files. Confirm the view reflects Telegram FileLoader's active/recent downloads, opens an already downloaded file via Telegram, and allows ordinary tap-based download/cancel. Long-press multi-select is intentionally unsupported in the new wrapper. Test with a normal, non-protected media file.
5. Open Media auto-download controls → Mobile / Wi-Fi / Roaming. Confirm each opens the correct **native Telegram** policy screen. Storage/cache link still opens native manager; no file is deleted without user action.
6. Run 0.12c+d regression: category swipe pad/typography, metadata citation, screenshot protection and strict proxy cold-start / bad-proxy behavior. Also confirm translation, photo/file download and ordinary chat behavior.
7. Repeat on **Pixel 8a GrapheneOS** and **Samsung A36** and record any crash/logs before promoting RC.

## Exclusions and privacy
Full Save As, file manager, automatic export, whole-app firewall, DoQ/DoH3, tracker enforcement and identity anonymization **are not implemented**. Telegram's native download view enforces Telegram's protected-media behavior; Ultgram does not bypass restrictions. Debug APK is not production-signed, and proxy leak isolation remains unverified for Java/HTTP, DNS, CDN/media, WebViews and calls.

# Ultgram Alpha 0.13 — personalization and media milestone

Base: `release/alpha12cd-combined-rc1`, commit `d9aeb676`, reported functionally sound by user. Do not modify that working candidate.

## Alpha 0.13a — implemented in first feature batch
- Fixed custom themes: select one of the six original ULT palettes; edit **accent** and **outgoing message bubble** using strict #RRGGBB fields. Contrast rules: accent/background >=3.0; message text/outgoing bubble >=4.5. Calls the existing Telegram-native theme-file adapter with a **different filename and theme name**, preserving the immutable original preset and standard auto day/night pairing.
- Native media controls: jump to upstream Telegram's mobile, Wi-Fi or roaming auto-download settings and existing storage/cache manager rather than installing a second downloader or reading private storage.
- Identity/safety scope: an explicit privacy limitations explanation; no deceptive account identity spoofing or new network enforcement.
- Local policy tests and fast CI. Full pinned Android build and Pixel/Samsung tests still pending.

## Planned Alpha 0.13b/c/d (not implemented yet)
- Review pinned Mercurygram downloads and FileLoader integration to implement a real visible queue/status screen using existing download state. Do not duplicate transport, silently download, or bypass protected content.
- Permission-aware Save As with Android Storage Access Framework, user opt-in, supported already-downloaded media only; avoid exposing private/secret content.
- Improved chat-list layout customization and a safe native preview/reset/rollback path for theme editing.
- Network source-path audit, privacy hardening and device capture checks, without claiming anonymous identity, leak proof, working DoQ/DoH3, tracker enforcement or Premium bypass.

## Release gates
Fast contract tests, then one combined ARM64 FOSS native compile and integrity metadata. Pixel 8a GrapheneOS and Samsung A36 manual QA: custom theme apply/restore + readable bubbles + device night toggles; archive/chat navigation; native auto-download settings; strict proxy cold start, screenshot privacy, translations, media, voice call and download behavior. Confirm APK signature and upgrade compatibility, open-source notices, privacy policy and signed release procedure before public release.

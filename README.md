# Yooshastan ULT Messenger

**Who is your Telegram client bias?**

An experimental, independent Android messaging project from Yooshastan Studios. Inspired by K-pop's *ultimate bias* and the flexibility of alternative Telegram clients.

> **Alpha 0.10c proxy diagnostics preview:** The `feature/alpha10c-proxy-diagnostics` branch adds an opt-in **read-only** connection diagnostics dialog to named proxy profiles. Its FOSS ARM64 APK will be available in [GitHub Actions](https://github.com/yooshastan/yooshastan-ultmessenger/actions/workflows/android-alpha.yml?query=branch%3Afeature%2Falpha10c-proxy-diagnostics) after CI passes as `ult-alpha10c-preview-foss-android`. See the [proxy/DNS audit](docs/PROXY_DNS_AUDIT.md) and [device checklist](docs/DEVICE_ALPHA_TEST.md).

## Goals
- Telegram Android compatibility based on the official GPL-2.0 client.
- Nullgram-inspired appearance; selected Nekogram, Goregram, and Plus Messenger enhancements.
- Built-in MTProxy/SOCKS5 management.
- Free client-side enhancements without bypassing Telegram Premium entitlements.
- No advertising or tracking added by Yooshastan. Official Telegram sponsored messages remain supported as required.
- Optional SMS/MMS inbox and experimental Google Messages RCS companion.

## Roadmap
See the [master feature inventory — shipped vs. remaining](docs/FEATURE_MATRIX.md), [current product roadmap](docs/PRODUCT_ROADMAP.md), [Alpha 0.9 device test plan](docs/DEVICE_ALPHA_TEST.md), and [architecture](docs/ARCHITECTURE.md).

The Samsung A36 fix makes ULT theme selection override automatic night switching, refreshes visible screens, and restores previous day/night/auto-night settings on **System**. The fixed-theme behavior has been device-tested on Samsung A36; Pixel 8a was also reported improved.

Alpha 0.10b adds an explicit **Follow phone light/dark** or **Keep fixed colors** choice after selecting a preset. Original light/dark companion palettes are provided for Candy Pink, Aurora, Obsidian, Midnight, Neon Stage, and Minimal. **System** restores the previous Telegram appearance and auto-night setting. The initial paired build ran on a user's device, but Telegram's **in-app Night Mode** toggle reverted to standard appearance. This patch updates the native manual toggle targets; the FOSS native build passed and the user confirmed all six ULT palettes work in both day and night mode.

Alpha 0.10a adds **Chats → ⋮ → ULT: Chat Views**, an account-specific local display control for All, Unread, Groups, Channels, Bots and Unread First. It filters/sorts displayed lists only; server-side archive, pins and folders remain untouched. Alpha 0.9.1 repaired the language-choice dialog and the English translation flow has been device-tested. Alpha 0.9 adds **⋮ → ULT Chat Translation** with per-chat language choices and a remote-engine consent prompt. Mercurygram's pre-existing offline AIDL translator is now **strictly local**: missing or failed providers never silently fall back to HTTP. Secret chats require the offline provider. The Alpha 0.8 hide-archive control remains included. Firebase remains optional.

Alpha 0.10c begins with **Connection diagnostics** in **ULT Proxy Manager → saved profile info → Connection diagnostics**. This exposes the app's existing proxy selection, native Telegram connection state, and cached reachability check without logging secrets, resolving a new hostname or modifying routing. **Strict DNS/DoQ and fail-closed proxy routing are not implemented**; see [transport/DNS audit](docs/PROXY_DNS_AUDIT.md).

## Disclaimer
Not affiliated with Telegram. Uses the Telegram API; a project-specific API ID is required for distribution. The application must comply with Telegram's [API terms](https://core.telegram.org/api/terms). GPL licensing and corresponding-source obligations apply to derivative code.

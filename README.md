# Yooshastan ULT Messenger

**Who is your Telegram client bias?**

An experimental, independent Android messaging project from Yooshastan Studios. Inspired by K-pop's *ultimate bias* and the flexibility of alternative Telegram clients.

> **Samsung A36 theme regression preview:** The `fix/samsung-a36-ult-themes` branch retains Alpha 0.10a features and builds a Google-free ARM64 debug APK in [GitHub Actions](https://github.com/yooshastan/yooshastan-ultmessenger/actions/workflows/android-alpha.yml?query=branch%3Afix%2Fsamsung-a36-ult-themes). Download `ult-alpha10a-themefix-preview-foss-android` from a **successful** run. The repository ZIP is a bootstrap; use the [preparation guide](docs/ANDROID_STUDIO.md) for complete Android Studio source.

## Goals
- Telegram Android compatibility based on the official GPL-2.0 client.
- Nullgram-inspired appearance; selected Nekogram, Goregram, and Plus Messenger enhancements.
- Built-in MTProxy/SOCKS5 management.
- Free client-side enhancements without bypassing Telegram Premium entitlements.
- No advertising or tracking added by Yooshastan. Official Telegram sponsored messages remain supported as required.
- Optional SMS/MMS inbox and experimental Google Messages RCS companion.

## Roadmap
See the [master feature inventory — shipped vs. remaining](docs/FEATURE_MATRIX.md), [current product roadmap](docs/PRODUCT_ROADMAP.md), [Alpha 0.9 device test plan](docs/DEVICE_ALPHA_TEST.md), and [architecture](docs/ARCHITECTURE.md).

The Samsung A36 fix makes ULT theme selection override automatic night switching, refreshes visible screens, and restores previous day/night/auto-night settings on **System**. This is a device-test candidate, not yet verified on an A36.

Alpha 0.10a adds **Chats → ⋮ → ULT: Chat Views**, an account-specific local display control for All, Unread, Groups, Channels, Bots and Unread First. It filters/sorts displayed lists only; server-side archive, pins and folders remain untouched. Alpha 0.9.1 repaired the language-choice dialog and the English translation flow has been device-tested. Alpha 0.9 adds **⋮ → ULT Chat Translation** with per-chat language choices and a remote-engine consent prompt. Mercurygram's pre-existing offline AIDL translator is now **strictly local**: missing or failed providers never silently fall back to HTTP. Secret chats require the offline provider. The Alpha 0.8 hide-archive control remains included. Firebase remains optional.

## Disclaimer
Not affiliated with Telegram. Uses the Telegram API; a project-specific API ID is required for distribution. The application must comply with Telegram's [API terms](https://core.telegram.org/api/terms). GPL licensing and corresponding-source obligations apply to derivative code.

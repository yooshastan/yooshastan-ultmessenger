# Yooshastan ULT Messenger

**Who is your Telegram client bias?**

An experimental, independent Android messaging project from Yooshastan Studios. Inspired by K-pop's *ultimate bias* and the flexibility of alternative Telegram clients.

> **Alpha 0.10b paired ULT themes preview:** The `feature/alpha10b-paired-ult-themes` branch builds a Google-free ARM64 APK in [GitHub Actions](https://github.com/yooshastan/yooshastan-ultmessenger/actions/workflows/android-alpha.yml?query=branch%3Afeature%2Falpha10b-paired-ult-themes). Once the FOSS job passes, download `ult-alpha10b-preview-foss-android`. The repository ZIP is a bootstrap; use the [Android Studio preparation guide](docs/ANDROID_STUDIO.md) for the complete pinned source.

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

Alpha 0.10b adds an explicit **Follow phone light/dark** or **Keep fixed colors** choice after selecting a preset. Original light/dark companion palettes are provided for Candy Pink, Aurora, Obsidian, Midnight, Neon Stage, and Minimal. **System** restores the previous Telegram appearance and auto-night setting. Paired mode is a CI/device-test preview, not yet validated on Samsung or GrapheneOS.

Alpha 0.10a adds **Chats → ⋮ → ULT: Chat Views**, an account-specific local display control for All, Unread, Groups, Channels, Bots and Unread First. It filters/sorts displayed lists only; server-side archive, pins and folders remain untouched. Alpha 0.9.1 repaired the language-choice dialog and the English translation flow has been device-tested. Alpha 0.9 adds **⋮ → ULT Chat Translation** with per-chat language choices and a remote-engine consent prompt. Mercurygram's pre-existing offline AIDL translator is now **strictly local**: missing or failed providers never silently fall back to HTTP. Secret chats require the offline provider. The Alpha 0.8 hide-archive control remains included. Firebase remains optional.

## Disclaimer
Not affiliated with Telegram. Uses the Telegram API; a project-specific API ID is required for distribution. The application must comply with Telegram's [API terms](https://core.telegram.org/api/terms). GPL licensing and corresponding-source obligations apply to derivative code.

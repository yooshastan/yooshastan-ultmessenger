# Yooshastan ULT Messenger

**Who is your Telegram client bias?**

An experimental, independent Android messaging project from Yooshastan Studios. Inspired by K-pop's *ultimate bias* and the flexibility of alternative Telegram clients.

> **Alpha 0.8 preview:** The `feature/alpha08-archive-visibility` branch builds a Google-free ARM64 debug APK in [GitHub Actions](https://github.com/yooshastan/yooshastan-ultmessenger/actions/workflows/android-alpha.yml?query=branch%3Afeature%2Falpha08-archive-visibility). Select a **successful** run and download the `ult-alpha08-preview-foss-android` artifact. The repository ZIP is a bootstrap, not the complete Android Studio checkout: use the [preparation guide](docs/ANDROID_STUDIO.md) to fetch and patch the pinned source.

## Goals
- Telegram Android compatibility based on the official GPL-2.0 client.
- Nullgram-inspired appearance; selected Nekogram, Goregram, and Plus Messenger enhancements.
- Built-in MTProxy/SOCKS5 management.
- Free client-side enhancements without bypassing Telegram Premium entitlements.
- No advertising or tracking added by Yooshastan. Official Telegram sponsored messages remain supported as required.
- Optional SMS/MMS inbox and experimental Google Messages RCS companion.

## Roadmap
See the [master feature inventory — shipped vs. remaining](docs/FEATURE_MATRIX.md), [current product roadmap](docs/PRODUCT_ROADMAP.md), [Alpha 0.8 device test plan](docs/DEVICE_ALPHA_TEST.md), and [architecture](docs/ARCHITECTURE.md).

Alpha 0.8 introduces an opt-in, per-account **Hide Archive row** control under **Chats → ⋮**. It affects the main chat-list display only; **ULT: Open Archive** remains available, and it does not unarchive, delete or move messages. The Firebase-compatible build still depends on optional Firebase configuration.

## Disclaimer
Not affiliated with Telegram. Uses the Telegram API; a project-specific API ID is required for distribution. The application must comply with Telegram's [API terms](https://core.telegram.org/api/terms). GPL licensing and corresponding-source obligations apply to derivative code.

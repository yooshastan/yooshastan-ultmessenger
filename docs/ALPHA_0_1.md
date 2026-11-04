# Alpha 0.1 — first prototype

## Definition of done
- [ ] Pin official Telegram Android source revision and fetch recursive submodules.
- [ ] Reproduce an unmodified upstream Android build.
- [ ] Rename app to **Yooshastan ULT Messenger** and application ID to `com.yooshastan.ultmessenger`.
- [ ] Replace official app icon, release keystore, Firebase config, and app API credentials with project-specific values.
- [ ] Keep official Telegram installed in parallel on Pixel 8a.
- [ ] Test login/logout, DM and group messages, attachments, notifications, and calls.
- [ ] Add a disabled-by-default Yooshastan settings entry and feature switches.
- [ ] Create CI build with no private credentials in git; upload debug APK artifacts.
- [ ] Release GPL-compliant corresponding source alongside APK.

## Current checkpoint
GitHub bootstrap scaffolding only. No Telegram source or APK is present. The CI workflow validates scaffolding; it does **not** compile Telegram yet.

## Upstream requirements (per its current README)
Android Studio 2025.1.4, Android SDK 36, NDK 27.2.12479018. Review exact requirements at the pinned upstream revision.

## Local upstream checkout
```bash
git clone --recursive --shallow-submodules https://github.com/DrKLO/Telegram.git telegram-upstream
```
Do not use the sample upstream API ID or dummy keystore in a released app. Follow the upstream build instructions and replace project configuration before distributing.

# Ultgram Alpha 0.14 precise cache size — combined internal RC1

Branch `release/alpha14cache-combined-rc1`; builds on successfully compiled Alpha 0.14 stabilization and the user-tested Alpha 0.14d+. Preserve installed app and existing working APK.

## Features
- Ultgram Settings → **Storage and Cache** → **Custom maximum cache size**: 2–32 GB in 1 GB steps, e.g. 8/10/12 GB. Displays saved and proposed limits separately. Only Apply writes native Telegram `cache_limit` preference. Separate No Limit and Cancel actions.
- **Telegram storage usage and cleanup** shortcut opens existing `CacheControlActivity`; original coarse selector includes the exact selected custom limit so it no longer falsely shows Unlimited.
- Existing cache cleanup mechanism remains Telegram's own scheduled maintenance. Selecting a limit does not immediately delete existing cached files or provide a strict real-time disk quota. The upstream preference value 1 means 300 MB, not 1 GB; custom range begins at 2 GB.

## CI and device acceptance
1. Full FOSS ARM64 Android CI passes source tests and Java native compilation; verifies signer/package identity using `apksigner` and `aapt`, produces artifact `ult-alpha14cache-combined-foss-android` with APK, SHA256SUMS, BUILD_INFO.txt and SIGNING_INFO.txt.
2. On Pixel 8a GrapheneOS and Samsung A36, select 8, 10, 12 GB; reopen custom picker and native Storage Usage; check values persist correctly and do not appear as No Limit. Cancel/No Limit should behave independently.
3. Confirm existing media stays after choosing a new maximum; cleanup is done by normal Telegram maintenance rather than an immediate file purge. Check low-storage behavior, notifications and local downloads.
4. Compare candidate signer and version code with **currently installed** APK before attempting update. Do not uninstall working app if signature is incompatible.
5. Regression: strict native proxy valid/invalid, user chats, stories Gallery saving, original downloaded media inspect/Save As and protected-media refusal.

Security: no new storage permissions, filesystem scanning, DNS/transport changes, identity impersonation, protected-content bypass or proven whole-app egress isolation. Public-release signing/license/privacy work still pending.

# Ultgram Alpha 0.14 cache and story-save fixes — combined RC2

This is one FOSS ARM64 test candidate from `release/alpha14cache-storyfix-rc2`, based on the successfully compiled previous cache RC1. Existing working APKs and strict native proxy remain unmodified.

## What changed

1. **Custom cache 2–32 GB:** In Ultgram Settings > Storage and Cache, set 8/10/12 GB or any whole GB (2–32). Native Telegram `cache_limit` stores this preference.
2. **Run cache maintenance now:** User must confirm an explicit action. Original `AutoDeleteMediaTask.run()` remains a throttled 24-hour check; a `run(true)` path schedules the same Telegram cleanup earlier without introducing a new cleaner. It may remove age-expired cached media under existing Keep Media rules. Existing Keep Forever/in-use media can prevent achieving exact maximum. Confirmation is required.
3. **Stories:** The viewer's Gallery icon now initiates Telegram `FileLoader` when an eligible own or public/shareable unprotected photo/video story is not cached, observes the specific fileLoaded/fileLoadFailed callback and saves the completed local file through Telegram `MediaController`. Non-Premium viewers can save public share-eligible media already available to their current Telegram account without impersonating Premium or accessing a restricted server resource. Protected/noforwards/unknown channel/private/nonshareable/live/skipped/expired stories are not accessible via this control. Story/account switch invalidates the pending local save.

## Device acceptance (Pixel 8a GrapheneOS and Samsung A36)

- Set 10 GB, reopen both Ultgram slider and native Storage Usage. Tap **Run cache maintenance now**, read deletion warning, test Cancel then Confirm, verify the remaining cache in Storage Usage after maintenance finishes. Do not assume immediate quota or delete private content beyond Telegram's original behavior.
- Own photo and video story where file already cached: confirm Gallery save.
- Own photo/video or public/shareable other-person story **not yet cached**: tap Gallery icon, observe native download then local save; no missing-file `PleaseDownload` loop.
- Non-Premium public/shareable story: icon present (no Premium spoofing). Protected/story noforwards/private and expired: no icon. Switch accounts or stories mid-download: old file never saved under new account.
- Test airplane mode, invalid proxy failure, and restoration; strict native proxy must not silently fall back. Test existing chats, translations, message media, file inspector/Save As, themes.
- Check Android signing certificate and versionCode against currently installed APK before an in-place upgrade. Do not uninstall a working app to resolve a signer mismatch.

## Build requirements

Full Android GitHub Actions workflow must succeed and produce unexpired `ult-alpha14cache-storyfix-foss-android` with APK `ult-alpha14cache-storyfix-foss-arm64-debug.apk`, SHA256SUMS, BUILD_INFO.txt and SIGNING_INFO.txt. Debug-signed internal candidate; not a verified public release or proof of whole-app leak-free network routing.

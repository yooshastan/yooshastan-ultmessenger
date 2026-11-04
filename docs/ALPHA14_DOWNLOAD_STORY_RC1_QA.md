# Ultgram Alpha 0.14 Downloads Control + Story Menu RC1

This is one FOSS ARM64 internal test build from `release/alpha14download-story-rc1`, built on the successfully compiled cache + story downloader RC2. Keep the previous installed app intact until Android signing and versionCode upgrade checks are completed.

## Download controls
- Ultgram Settings → Downloads: **Pause all**, **Resume all**, **Network rules** for mobile data/Wi-Fi/roaming, and explicitly confirmed **Clear history**. Clear history uses Telegram's `DownloadController.clearRecentDownloadedFiles` (history only), not `deleteRecentFiles` (deletes file data). Original file list and download queue remain Telegram's.
- Long-press a queued document for individual Pause/Resume or Move to top; existing protected-media Inspect/SaveAs/Verify eligibility remains.
- Native Telegram auto-download rules cover media type and download size but are NOT manual-transfer blocking or a proxy/VPN kill switch.

## Story lock clarity
- Original Telegram `Save to Gallery` with Premium lock remains untouched, including its Premium subscription prompt.
- New separate `⋮ → ULT: Download story` action appears for an accessible story and runs native FileLoader-to-Gallery save only for eligible own or public/shareable, unprotected photo/video stories. If ineligible, the menu reads `ULT: Download unavailable`, with a toast explaining missing media, expired/live content, restricted forwarding or unavailable public-sharing status.
- Existing small Gallery icon remains. Missing permitted media is downloaded through native FileLoader, with account+story binding on completion. No support for private/restricted stories or an entitlement bypass.

## Device QA
1. Confirm story menu for user's own photo/video, other public shareable photo/video, protected/nonpublic/expired/live stories, both cached and uncached, and observe correct save or clear explanation. Test network failure, account and story switching during download.
2. Queue several documents; test Pause all/Resume all, long-press per-file pause/resume, Move to top, regular item taps, Wi-Fi/mobile/roaming rules and restarting app.
3. Confirm Clear history removes only Recent list entries, not disk files or active transfers; Cancel does nothing.
4. Regression: 2–32GB cache picker, confirmed one-off maintenance, strict native proxy good/bad and reconnect, story gallery, translations, themes, document inspection and signed installation upgrade.
5. Require successful full Android build, `ult-alpha14download-story-foss-android` artifact with `ult-alpha14download-story-foss-arm64-debug.apk`, SHA256SUMS, BUILD_INFO.txt, SIGNING_INFO.txt. Debug-signed internal candidate only. Compare APK signer and versionCode to currently installed version before upgrading; do not uninstall working app to force a mismatch.

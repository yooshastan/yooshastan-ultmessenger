# Ultgram Alpha 0.14c — story saving shortcut

Release candidate: `release/alpha14c-combined-rc1`; FOSS ARM64 debug artifact `ult-alpha14c-combined-foss-android` with APK, SHA256SUMS and BUILD_INFO.txt **only after successful full native CI**. Test notes: [Alpha 0.14c RC1 QA](ALPHA14C_RC1_QA.md).

This stage adds a visible Gallery/download icon immediately beside the story's ⋮ menu, available only where native Telegram's saving permissions allow it.

**Supported (updated):** User-owned, available unprotected photo/video stories; other users' public/share-eligible stories the current account can already view, including non-Premium accounts. A user tap initiates the native Telegram FileLoader download when media is not yet cached. It uses the existing Telegram `FileLoader` for missing permitted local media and `MediaController.saveFile()` to save photo/video to Gallery/Photos. No external downloader, scraping, alternate media request or new file permissions. It does not alter Telegram server entitlements.

**Intentionally unsupported:** story with noforwards/screenshots disabled, inaccessible/private/nonshareable story, unknown protected channel, deleted/skipped or live stories, unsupported media. If media is not cached, a user tap initiates `FileLoader`, then saves the resulting file after the matching fileLoaded callback; failures are shown and account/story switches cancel the save observer. Expired/nonshareable other-user stories remain unavailable. The quick action is refreshed when the current story changes.

**Why this differs from stock Telegram:** Visible one-tap control on the story viewer instead of finding the Save-to-Gallery action in the overflow menu. It does **not** unlock server Premium entitlements and does not bypass protected media.

**Implementation:** `scripts/integrate-story-save.py` patches pinned Mercurygram `PeerStoriesView.java` using fail-closed exact anchors and installs Android-free `UltStorySavePolicy.java`. App-owned settings include a discovery note. Existing 0.14a+b source, strict proxy and network integrations are unchanged.

**QA prior to release:** Own photo/video, Premium public story photo/video, missing cache, non-Premium/no-fwd/expired/private/live/unknown channel, multiple accounts, screenshot-protected stories, autoplay and touch targets on Pixel 8a GrapheneOS and Samsung A36. Light source/JVM checks are not native Android validation. Preserve working RC and run one combined FOSS ARM64 candidate after integration review.

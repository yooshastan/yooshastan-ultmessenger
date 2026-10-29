# Ultgram Alpha 0.14c — story saving shortcut

This stage adds a visible Gallery/download icon immediately beside the story's ⋮ menu, available only where native Telegram's saving permissions allow it.

**Supported:** User-owned, available unprotected photo/video stories; other users' public, share-eligible stories if the account is Premium. It uses the existing `PeerStoriesView.saveToGallery()` and `MediaController.saveFile()` path, which saves photo/video to Gallery/Photos. No external downloader, scraping, alternate media request or new file permissions.

**Intentionally unsupported:** Non-Premium save of another account's story, story with noforwards/screenshots disabled, inaccessible/unknown protected channel, deleted/skipped or live stories, unsupported media. If the underlying story file is not yet cached locally, native Telegram's `PleaseDownload` guidance is shown. Expired/nonshareable other-user stories remain unavailable. The quick action is refreshed when the current story changes.

**Why this differs from stock Telegram:** Visible one-tap control on the story viewer instead of finding the Save-to-Gallery action in the overflow menu. It does **not** unlock server Premium entitlements and does not bypass protected media.

**Implementation:** `scripts/integrate-story-save.py` patches pinned Mercurygram `PeerStoriesView.java` using fail-closed exact anchors and installs Android-free `UltStorySavePolicy.java`. App-owned settings include a discovery note. Existing 0.14a+b source, strict proxy and network integrations are unchanged.

**QA prior to release:** Own photo/video, Premium public story photo/video, missing cache, non-Premium/no-fwd/expired/private/live/unknown channel, multiple accounts, screenshot-protected stories, autoplay and touch targets on Pixel 8a GrapheneOS and Samsung A36. Light source/JVM checks are not native Android validation. Preserve working RC and run one combined FOSS ARM64 candidate after integration review.

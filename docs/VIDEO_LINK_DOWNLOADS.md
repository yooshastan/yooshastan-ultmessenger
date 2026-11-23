# Ultgram: auto-save video links (experimental Beta 3.3)

This opt-in feature observes **newly received** Telegram messages while Ultgram is running and examines their visible HTTPS links (up to three per message). It does not use a bot, send chat contents to an extraction service, or ask the Telegram server to fetch external media.

**User flow:** Ultgram Settings → Downloads → Video links → Automatic saving → Enable. Defaults: OFF, unmetered/Wi-Fi-only ON, file size must be known and no greater than 100 MiB. Downloaded files are queued using Android DownloadManager to the device's **Downloads/Ultgram** directory, separate from Telegram's native download queue. Android can finish an already queued transfer after Ultgram closes.

**Supported in this first pass:**
- Public HTTPS video files returned as \`video/*\` (or octet-stream for known video file extensions).
- Public HTTPS pages exposing a standard \`og:video\` / \`og:video:secure_url\` / \`twitter:player:stream\` URL that resolves to a real video file with a declared size.
- HTTPS redirects to public hosts with public DNS addresses.

**Not supported/guaranteed:** YouTube, Instagram, TikTok, X, or other JavaScript/streaming pages that do not expose a public directly downloadable video URL; DASH/HLS playlists; DRM; private/authenticated content; HTTP; unknown-size files. This is *not yet* a universal yt-dlp replacement. File-size checking uses server-declared sizes and is not a hard byte-accurate limit on untrusted servers.

## Privacy and safeguards

External link inspection and downloads use Android HTTP(S)/DownloadManager, **not Telegram's MTProto proxy**. A remote website can learn the network IP and that a link appeared in the user's chat. The feature is explicitly consent-gated and will not inspect messages when the Ultgram native strict proxy preference is on; it checks again before enqueueing. Android VPN behavior depends on system routing and split-tunnel policies and is not guaranteed by the app.

Secret chats, sponsored content and messages marked \`noforwards\` are excluded. This is not a bypass for protected media or disappearing content. Only URLs in new message text and explicit text-URL entities are considered; old chat history is never scanned. No URL or token is logged, and queued transfer IDs are not transmitted to Yooshastan services.

Only allowed public HTTPS hosts are probed. Non-public DNS answers, local/private IPs, userinfo URLs, HTTP, custom ports, and unknown file lengths are rejected. At most two background resolver tasks run concurrently and up to 1000 message-link fingerprints are deduplicated locally. These safeguards reduce risk but are not a network sandbox.

**Wi-Fi-only behavior:** If a new link arrives while the active network is metered, the URL is skipped instead of being saved for later. For links already queued, Android DownloadManager enforces the unmetered-only preference. Turning off Wi-Fi-only allows mobile data; roaming is still disallowed.

## Before a release

1. CI source contracts and Android build must pass for the pinned Mercurygram revision.
2. On-device: enable/disable, direct MP4, web page OG video, redirect, 404, unknown size, oversize, duplicate, metered/offline behavior.
3. Confirm strict proxy ON causes **no external probe**, including mode changes during resolution.
4. Verify secret/protected messages are not downloaded and Downloads/Ultgram files can be opened and deleted using normal Android tools.
5. Verify the APK package identity, versionCode and certificate against the installed Beta 3.2 before trying an upgrade.

**Follow-up:** Evaluate a sandboxed, maintained multi-site extractor for social/streaming sites. That is a separate, more complex addition, not a capability of this first APK.

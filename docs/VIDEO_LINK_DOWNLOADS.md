# Ultgram: auto-save video links (experimental Beta 3.4.1)

This opt-in feature observes **newly received** Telegram messages while Ultgram is running and examines their visible HTTPS links (up to three per message). It does not use a bot, send chat contents to an extraction service, or ask the Telegram server to fetch external media.

**User flow:** Ultgram Settings → Downloads → Video links → **Auto-save: OFF** → **Enable**. The first screen lists controls immediately, with privacy details behind the separate **Privacy** button; toggles reopen the short dialog to show updated state. Defaults: OFF, unmetered/Wi-Fi-only ON, file size must be known and no greater than 100 MiB. Downloaded files are queued using Android DownloadManager to the device's **Downloads/Ultgram** directory, separate from Telegram's native download queue. Android can finish an already queued transfer after Ultgram closes.

**Supported in this first pass:**
- Public HTTPS video files returned as \`video/*\` (or octet-stream for known video file extensions).
- Public HTTPS pages exposing a standard \`og:video\` / \`og:video:secure_url\` / \`twitter:player:stream\` URL that resolves to a real video file with a declared size.
- HTTPS redirects to public hosts with public DNS addresses.

**Experimental site extraction in Beta 3.4:** Public YouTube/Instagram and selected other video domains use embedded yt-dlp-android 0.18.1 with FFmpeg, app-owned temporary storage, and a final MediaStore export to Downloads/Ultgram. The extractor runs ON DEVICE, without a bot or external extractor service. Real-world success depends on the provider; do not claim Instagram/YouTube compatibility until tested on the phone. Site-specific bot protections, authentication, JavaScript changes, unavailable formats, or DRM may prevent downloads. The library's bundled yt-dlp/Python version may require updates after device testing.

**Limits:** Site downloads combine yt-dlp's max-filesize option with a 250 ms workspace watchdog and 100 MiB final copy limit. Direct-file DownloadManager transfers still rely on server-declared sizes; that is not a hard downloaded-byte limit.

## Privacy and safeguards

External link inspection and downloads use Android HTTP(S)/DownloadManager, **not Telegram's MTProto proxy**. A remote website can learn the network IP and that a link appeared in the user's chat. The feature is explicitly consent-gated and will not inspect messages when the Ultgram native strict proxy preference is on; it checks again before enqueueing. Android VPN behavior depends on system routing and split-tunnel policies and is not guaranteed by the app.

Secret chats, sponsored content and messages marked \`noforwards\` are excluded. This is not a bypass for protected media or disappearing content. Only URLs in new message text and explicit text-URL entities are considered, including newly sent messages after server acknowledgement; old chat history is never scanned. No URL or token is logged, and queued transfer IDs are not transmitted to Yooshastan services.

Only allowed public HTTPS hosts are probed. Non-public DNS answers, local/private IPs, userinfo URLs, HTTP, custom ports, and unknown file lengths are rejected. At most two background resolver tasks run concurrently and up to 1000 message-link fingerprints are deduplicated locally. These safeguards reduce risk but are not a network sandbox.

**Wi-Fi-only behavior:** If a new link arrives while the active network is metered, the URL is skipped instead of being saved for later. For links already queued, Android DownloadManager enforces the unmetered-only preference. Turning off Wi-Fi-only allows mobile data; roaming is still disallowed.

## Before a release

1. CI source contracts and Android build must pass for the pinned Mercurygram revision.
2. On-device: enable/disable, direct MP4, web page OG video, redirect, 404, unknown size, oversize, duplicate, metered/offline behavior.
3. Confirm strict proxy ON causes **no external probe**, including mode changes during resolution.
4. Verify secret/protected messages are not downloaded, and enabling native strict cancels incomplete external transfers while preserving completed videos. Check Downloads/Ultgram using normal Android tools.
5. Verify the APK package identity, versionCode and certificate against the installed Beta 3.2 before trying an upgrade.

**Release gate:** Beta 3.4 is for internal QA only. Strict native proxy requests cancellation of active yt-dlp subprocesses on a best-effort basis, but this is NOT a system VPN kill switch and cannot guarantee zero packets after a mode switch. Dependency licenses, privacy/routing and cancellation need independent device review. Keep signed public releases blocked until checks pass. Automatic link sanitization remains deferred as GitHub issue #70.


## Separate link-sanitizer option in Beta 3.4.1

Under **Ultgram Settings → Privacy and connection → Link sanitizer**, enable conservative OFF-by-default tracking-parameter cleaning before sending ordinary text. Processing occurs entirely on the device, not through a cleaning API. Known share identifiers such as Instagram `bqrl` and YouTube `si` are removed while video IDs, YouTube timestamps, and non-tracking query parameters remain. Authenticated/signed URLs are unchanged. Rich text containing styled entities may be skipped. A cleaned message omits its precomputed URL preview to avoid sending a preview for the unsanitized URL. Incoming messages, message edits, captions, and old chat history are not changed. More extensive link cleanup belongs to the next iteration after this beta's on-device QA.

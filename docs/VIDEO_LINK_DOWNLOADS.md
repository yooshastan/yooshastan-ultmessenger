# Ultgram: auto-save video links (experimental Beta 3.4.4)

This opt-in feature observes **newly received** Telegram messages while Ultgram is running and examines their visible HTTPS links (up to three per message). It does not use a bot, send chat contents to an extraction service, or ask the Telegram server to fetch external media.

**User flow:** Ultgram Settings → **Downloads and Storage → Video links & auto-post**. This is a direct settings row, not a hidden horizontal toolbar. The screen offers separate **Auto-save video links** (OFF by default), **Automatically send video to chat** (OFF by default), and **Wi-Fi / unmetered only**, plus last-attempt status. Turning auto-save OFF also turns chat posting OFF. Defaults: OFF, unmetered/Wi-Fi-only ON, file size must be known and no greater than 100 MiB. Downloaded files are queued using Android DownloadManager to the device's **Downloads/Ultgram** directory, separate from Telegram's native download queue. Android can finish an already queued transfer after Ultgram closes.

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


## Outgoing chat video auto-post (experimental Beta 3.4.2)

After explicit separate opt-in, only a supported site video URL **you posted** after Telegram server acknowledgement is eligible. A successful extracted MP4 is saved locally, staged in app-private storage, and handed to Telegram's native asynchronous media-upload queue as an additional message in the same chat. This is **not a delivery guarantee**; verify the resulting message in the conversation. Incoming links never auto-post. Failed extraction, protected/secret messages, disabling the feature, strict native proxy mode, and unsupported files cannot auto-post. Local staged files are retained for pending asynchronous uploads; a later staging operation removes files over 7 days old. Telegram MTProxy does NOT cover external extractor traffic; strict mode requests cancellation but is not a system VPN kill switch. Auto-post currently applies to successfully extracted MP4 site videos, not separate Android DownloadManager direct-file transfers. Forum topics, paid chats, and restricted content require additional device QA; do not use for public production releases until verified.

Beta 3.4.1 on-device screenshot showed a generic extractor failure. Beta 3.4.2 classifies common failure categories without logging or displaying private URLs. Actual root cause still requires device testing. Link sanitizer remains a separate working, opt-in feature and is unchanged.


## Device fix in Beta 3.4.3: rotating Instagram share IDs

A screenshot of a sent Reel link retaining `?avpt=...` exposed a gap: previous sanitation rules listed `bqrl` but not Instagram's newer rotating `avpt` share token. The Instagram-only tracking allowlist now includes known rotating identifiers (`avpt`, `stkn`, `srtk`, etc.) without a catch-all that could remove functional four-character query keys. Video shortcodes, carousel `img_index`, language `hl`, and signed/authentication query strings remain intact. Also permit ordinary auto-linked pasted URLSpan text **only** when the visible URL equals its target and there is no other CharacterStyle; preserve custom text links and formatted content unchanged. No cleaning of already-sent messages. Device verification needed specifically for anonymous sending and Telegram's alternative send paths. The Instagram downloader's "login or denied access" status is unrelated to this link sanitizer fix and remains a separate site access limitation.


### Beta 3.4.4: device diagnostics and sender-identity guard

The original screen's `Completed/queued` count combined merely queued Android download requests with previous successful site saves and **did not prove delivery to Telegram chats**. The new screen adds **Confirmed site files saved (new counter)**, starting from zero in this version, and separately labels the historical queue/saved count as unverified. For each new attempt it stores only a safe **source category** (e.g. Instagram, YouTube, Other HTTPS webpage), local attempt time and outcome; no chat URL paths/query tokens are stored. The direct-page error now makes clear that no advanced site extractor was invoked. Operations are serialized to prevent out-of-order status overwrites.

**Group auto-post identity policy (Beta 3.4.5):** For ordinary groups, after a completed site-video download Ultgram checks that the original message belongs to the same group, was posted by this account, and uses the same send-as identity currently selected by Telegram (including an anonymous group admin identity). The native media uploader is invoked only after this check, and the video replies to the original link message in that group. The identity and rights are checked again immediately before enqueueing. If permissions, sender identity, paid-message rules or native strict-proxy mode differ, auto-post is blocked and the local video remains saved. Forum topics, broadcast channels, restricted chats and links posted by other members are excluded until fully validated; **a normal Android APK compile is not proof of group send-as safety**. Public Instagram/YouTube extraction may still be blocked by login/bot checks. The old 'Completed/queued' count never guaranteed Telegram delivery; Beta 3.4.4 added confirmed-site-save counts separately.


**Group QA acceptance (3.4.5):** With both auto-save and auto-post explicitly ON and native strict proxy OFF, test an ordinary group using the user's normal identity and one using 'Send anonymously'. The originating link and new video must be in the same group; the sender identity must match exactly. Switching the send-as identity between link posting and download completion must NOT trigger an auto-post. Verify that links from others do not send, that group video permissions are honored, and that a forum topic or broadcast channel does not receive a message in the wrong place. A failed extractor should never post. Test direct-file links separately: direct Android DownloadManager transfers are still local-save-only. Treat all these as unverified until actual device testing.

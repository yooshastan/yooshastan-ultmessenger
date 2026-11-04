# Ultgram Alpha 0.10i.3 RC1 — internal FOSS test candidate

**Status:** RC branch prepared from device-tested Alpha 0.10i.3. Built as FOSS ARM64 **debug** variant. This is not a production-signed release, Play/F-Droid submission, or a guarantee of safe routing. No GitHub Release object or immutable tag is implied by this file.

## Features included
- **Proxy:** saved experimental native tgnet strict guard reconnects on cold startup with selected working native MTProto/SOCKS5 proxy, without a manual strict-mode OFF/ON cycle. Guard remains opt-in, and this patch does not add a whole-app firewall.
- **Chat navigation:** optional six-category All / Unread / Groups / Channels / Bots / People bar; existing Telegram folders and server ordering are untouched.
- **Message utilities:** copy message ID, sender peer ID, Unix/ISO UTC timestamps and public channel/supergroup message permalinks when eligible. No private or secret message permalink synthesis.
- **Media:** user-initiated document metadata/file summary including file name, exact byte count, readable size and MIME; link into Telegram's existing cache/storage manager; no automatic downloads/deletions.
- **Appearance:** reuse Telegram's native two-line/three-line chat previews; opt-in settings and existing theme presets remain.
- **Privacy visibility:** static network-path coverage report and manual test checklist that distinguish native guard state from unverified Java/HTTP, DNS, CDN/media, WebView and calls/UDP routes.

## User-reported Pixel 8a functional acceptance — October 26, 2026
- Strict proxy remained ON during update and start; connection formed without toggling it.
- Messages and connectivity reported working.
- Two document-copy outputs reported for the same PDF: MIME `application/pdf`, `64375` bytes and `62.9 KiB` readable size.
- Two-line/three-line preview switch worked; user confirmed persistence.
- Network path coverage dialog printed its explicit NOT ROUTE-VERIFIED caveats.
- Media cache navigation, public-link action on an eligible channel and system-wide packet capture have not all been demonstrated in this cycle.

## Release process and signing
1. CI runs Python/Java regression contracts and the pinned FOSS upstream Android compile.
2. FOSS ARM64 artifact includes `ult-alpha10i3-rc1-foss-arm64-debug.apk`, `SHA256SUMS`, and `BUILD_INFO.txt`. Compare SHA256SUMS against downloaded APK before side-loading.
3. Debug-signed packages are for internal testing only; don't publish as store-ready or promise update compatibility with production keys.
4. For a public release, establish a project-owned release signing key, verify package/versionCode/update migration, review licensing/Telegram API obligations, and obtain final maintainer sign-off.
5. The nested draft PR stack (#2–#32) has **not** been merged into `main`; the RC branch is a packaging/review target, not a claim that `main` contains these features.

## Privacy limitations
The app does **not** contain a working DoQ QUIC backend or enforced DNS tracker filtering. Native tgnet socket guard behavior does not establish fail-closed routing for DNS, Java/HTTP, WebViews, calls, CDN, media, or other apps. Android VPN state is a hint, not proof that Ultgram itself traverses a specific tunnel. A genuine leak audit needs controlled captures at a trusted phone edge/gateway, tunnel-inner, and proxy-host, including outage, IPv6 and network-handoff cases. Do not publish a “no leaks” claim based on manual checkbox tests.

## Links
- [Alpha 0.10i.3 combined QA](ALPHA10I3_COMBINED_QA.md)
- [Earlier path audit](ALPHA10G_NETWORK_REGRESSION.md)
- [Proxy and DNS source audit](PROXY_DNS_AUDIT.md)
- [Device-tested Alpha 0.10i.3 Actions run](https://github.com/yooshastan/yooshastan-ultmessenger/actions/runs/40412773929)

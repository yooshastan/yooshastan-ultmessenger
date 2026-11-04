# Ultgram Alpha 0.10i.3 — media / appearance / privacy coverage

Base: user-tested Alpha 0.10i.2 (built from tested 0.10i.1a strict cold-start recovery).

## Delivered in this increment
1. **Media:** explicit "ULT: Copy file summary" for Telegram document messages (name, human-readable size, exact bytes, sanitized MIME). This does **not** download, export, or delete media. Ultgram Settings opens Telegram's existing **Media storage and cache** interface; destructive actions remain inside Telegram's own deliberate confirmation flow.
2. **Appearance:** Ultgram Settings → **Chat list preview density** exposes Telegram's native two-line vs three-line chat-list layout. This reuses `SharedConfig.setUseThreeLinesLayout`, affects the global chat-list preview setting, and is not an independent Ultgram renderer or per-account density implementation.
3. **Privacy:** a read-only **Network path audit coverage** report identifies unverified paths and correct capture placements. No packet interception, DoQ, tracker-blocker enforcement, leak prevention, or added network calls.

## Pinned source evidence / review limitations
- Native tgnet proxy and guarded sockets: `scripts/integrate-native-strict.py` and pinned `ConnectionsManager.cpp`/`ConnectionSocket.cpp`. Strict applies to observed tgnet socket paths only.
- Native chat-list layout: pinned Mercurygram `SharedConfig.setUseThreeLinesLayout`, with `DialogsAdapter` deriving normal 70/76dp row heights from this setting.
- Media cache manager: pinned `org.telegram.ui.CacheControlActivity` (existing UI). The new Ultgram Settings link only navigates to it.
- Native document name: Telegram `FileLoader.getDocumentFileName`; user must choose the clipboard copy action.
- Java hostname/HTTP, DNS including DoT/DoQ, WebView/links, CDN/media, calls/UDP are **not independently routed or leak-verified**. Existing `docs/PROXY_DNS_AUDIT.md` and `docs/ALPHA10G_NETWORK_REGRESSION.md` are prior source-review/runbook context, not packet-capture results.

## Device QA gates
- Install over 0.10i.2 without clearing storage; strict mode ON and working MTProxy selected. Cold-start and force-stop/reopen must send/receive immediately.
- Long-press a PDF or FLAC document. Check "Copy file summary" for filename, human size, exact byte count and MIME. Test an unnamed file too.
- Open **Media storage and cache**; ensure the native Telegram storage screen opens. Do **not** delete or clear cache during the navigation test.
- Switch two-line and three-line chat previews; confirm change in Chats, persistent after restart and reversible without affecting message history or list order.
- Verify existing chat-category bar, themes, conversation tools and newly added public message links remain functional.
- Open **Network path audit coverage** and copy the report. Ensure it displays "NOT ROUTE-VERIFIED" caveats and no endpoints, usernames, tokens or secrets.
- For traffic audits, compare **trusted phone-edge**, WireGuard tunnel-inner and proxy-host capture under: normal, proxy-down, IPv4/IPv6 and Wi-Fi/mobile handoff conditions. Do not claim safe zero-leak outcomes from checkbox tests or proxy-host-only observations.

## Pixel 8a device testing reported by user (2026-10-26)

These are **user-reported functional results**, not CI or packet-capture proof.

| Tested behavior | Status | Evidence scope |
| --- | --- | --- |
| Install/update and strict-proxy cold start with switch continuously ON | Passed | Proxy connected immediately without an OFF/ON toggle |
| Chat messaging and general proxy connectivity | Passed | User-reported |
| Document metadata long-press | Passed | Reported PDF MIME: application/pdf, Bytes: 64375 |
| Document file summary | Passed | Reported 62.9 KiB for 64375 bytes and application/pdf |
| Two/three-line chat preview | Passed | User observed layout change |
| Preview setting persists after reopening | User confirmed | No independent instrumented persistence test |
| Network path coverage dialog | Passed as display test | User copied redacted text; no packet capture |
| Media storage/cache navigation, public-message permalinks, network leak test | Not fully reported | Do not mark passed |

The packaging-only RC1 branch retains the device-tested app source changes. Debug-signing and binary reproducibility are **not** proven; install the RC1 APK as an internal test build and repeat startup checks.

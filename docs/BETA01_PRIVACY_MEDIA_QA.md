# Ultgram Beta 0.1 Privacy + Media — internal RC1

This combines two new screens, not a public-ready release. Branch:
feature/beta01-privacy-media, based on Alpha 0.16 source branch, which
itself began from tested Alpha 0.15 FOSS ARM64 baseline
acb59a5059808a9ed289b53ef34d7120a47fc7aa.

## Privacy Lab

- Four-second screen-scoped live sampling: Android active transport
  category, saved native strict request, observed native strict guard,
  Telegram connection state code.
- Bounded 40-event, in-memory change timeline. Clear without disk writes.
- Copy a redacted report with no IP, proxy host, account ID, usernames,
  file names, messages, credentials or packet payloads.
- Read-only: previously tested strict-proxy implementation untouched.
- VPN hint and native guard switch are NOT route proof. Outside captures
  may be required for DNS, CDN/media, WebViews and calls. No kill-switch claim.

## Media Studio

- Per-account search and Photo/Video/Audio/Document filters in up to 200
  eligible, already cached, document-backed Telegram Recent Downloads.
- Newest / largest / name sorting plus size/date summaries.
- Native ULT Downloads, queue, per-network rules and cache manager reachable.
- Save As still requires a user-selected Android document destination.
- No broad file scan, automatic export, downloader or protected-media bypass.
- Photo-only messages and older files outside native Recent Downloads are not
  indexed. Telegram normal media screens remain available.
- Account switch: reopen Media Studio under new account; no cross-account index.

## Internal APK gates

- Python contracts and Java model tests green.
- Full pinned FOSS ARM64 build green.
- Unique version 16.0.0-beta01.1 / 7183011, debug-signed. Debug signing
  does not qualify for public beta.
- Artifact contains APK, SHA256SUMS, BUILD_INFO, SIGNING_INFO.
- Verify signer/package ID against currently installed Alpha 0.15 before
  attempting in-place upgrade. Do not blindly uninstall the working build.
- Regression on Pixel 8a GrapheneOS and Samsung A36 Android 16:
  chats/calls/notifications, translation, story save, queue priority, cache,
  proxy startup, invalid proxy blocking native Telegram chat, valid proxy
  restored, Wi-Fi/mobile recovery; no routing claims beyond that.
- Privacy Lab: Wi-Fi, cellular, VPN and strict guard timeline; redacted
  Copy Report and Clear; account switch, background, session close.
- Media Studio: local FLAC, MP4, JPEG document and PDF; search, filter,
  sort, queue and rules, protected media absent, empty history, 200-item cap.
- Check crashes/ANRs and confirm no protected exports or privacy leaks.

## Public beta gates remain

Permanent release key and upgrade policy; third-party code/license/source
audit; privacy policy; F-Droid/other-store compliance; diagnostics design;
real device QA and independent network-route claim review. Publish a GitHub
prerelease only after those gates have been checked.

# Ultgram Alpha 0.16 Combined RC1 — Device QA

Base: tested Alpha 0.15 FOSS ARM64 commit `acb59a5059808a9ed289b53ef34d7120a47fc7aa`.
Candidate branch: `feature/alpha16-combined-rc1`.

## Features bundled

1. **Chat appearance/navigation/sorting** — Retain native two/three-line preview density and the dedicated opt-in category swipe control; expand per-account category font choices to 11/12/13/14/16/18 sp; add Mentions First presentation sort with archive and pinned priority preserved. No server folder/reorder changes.
2. **Message tools** — Add explicit long-press Copy metadata (JSON): message ID, optional sender peer ID, optional UTC Unix timestamp, optional validated public channel/supergroup permalink. Never includes body text; secret/private links omitted. Existing native search and public Markdown link tools retained.
3. **Privacy/identity** — Local Settings account card Reveal Once is temporary, closing after the dialog; no persistence of unmasked state. Existing external-link confirmations, screen protection and native strict-proxy implementation unchanged.
4. **Downloads/media** — Save As continues to use the Android system picker for every eligible, already downloaded file. The picker can suggest the previous selected document location (per account); Forget save location removes that hint. No automatic export, private/protected-media bypass or new network route.

## Gate A — build

- Scaffold and Java model tests pass.
- Full FOSS ARM64 Gradle build passes with unique `16.0.0-alpha16.1` / code `7183010` identity.
- APK signature verified, artifact ZIP contains APK + SHA256SUMS + BUILD_INFO + SIGNING_INFO.
- Compare candidate package, signer and versionCode with *installed* Alpha 0.15 before in-place installation. Never uninstall the working app to force an upgrade.

## Gate B — Pixel 8a GrapheneOS and Samsung A36

- Existing chats, calls, notifications, theme pairs, translations and account switching work.
- Try all six category fonts, switch account, return/relaunch: per-account persistence.
- Two/three-line preview density still works. Dedicated swipe navigator changes only the displayed ULT categories, without interfering with chat-row swipes or Android back.
- Mentions First places chats with mentions after existing pinned/archived items; return to All and verify normal Telegram order. No server-side mutation.
- Long-press a public message: JSON copy includes accurate IDs/timestamp/permalink. Long-press in private/secret chat: no public permalink and no message body in JSON.
- Local identity card: Reveal Once, close, reopen; card is masked again; verify per-account isolation.
- Strict proxy: startup retained, bad proxy blocks native Telegram traffic, restore valid proxy, Wi-Fi ↔ cellular recovery. This is *not* a verified whole-app kill switch.
- Downloads: queue pause/resume/priority, network auto-download rules, Save As picker suggestion, Forget save location, protected-media export denied. Cancel Save As leaves source unchanged.
- Alpha 0.14/0.15 regression: eligible story saves, 2–32GB cache, signed upgrade path.

Do not mark Alpha 0.16 device-verified until these checks are reported.

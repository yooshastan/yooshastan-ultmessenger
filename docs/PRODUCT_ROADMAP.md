# Ultgram — unified client direction

**Vision:** An independent Android Telegram-compatible client by Yooshastan Studios, with Nullgram-inspired polish, useful local tools, privacy controls, and straightforward configuration. The app should remain a fully functioning Telegram client on Pixel/GrapheneOS without requiring Google services. Support an optional Firebase-compatible flavor, but never make Google services mandatory.

## Product promises

- **Opt-in, understandable controls.** No silent forwarding, edits, external data uploads, proxy switching or clipboard writes.
- **No ads added by Ultgram.** Server-delivered sponsored Telegram content is a separate mechanism; never imply full network-wide ad removal unless it is actually supported and verified.
- **No fake Premium entitlement.** Telegram enforces some story, upload, channel, viewer-history, limit and Premium features on its servers. We can reproduce *client-only* quality-of-life features without a subscription; we cannot unlock the server-side account flags.
- **Privacy with accurate threat modeling.** A proxy, encrypted DNS, scrubbed media metadata, local names and UI obfuscation each address different leaks; none makes a Telegram account anonymous or changes the identity Telegram sends to other users.
- **No challenge bypass.** Help users navigate bot verification and provide standard Android Autofill where available, but do not silently solve group CAPTCHAs or circumvent administrator anti-spam controls.

## Roadmap (small independently testable alpha slices)

| Priority | Milestone | Scope | Exit criteria |
| --- | --- | --- | --- |
| Shipped, device-tested | 0.1.1–0.5 | Stable native client, themes, named proxy profiles, chat layouts, chat-level IDs | Pixel/GrapheneOS smoke tests pass |
| In review | **0.6 Message tools** | Long-press: copy message ID and sender peer ID. No content access/upload | Native FOSS ARM64 CI and device tests pass |
| In review / CI preview | **0.7 Visual identity** | Distinct Ultgram launcher icon; Nullgram-inspired *original* color/spacing/surfaces; light and dark variants; retain Android dynamic color fallback | Adaptive icon safe zone, high/low density assets and screenshots verified |
| In implementation / device test pending | **0.8 Conversation organization** | Hide/archive-free chat-list presentation toggle (archived chats remain intact); clean tabs and advanced sorting | No chat deleted or unarchived just by changing visibility |
| Next | **0.9 Translation** | Per-chat automatic translation toggle, language selectors, on-device/offline preference and explicit opt-in to remote engines | Secret chats do not leave device; tests in several languages; clear download/data cost |
| Later | **0.10 Proxy & DNS** | Expand named SOCKS5/MTProto profiles; connection diagnostics; audit app DNS paths, then optional encrypted DNS (DoH3/DoQ only if compatible) | No plaintext fallback when explicit strict mode is requested; clear proxy trust warnings |
| Later | **0.11 Privacy/identity controls** | Local account alias/masking in screenshots and UI; media metadata stripping where safe; direct-peer connection settings; privacy audit | No claim of spoofing Telegram account identity or anonymity; account/network traffic tests |
| Later | **0.12 Creator tools** | Post/channel drafts, scheduling shortcuts, caption templates, media formatting, story composer improvements where APIs permit | Respect Telegram rate, role, server limits and Premium restrictions |
| Research | SMS/MMS/RCS companion | Permission-aware, opt-in companion view; Android SMS default-app requirements; RCS access depends on platform/carrier support | Never promise arbitrary Google Messages RCS history extraction or rooting-based bypass |
| Research | Verification assistance | Clear group join guidance, opt-in supported Autofill and accessible prompts | No CAPTCHA bypass or automatic anti-bot challenge solving |

## New launcher icon direction

A futuristic ultraviolet/pink/silver **folded-wing U** with a central message/plane silhouette and small starlight accents: a subtle K-pop reference, not a copied Telegram or artist logo. Ship Android adaptive foreground with comfortable circular-mask safe zone; keep previews separate until device review.

## Source and release hygiene

- Keep changes on separate branches and draft PRs until CI plus real-device tests succeed.
- Continue the Google-free ARM64 build. Firebase build may be skipped when registration/config is absent and should be reported as such.
- Keep the pinned upstream source and guarded patches; never commit Telegram API hashes, phone numbers, private chats, user content or production signing keys.
- Change the visible app name/icon only through a tested branding patch and verify launcher, recents, share sheet and Android Auto behaviors.
- The Alpha 0.8 setting is per-account and defaults off. It filters only the **displayed** Archive row in All Chats, not the underlying archive, pinned state, search, notifications or folder contents; provide an explicit **Open Archive** menu entry when hidden.

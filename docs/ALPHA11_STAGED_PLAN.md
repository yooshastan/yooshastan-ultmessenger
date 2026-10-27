# Ultgram Alpha 0.11 — staged development plan (2026-10-27)

**Base:** Alpha 0.10i.3 RC1, FOSS ARM64 CI passed and user-tested on Pixel 8a.
**Nonregression invariant:** native strict-proxy source is unchanged across Alpha 0.11 slices; no direct-network fallback, no new active DNS routing, and no hidden network transmissions.

## 0.11a — opt-in local privacy (first implemented slice)
- Screen privacy toggle in Ultgram Settings: Android FLAG_SECURE for main `LaunchActivity` window, using upstream `FlagSecureReason` so passcode-based protection is not clobbered.
- Off by default; per-install local preference, reversible; update immediately through LaunchActivity and again at resume.
- Does **not** mask identity in Telegram's servers, notifications, external app windows or another device's camera. Existing Telegram global `disableSecureFlags` override may limit effectiveness. Multiwindow, share sheets and platform screenshots require device QA.
- Follow-up local aliases/identity redaction need explicit per-screen implementation and safe validation; no claim that account names in all views are changed.

## 0.11b — DNS and network-path audit (next stage)
- Trace Java HTTP, hostname resolution, media CDN, WebView/previews and calls/UDP against pinned upstream source; compare system VPN and proxy routing correctly.
- Continue read-only/offline source and packet *text* audit. Establish trusted phone edge, tunnel-inner and proxy-host visibility. Never capture phone payloads or print endpoint secrets in reports.
- No live DoQ/DoH3, encrypted-only DNS, or tracker enforcement until the actual backends are installed and fail-closed behavior is proven for owned code paths; rootless VPN-based capture must not collide with Wonhee VPN.
- Test proxy down, IPv6, background startup, Wi-Fi/mobile handoff and VPN presence with privacy caveats.

## 0.11c — chat navigation and offline translation (subsequent stage)
- Local account-scoped category tab order and optional hide/show with a guaranteed All view and reset, independent of server folders and chat databases.
- Offline translation engine inventory and explicit capability/consent; missing offline provider must fail closed for secrets rather than fall back to Java HTTP.
- Device QA: account switching, persistence, pinned/archived conversations, switching categories, no unauthorized remote translation of secret chats.

## Release gates
- Each stage has its own branch/PR, Python/Java contracts and FOSS ARM64 compile before Pixel smoke test.
- Strict-proxy ON across cold start and reconnect, messaging, downloads, no forced-toggle recovery, existing theme/document behavior.
- No claims of VPN/DoQ protections or network fail-closed behavior without actual capture.
- Public distribution needs owned signing, versioning, security/license review.

## 0.11a.1 — Settings dialog screenshot test finding

A Pixel 8a screenshot with Ultgram screenshot protection enabled showed a blacked-out main Activity background, while the Android Private DNS information dialog remained visible. This is consistent with a separate dialog window that did not inherit the main window's FLAG_SECURE setting.

Remedy: all sixteen `android.app.AlertDialog.Builder` sites owned by `UltSettingsActivity` now use a guarded builder that sets FLAG_SECURE **on each dialog window before showing it**, if Ultgram screenshot privacy OR Telegram's own passcode screenshot rule requires secure behavior. The global Telegram `SharedConfig.disableSecureFlags` override is honored. This is a UI-only change, not a change to native MTProto/DNS/Java traffic.

### QA after patch
1. On Pixel 8a, turn **Protect Ultgram screenshots** ON. Open the **Android Private DNS (DoT)** dialog and take a system screenshot. The dialog should be hidden, blank, or blocked. No personal network or account details should be exposed.
2. Repeat on **Network privacy dashboard**, **Network path audit coverage**, proxy-related Settings dialogs, and the screen-privacy Settings dialog.
3. Turn the feature OFF, confirm screenshots of these Ultgram-owned Settings dialogs work again **unless Telegram's independent passcode/secure setting is active**.
4. Reopen the app with strict proxy continuously ON. Ensure connectivity and messaging still work without toggling it.
5. This patch does not cover **all Telegram dialogs**, system settings, notifications, the Android share sheet or third-party overlays; audit those separately. Screen privacy is not a traffic leak test or identity masking to Telegram peers.

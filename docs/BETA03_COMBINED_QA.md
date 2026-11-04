# Ultgram Beta 0.3 — one combined internal test candidate

## Scope

This is one test APK covering stabilization (0.1), native advanced-search access
(0.2), and reconnect-status guidance (0.3). No three-install sequence.

**Source:** `feature/beta01-navigation-message-polish`
**Candidate:** `16.0.0-beta03.1`, versionCode `7183013`
**Artifact:** `ult-beta03-combined-foss-arm64-debug.apk`
**Android variants:** pinned Mercurygram FOSS ARM64 debug; not public release-signed.

### What changed

1. Remove the floating Ultgram category chip strip and swipe pad overlapping
   Mercurygram's bottom navigation. Preserve native Telegram top folders,
   bottom navigation, filter/sort selection and normal search.
2. Message long-press keeps Telegram's native context tools and exposes only
   `ULT: Copy reference`, plus `ULT: Copy file info` for eligible documents.
   Copy reference combines message ID, peer ID, UTC and valid public permalink.
   No private t.me/c or secret-chat link export.
3. `Chats → ⋮ → ULT: Advanced search` invokes the existing Telegram
   full message/media search with sender/date filters. It does not create a
   separate index, exporter or message pipeline. Ordinary search remains.
4. Advanced → Network diagnostics reports human-readable Telegram connection
   states and targeted recovery advice. Does not trigger direct reconnect,
   change network routing, or disable strict proxy.

### Required Pixel 8a and Samsung A36 tests

- Open Chats with 0, 1 and several Telegram folders: only Telegram's native
  top folder tabs and app bottom navigation appear. No floating ULT bar.
- Telegram folder swipes, gesture navigation, app switch, back, search keyboard
  and story strip remain tappable without overlap.
- Long-press ordinary private text, public channel post, eligible document,
  protected media, sponsored content (if present), secret chat. Confirm only
  allowed actions display; user-selected copies include no private permalinks.
- Open native search and `ULT: Advanced search`; test query, message/file/media,
  sender/date filters and back navigation. Must never silently replace Telegram
  native index or suppress ordinary search.
- Disable all proxies, enable valid proxy, restart app; then enable native strict
  mode with valid proxy. Confirm messages send/download with valid proxy.
- Choose intentionally broken proxy with strict mode ON: Telegram native chat
  must fail closed, not silently connect directly. Restore valid proxy.
- Toggle Wi-Fi/mobile network and restart app; observe recovered connection
  status while strict guard stays on. No claim about Java/DNS/calls without
  independent capture.
- Confirm translated messages, permitted story saving, cache limits, downloads,
  notification delivery, text composer, calls and media playback still work.
- Verify exact package ID, versionCode, versionName, signer SHA256 and upgrade
  compatibility with the installed APK before installing. Do not uninstall a
  working app or clear its data for the test.

### Release gates

- Broad Python/Java source tests and pinned ARM64 Gradle compile pass.
- Artifact is uploaded with hash, package identity and signer details.
- Device checks on both Pixel 8a GrapheneOS and Samsung A36 pass.
- Public beta still requires dedicated release key custody, a predictable
  update path, policy/license/source audit, and privacy/network claims review.

# Ultgram Beta 0.1 — Quality-first internal candidate

Development branch: `feature/beta01-quality-trim`. Based on the Beta 0.1
privacy/media experiment, without changing the tested native strict proxy
or translation implementations.

## Product decision

Quality over quantity. The former Privacy Lab was a read-only observer,
not a firewall or packet capture tool, and the standalone Media Studio
duplicated Telegram's existing Recent Downloads. Neither is advertised
as a new flagship feature in this candidate.

## Simplified navigation

Main Ultgram Settings contains **11 action rows**:

- What's New / build information
- Chats and categories (grouped controls)
- Themes and display (grouped controls)
- Downloads (single native download queue/history)
- Storage and cache (cache-size and cleanup controls)
- Proxy manager
- Strict proxy (native Telegram sockets only)
- Screenshot protection
- External link confirmation
- Local identity cover (local settings cards only)
- Network diagnostics (advanced, read-only)

Media Studio is removed from navigation and generated APK sources.
Its unused source prototypes remain in the repository for reference only.
DoQ preview, tracker rules preview, duplicate privacy dashboards, and
manual checklists are no longer promoted as normal user features.

In Downloads, expose Pause all, Resume next, Network rules, Storage and
More. More contains Resume all, Queue status, Clear recent history and
Forget save location. Native Telegram owns transfers, history and ordering.

## Non-regression checks

- All existing chat, translation, story, download, and native strict-proxy
  integrations remain in the build scripts.
- Test valid proxy and invalid-proxy blocking, force-stop/reopen, and
  Wi-Fi/cellular recovery on Pixel 8a and Samsung A36.
- Verify file Save As uses the system document picker, honors protected
  media restrictions, does not delete files, and remains per-account.
- Verify network-rule dialog shows Mobile/Wi-Fi/Roaming choices.
- Verify all grouped menu choices open a functioning native screen or
  the existing Ultgram preference action.
- Verify no duplicate Media Studio destination, and only one Downloads
  entry in the main settings page.
- Verify release notes explicitly distinguish working safeguards
  from diagnostic hints and inactive transports.
- Switch account during diagnostics, confirm reports stay scoped
  and no host/identity/payload data appears.
- Confirm old Alpha 0.15 data remains intact; never uninstall the
  working app to force a debug-signed candidate to install.

## Still required before public Beta 0.1

Fresh full ARM64 compile, signed artifact inspection, device smoke tests,
release key and upgrade policy, source/license audit, privacy statement,
and independent verification of any whole-path networking claims.
No new privacy guarantees are asserted by this UI cleanup.

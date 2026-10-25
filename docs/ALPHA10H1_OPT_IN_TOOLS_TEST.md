# Ultgram Alpha 0.10h.1 — opt-in tools and rootless network test report

Base: Alpha 0.10g, kept as the user's known-good APK. **All new UI actions are opt-in**.
This is not a signed production release and no per-app leak-proof/fail-closed claim is made.

## Added in this increment

1. **Chats → overflow → ULT: Next Chat Tab** cycles six local views: All,
   Unread, Groups, Channels, Bots, People/private chats. The existing picker
   retains **Unread First** as a separate local sorting preset. Preferences are
   per account. No server folder, pin, archive, read mark, or account data is changed.
   This is a *menu-based quick-cycle*, **not yet a visible swipe-tab strip**.
2. **Long-press a normal message** to copy its positive Unix timestamp (UTC).
   For document attachments, you can copy MIME type and raw byte count.
   No file upload, export or download-manager interception is added. Clipboard
   changes occur only after explicit user selection.
3. **Ultgram Settings → Compact Ultgram Settings rows** toggles the padding
   on Ultgram's own settings page and remembers your choice. Telegram chat
   list density and Mercurygram Settings are unchanged.
4. **Ultgram Settings → Network test checklist / Copy network test report**
   stores manual yes/no checkmarks and generates a redacted report containing
   Android's VPN transport hint, native proxy mode and the manual checklist.
   It does **not** capture packets, read traffic content, save endpoint IPs,
   or inspect Wonhee's private app state.

## Phone test sequence (Pixel 8a first, Samsung A36 next)

- Keep **Wonhee Proxy active** during the normal app tests. Do **not** start
  PCAPdroid in ordinary non-root VPN mode concurrently.
- Switch from **All** through each ULT quick tab, including People, then back
  to All. Verify no conversations disappeared when All is restored.
- Switch to **Unread First** with the existing picker. Confirm normal pinned
  order remains stable. Test the archive visibility preference separately.
- Check a second Telegram account if available; changing the first account's
  local view should not change the second account's selection.
- Copy a known message timestamp; verify it consists of Unix seconds. Copy
  document details from a non-sensitive test file; compare MIME/size.
- Toggle compact Ultgram Settings rows, navigate away and return. Confirm the
  selection persists, and Mercurygram Settings remains unaffected.
- Open the manual checklist, mark only tests actually done, save, copy a report.
  The report should include NO token, public IP, account, username, proxy
  secret or other credentials. **Redact and review before sharing.**
- Keep WireGuard connected while sending a message, loading a channel,
  downloading a small file and opening/canceling an external URL. Repeat a
  safe Wi-Fi/cellular transition. Mark the checklist only for observed results.

Optional independent server observation: `sudo wg show`, plus Alpha 0.10g's
`scripts/alpha10g-path-audit.py` on an appropriately verified `wg0` capture.
WireGuard traffic arriving at Fedora is not proof of no alternate phone egress.
For such a proof attempt, capture at a phone-edge router/controlled gateway.
No root access is needed for the UI regression tests.

## Explicitly deferred

- A visible horizontally scrollable/swipeable Telegram chat tab bar
- Batch save/forward message workflows and a file export manager
- Download queue controls, media cleanup or a changed media player
- Telegram chat-list density/typography changes (this slice affects ULT Settings)
- Real DoQ QUIC transport, active tracker blocking, and local identity masking

**Release gate:** Android FOSS ARM64 CI and the relevant source/Java unit tests
must pass; only the user can sign off Pixel/Samsung functional behavior. Native
strict-proxy routing remains an independently pending packet-capture audit.

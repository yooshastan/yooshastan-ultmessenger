# Ultgram Alpha 0.10h.2 — native strict-proxy recovery test

Base: Alpha 0.10h.1, whose FOSS ARM64 CI build passed. This branch is a **draft** pending device testing.
The user's Alpha 0.10g/h.1 APK remains the recovery baseline. Do **not** clear Ultgram storage or uninstall it to test this patch.
All privacy, theme, chat and external-link settings must remain intact.

## Why this patch exists
On the Pixel 8a with Wonhee Proxy WireGuard active, Ultgram stopped loading Telegram while experimental
native strict-proxy mode had been used. Ultgram recovered after the mode was turned OFF and
the app was restarted. Cause is strongly suspected but not isolated to the mode vs reconnect event.

## Changes and limits
- Strict enabling requires **Use Proxy ON**, a valid selected MTProto/SOCKS5 proxy, and Mercurygram Tor OFF.
  This is configuration validation **only**, not an MTProto handshake or transport proof.
- While strict mode is requested ON, the normal Use Proxy OFF toggle is blocked with an explanatory dialog.
  Ultgram must not silently enable direct connections to "fix" a broken proxy.
- Turning strict OFF closes stale native Telegram transports, rechecks handshake/request queues and
  attempts to reconnect the active native DC when Android reports network availability and it is not paused.
  Java/HTTP, DNS, CDN, WebViews, uploads and media routes remain independently unaudited.
- The privacy dashboard, proxy diagnostics, and manual network test report show configured state and/or
  **native-observed state for the selected Telegram account**. Mismatches can occur briefly while queued
  native operations apply; a native ON/OFF report is not a network leak test.
- Removed a stale hardcoded "Alpha 0.10d" header in the copied proxy-status snapshot.

## Pixel 8a test sequence

0. **Leave Wonhee WireGuard active.** Preserve the working saved profile. Keep Ultgram native strict OFF at the start.
1. In ULT Proxy Manager, disable Telegram's proxy. Open messages, receive a test reply, load a channel,
   and download a small public test image. This should continue to work when permitted by the phone's
   current network. Network paths are **not** asserted to be encrypted by these checks.
2. With Use Proxy OFF, try to enable experimental native strict. It must refuse and show a "Select a proxy"
   explanation; no native strict switch or Android VPN setting must change.
3. Re-enable a known-good MTProto/SOCKS5 proxy. Verify its connectivity independently, then enable
   native strict and inspect the displayed "requested" vs "native observed" state. Do not trust a green
   native reachability check as a DNS or leak verdict.
4. While strict ON, try Use Proxy OFF. It must **refuse** and explain "Disable native strict mode first",
   while leaving the proxy enabled. Restore/keep the normal proxy-enabled state afterward.
5. Turn strict OFF via Ultgram Settings ("Disable and reconnect"). Within normal network conditions,
   confirm a message can be sent and received without force-stopping the app. Verify the copied report
   says native-observed OFF after the asynchronous transition settles.
6. Repeat steps 1–5 after a normal app force-stop/relaunch. Confirm settings persist and the native
   observed state eventually matches requested state.
7. Optional Wi-Fi ↔ cellular transition with Wonhee connected. Record connection behavior, not privacy
   conclusions. If connectivity fails, leave strict OFF; capture ADB logs and the redacted report.
8. On Samsung A36, repeat enabling refusal / disabling recovery if a second-device test is available.

**Stop testing and return strict OFF** if chats stop loading, recovery takes longer than normal,
or the native state never matches the requested state. Preserve known-good APK and logs.
Avoid testing direct fallback if it violates your privacy expectation.

## Security boundary
Wonhee is the only Android VPN. Ultgram does not launch another VPN.
This experimental tgnet guard remains **not a full firewall** and is not independently verified
to prevent direct Java, HTTP, media, CDN, DNS or external-link traffic. With no root and Wonhee
occupying the VPN slot, use an independent router or gateway capture for any future routing proof.

## Build gate
FOSS ARM64 CI, Python patch tests, Java policy/report tests, and pinned Mercurygram native compilation
must pass before installing. **Device connection recovery is not established by CI alone.**

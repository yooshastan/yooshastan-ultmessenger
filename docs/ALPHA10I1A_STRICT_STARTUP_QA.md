# Ultgram Alpha 0.10i.1a — strict cold-start regression

Base: Alpha 0.10i.1 category-bar build. Strict-mode startup recovery only; UI categories unchanged.

## Reported issue
After installing Alpha 0.10i.1, the app would not connect with saved native strict ON until the user switched strict OFF, established a connection, and enabled strict ON again. Conversations worked after recovery.

## Code hypothesis
The persisted native strict guard was being installed before the saved proxy address was applied. Early blocked native socket attempts could leave the initial connection idle. The patch preserves early guard activation, **does not permit direct traffic**, defers strict ON attempts while the proxy slot is empty, retries on restored proxy settings, and replays ON after Java network init.

## Test
1. Keep a known-working MTProto profile selected; Use Proxy ON; experimental strict ON.
2. Install the update over the previous APK without clearing storage.
3. Cold-launch: verify Telegram connects **without** toggling strict OFF.
4. Send and receive a message, load image and a larger file; repeat after force-stop/reopen.
5. Switch Wi-Fi ↔ mobile data with Wonhee VPN on; validate the selected proxy route.
6. Set an invalid proxy address while strict ON: messages should stall instead of silently connecting directly.
7. Restore working proxy while strict remains ON: messages should resume.
8. Disable strict manually and verify recovery. Re-enable with working proxy, then repeat cold launch.
9. Confirm the six-category bar remains unchanged.
10. Record saved/native strict status in Ultgram Settings → Network privacy dashboard.

This is a native tgnet guard, not a full leak-proof kill switch. DNS, Java, HTTP, media/CDN, and WebView routes remain unaudited. Do not claim full fail-closed guarantees without packet-capture validation.

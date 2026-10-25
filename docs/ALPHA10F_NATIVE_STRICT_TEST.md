# Ultgram Alpha 0.10f.1 — experimental native proxy guard

## Scope and safety

The **Experimental native proxy-only mode** switch in **Settings → Ultgram Settings → Privacy and Network** is **off by default**. It aims to refuse opening a direct `tgnet` socket when the native Telegram proxy address is empty. It is not a whole-device or whole-app firewall. It does not govern Java HTTP requests, Android DNS, WebViews, media loaders, the external browser, or every network path. It does not provide DoQ, encrypted-only DNS, or tracker filtering.

**Do not treat a passing build or an apparently working proxy as proof of fail-closed routing.** Until verified by controlled network captures, this is a developer-only preview. Do not use it where failure to enforce traffic routing could put someone at risk.

Mercurygram Tor also controls native proxy configuration. The Ultgram settings screen blocks enabling the experimental guard while Tor is active; avoid toggling Tor and strict mode concurrently.

## Baseline

1. Confirm the test server is available (on the user's Fedora host, `wonhee-mtproxy` maps host port 8443 to container port 443).
2. Choose a functioning MTProto proxy in **ULT Proxy Manager** and turn on the native **Use Proxy** switch.
3. Open **Ultgram Settings → Experimental native proxy-only mode** and opt in only for this test.
4. With PCAPdroid, isolate Ultgram and capture app traffic while loading a channel, sending a harmless test message, and downloading an ordinary file. Include the timestamp and Android device/build in the test notes. Root-enabled capture or an independent gateway capture is preferred to distinguish app traffic from system DNS.
5. Look for sustained proxy payload traffic. Also investigate *any* direct Telegram DC traffic, DNS packets, and unexpected hosts. A host-side proxy capture alone does not prove absence of direct paths.

## Proxy outage and recovery

Perform this test only when you can intentionally interrupt your own proxy, for example during a maintenance window; other active clients may lose connectivity.

1. Begin a fresh app-level PCAPdroid capture **before** creating the outage. Keep the guard on throughout; do not change proxy selection.
2. Intentionally interrupt the **test proxy** from its owner/host. Observe the native Telegram connection status and attempt one harmless chat/channel refresh.
3. Check whether the app opens a direct native socket to Telegram data centers or performs unexpected DNS/HTTP traffic. Record the raw evidence, not just a total packet count.
4. Restore the same proxy and confirm the app can reconnect without enabling direct mode. Then switch the experimental guard off and confirm normal behavior.
5. Repeat on Wi-Fi and cellular, and after process restart, device network changes, and a saved proxy being manually disabled. Repeat on Pixel 8a and Samsung A36 if available.

**Fail criteria:** Any observed direct MTProto connection while the experimental native guard is enabled, loss of the guard on restart, broken navigation/CPU churn during an outage, or traffic falsely labeled as blocked. Stop testing and report the issue; do not promote the toggle to a security guarantee.

**Separate gates:** Complete the Java/native DNS and media-path audit, implement a verified encrypted resolver with an explicit failure policy if desired, and implement tracker filtering only after defining exactly which HTTP/DNS paths are covered. PCAPdroid alone cannot certify all system behavior, especially if capture is not rooted.

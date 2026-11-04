# Alpha 0.10f — privacy stack design and native routing gates

This first patch adds a single **Ultgram Settings** navigation hub. It does
**not** activate strict proxy routing, DoQ, encrypted DNS or tracker filtering.
Those controls must remain marked *under development* until the transport
changes and device tests below are complete.

## Pinned upstream review

Mercurygram commit: `ff1ef95c8dfec5be9d15d50b9f4685a830f3bdd5`.

- `ConnectionsManager.java` loads `mainconfig/proxy_enabled` and proxy
  credentials at startup. When the UI disables the proxy, its global
  `setProxySettings` can pass an **empty address** to `native_setProxySettings`.
- `TgNetWrapper.cpp` forwards `native_setProxySettings` to the native
  `ConnectionsManager::setProxySettings` method.
- `ConnectionSocket.cpp::openConnection` uses the configured native proxy
  address when nonempty. Its **empty-proxy branch** opens a socket to the
  destination address instead. An override-proxy path also exists for checks.
- `ConnectionsManager.cpp::onConnectionClosed` distinguishes
  `ConnectionStateConnecting` from `ConnectionStateConnectingViaProxy`
  based on the proxy address. This is state feedback, not route enforcement.
- Other native, Java, DNS, media, call and WebView paths must still be audited.

## Strict proxy design gate

The intended strict rule is **no direct Telegram transport when strict mode is
enabled**, even during proxy startup, disable, configuration error or outage.
We should enforce that at the native socket decision point and make settings
transitions atomic across accounts. Do not rely solely on an Android UI toggle
or a dummy/unreachable proxy address. Account for Mercurygram Tor's separate
ownership of the native proxy slot.

Required checks: cold startup, enabled/disabled proxy state, healthy and
unreachable proxy, IPv4/IPv6, Wi-Fi/cellular switching, background process
death/restart, call/media/downloads, proxy checks and reconnection. Denied
connections should stop without excessive spin/retry or leaking destinations.

## DNS encryption gate

DoQ (RFC 9250) typically uses UDP 853 and validates a QUIC/TLS server identity.
Android Private DNS generally uses a distinct platform resolver mode; enabling
it does not prove the app's native/Java/third-party resolvers use DoQ. Audit
bootstrap resolution and every resolver path. An opt-in **encrypted-only**
mode must have explicit failed-lookup behavior and must never silently fall
back to UDP/TCP 53.

## Tracker protection gate

Start with local domain-based filtering on **app-controlled** external
HTTP/WebView requests. Provide a visible blocked-request log without storing
full private URLs; false-positive exceptions must be explicit. Telegram's own
messages and server-delivered sponsored content are outside this scope.
External links, previews, auto-loaded media and WebView subresources must be
evaluated separately. Never describe this as a system-wide ad blocker.

## Independent network verification

Use PCAPdroid's app filter for repeatable *functionality* captures on Samsung
and Pixel. For actual leak claims, also inspect unfiltered/root or gateway
captures because Android `netd` may originate DNS outside the app's UID and
non-root PCAPdroid uses a synthetic local VPN transport. Confirm persistent
application payload, not only TCP SYNs. A proxy outage must be **forced**
under observation. A successful proxy handshake is not a fail-closed test.

If a future strict implementation requires Android `VpnService`, remember
there is one active VPN slot per user/profile: it may displace Tailscale and
non-root PCAPdroid. Native enforcement is preferable if sufficient.

Never upload proxy credentials, Telegram IDs, complete PCAPs or private
browsing destinations to public issues.

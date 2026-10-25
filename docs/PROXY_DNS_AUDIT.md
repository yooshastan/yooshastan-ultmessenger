# Alpha 0.10c — Proxy transport and DNS audit (phase 1)

**Status:** read-only diagnostics preview. No new proxy implementation, DNS resolver, kill switch, or automatic proxy switching is activated in this release.

## Findings from pinned Mercurygram Android source

Pinned revision: `ff1ef95c8dfec5be9d15d50b9f4685a830f3bdd5`.

| Path | Evidence in source | What we know / cannot infer |
| --- | --- | --- |
| Native Telegram connections | `org.telegram.tgnet.ConnectionsManager.setProxySettings` calls `native_setProxySettings` for configured transport | Telegram's native network stack has proxy support; this does not prove every HTTP/media/DNS path uses it |
| Built-in proxy check | `ProxyListActivity.checkProxyList` calls `ConnectionsManager.checkProxy`; check results populate `checking`, `available`, `availableCheckTime`, `ping` | A reachability/latency check is available without writing a new networking client; it is not a leak test |
| Web proxy path | `WebProxyTransport.start` starts a local transport, which forwards through `127.0.0.1` | Distinct from SOCKS5 and MTProto routing; transport and resolver behavior requires separate tests |
| Java hostname resolution | `ConnectionsManager` contains hostname resolution tasks and `DNS_THREAD_POOL_EXECUTOR` | Hostname resolution paths exist outside the native proxy setter; actual DNS egress unverified |
| Telegram DC configuration | `ConnectionsManager` includes `MozillaDnsLoadTask` and `native_applyDnsConfig` | DNS configuration fetch has separate logic; behavior with each proxy type must be packet-tested |
| Media/previews and external links | Not exhaustively traced in this milestone | Do not assume thumbnails, voice/video calls, WebViews or shared links use the Telegram proxy |
| Mercurygram Tor feature | Special `mgInternal` proxy state and separate Tor controls | Do not overwrite or claim equivalent protections; diagnostics omit synthetic Tor entries |

## 0.10c phase 1 behavior

In **ULT Proxy Manager**, choose a saved SOCKS5/MTProto entry's information button, then **Connection diagnostics**. The dialog reports only:

- Selected or inactive profile state (read-only).
- Telegram client connection status as currently displayed by native code; not a verified route.
- Most recently cached native proxy reachability/ping outcome; no new probe triggered by opening the dialog.
- A clear notice that encrypted DNS and strict no-direct-fallback are **not** implemented.

No passwords, MTProto secrets, authentication tokens, message contents or proxy hostnames are logged, copied, or sent anywhere by this feature.

## Next phases — release blockers for strict routing / DoQ

1. Inventory and trace `ConnectionsManager` native/C++ resolver calls, Java DNS, WebProxy transport, media/preview downloaders, and external HTTP/translation integrations.
2. Reproduce on Pixel/GrapheneOS and Samsung A36 using a controlled SOCKS5/MTProto server while capturing device LAN and proxy-side traffic. Check DNS (UDP/TCP 53, DoT 853), HTTP(S), direct UDP, fallback and IPv6 paths; compare with VPN/Tor modes separately.
3. Test proxy downtime, captive portal, network changes, VPN coexistence and background reconnect. Never call a setting **strict** unless routing truly fails closed across required paths.
4. Evaluate DoQ (RFC 9250) and DoH3 only where resolver ownership and upstream behavior are established, with explicit privacy disclosure and user-selected trust anchor. Do not force a third-party DNS service.
5. Add a separate explicit consent switch only after verified implementation. The read-only dialog is **not** that switch.

A Telegram proxy may hide an IP from the Telegram service yet expose traffic metadata to the proxy operator, and does not mask the signed-in Telegram account identity or replace a VPN/Tailscale for arbitrary home services.

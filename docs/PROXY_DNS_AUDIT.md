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

## Phase 2 — controlled leak/failover verification on A36 and Pixel 8a

**These tests have not yet been executed or passed.** Record only the yes/no outcomes below until a trusted packet-capture point exists. Do not treat the app's 69 ms native MTProto ping as a DNS privacy measurement.

### Network visibility requirement

- A Fedora PC running the MTProto container sees **connections reaching its own proxy**, but it generally cannot see unrelated phone-to-internet traffic on a switched LAN or Wi-Fi. To verify direct connections and DNS leaks, capture at the **Wi-Fi router / gateway**, a permitted managed access point, or a dedicated test network that routes all the phone's traffic.
- A phone-side local-VPN capture app changes the network topology and may conflict with Tailscale or another VPN: Android supports only one active VPN service per user/profile. It is a useful secondary test, not unqualified proof of baseline behavior. [Android VPN guidance](https://developer.android.com/develop/connectivity/vpn).
- Do not share raw packet captures, DNS hostnames, proxy secrets, private IP inventories or Telegram account identifiers publicly. Summarize counts and endpoints by category.

### Device matrix

| Scenario | Telegram chat send/receive | Proxy server traffic | Non-proxy Telegram egress | DNS/DoT/DoQ egress | Expected safety interpretation |
| --- | --- | --- | --- | --- | --- |
| Native direct baseline, proxy off | Establish baseline | None expected | Expected | Record baseline | Direct mode, not private-routing test |
| Working selected MTProto, proxy on | Should work | Expected | **Investigate any** | Inspect; unresolved | Native app status alone is insufficient |
| Unreachable saved proxy, not selected | Should use selected working proxy | Expected for selected profile | **Investigate any** | Inspect; unresolved | Saved failure must not affect routing by itself |
| Selected proxy deliberately made unavailable on controlled test LAN | Record whether app reconnects/fails | Record retries | **Any unexpected direct fallback is a potential leak** | Inspect | No strict mode can ship until failure behavior is understood |
| Wi-Fi to mobile-data handoff while proxy selected | Record reconnect behavior | Expected over new network if reachable | **Investigate any** | Inspect both interfaces | Must revalidate in background and after restart |
| System VPN/Tailscale enabled vs disabled | Record app behavior separately | Depends on network route | Capture each condition | Inspect | VPN may alter visibility; do not conflate with proxy guarantees |

Mark each result with device, Android version, app artifact/build SHA, exact scenario, and whether the capture point was the **network gateway** or the **proxy host**.

### Resolution and fail-closed gate

- **No strict mode yet:** The current release does not block direct traffic if the proxy is disabled or unavailable. It would be misleading to label an existing `Use Proxy` toggle as a kill switch.
- **No DoQ yet:** DNS over QUIC is standardized by [RFC 9250](https://www.rfc-editor.org/rfc/rfc9250), with dedicated QUIC connections (by default UDP 853). It is not enabled merely by editing DNS settings; each app Java/native lookup path must be owned and tested. DoQ also does not tunnel Telegram message traffic.
- **Release gate:** only claim strict routing when the controlled network test shows no unintended direct Telegram or plaintext DNS egress in normal, failed-proxy, restart, handoff and IPv6 scenarios, with explicit user opt-in and honest reporting of exceptions.

## Alpha 0.10d audit tooling (next preview)

See the [dedicated device/capture runbook](ALPHA10D_TEST.md). It contains separate proxy-host versus gateway capture commands, a metadata-only stdin classifier, a controlled failed-proxy sequence, and explicit no-leak-proof interpretation. The redacted in-app status copy does **not** initiate a scan, collect network traffic or alter the proxy. All existing strict-route and DoQ release gates above remain in force.

## Alpha 0.10e scoped link guard (draft)

The pinned `org.telegram.messenger.browser.Browser.openUrl` dispatcher is now
wrapped by an **optional, locally stored external HTTP(S) confirmation** before
the ordinary upstream open behavior. ULT Proxy Manager's privacy panel exposes
the preference, default **Off**. This is a deliberate user-consent checkpoint,
**not** interception of automatic media, WebView subrequests, previews, DNS,
Java traffic, or native Telegram fallback. Strict fail-closed remains unavailable.
See [Alpha 0.10e test plan](ALPHA10E_TEST.md).

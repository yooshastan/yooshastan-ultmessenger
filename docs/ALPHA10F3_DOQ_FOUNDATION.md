# Alpha 0.10f.3 — DNS-over-QUIC foundation (draft)

**Status: protocol/transport contract only. DoQ is NOT running inside Ultgram.**

This follows Alpha 0.10f.2's **read-only Android Private DNS (DoT)** view. DoT differs from DNS-over-QUIC (DoQ). Private-DNS system status cannot establish how Telegram, Java HTTP clients, media/CDN code, or proxy hostnames resolve DNS.

## Implemented foundation

- `UltDoqEndpoint`: validates `quic://dns.example.org[:port]` and RFC 9250's default **UDP/853**. Requires a hostname for future TLS verification.
- `UltDoqResolver`: validates DNS wire queries, zero DNS message ID, one question, and response headers. It delegates exclusively to an injected QUIC backend.
- Missing or failing backend means an explicit error. **There is no fallback** to plaintext UDP/TCP 53, platform DNS, DoT or DoH in this contract.
- `UltDoqContractTest`: tests invalid endpoint rejection, wire errors, transport failure and synthetic response handling.

**These classes are not yet included in the Android APK or connected to `tgnet`.** They are not a QUIC implementation. No network DNS paths have changed; no privacy guarantee is claimed from these tests.

## Native backend candidate

AdGuardTeam/DnsLibs supports Android-native DoQ, DoT and DoH and is Apache-2.0 licensed: https://github.com/AdguardTeam/DnsLibs

- Candidate release **v2.10.2**, pinned underlying Git commit `0c6e855b12eee2f696e7cc30719532fda4fdd512`. Review transitive dependencies and licenses before bundling.
- Build reference: https://github.com/AdguardTeam/DnsLibs/blob/master/docs/android.md
- Requires native build toolchain (JDK 17+, Android SDK/NDK, CMake, Conan). This draft deliberately does **not** claim a verified Maven coordinate or ship an unknown AAR.
- A future adapter should call the library DNS-message handler as `UltDoqResolver.Backend`, with verified QUIC/TLS certificate and hostname checking, ALPN, timeout/error semantics, and no plaintext fallback.

## Gates before enabling a DoQ switch

1. **Secure bootstrap:** resolve the DoQ resolver hostname through configured trusted bootstrap addresses or another proven encrypted strategy. Failure must be visible and block the query. OS DNS bootstrapping cannot be described as strict private DNS.
2. **Actual call-site integration:** audit/native-hook `tgnet` hostname resolution, Java networking, SOCKS/MTProxy hostname lookups, media/CDN paths and any WebView or preview fetcher. Measure scope per path, never infer coverage from successful messaging.
3. **No downgrade:** no fallback to system DNS, UDP/TCP 53, DoH or DoT unless explicitly selected under a separate, honestly named mode. QUIC certificate errors must be fatal.
4. **Proxy boundaries:** DoQ UDP/853 to a resolver is a separate network flow; it does **not** force all Telegram connections through MTProxy or make the existing experimental native guard a whole-app firewall.
5. **Honest controls:** only show an enabled “DoQ protection” setting when the native backend is installed, owned query paths are rerouted, and the status/error UI reports actual network state. Keep this draft's UI unchanged.
6. **Network test:** on Pixel 8a and Samsung A36, capture Ultgram's UID with PCAPdroid plus root/gateway where possible, test Wi-Fi/cellular and deliberate resolver failure, and verify expected UDP/853 to the selected resolver, no app-owned plaintext DNS 53, and no silent failover. PCAPdroid cannot by itself prove whole-device leak freedom.

Protocol specification: https://www.rfc-editor.org/rfc/rfc9250

## Separate VPN later: Ultgram + Moka

A future Tailscale-like product can be an independent Android `VpnService` app to provide private connectivity and DNS policy for **both Ultgram and Moka**. This DoQ foundation does not create a VPN, require root, or interfere with system Tailscale. Android typically permits only one active VPN per user/profile; document that limitation when designing coexistence with Tailscale and PCAPdroid.

## Pure-Java contract test

```bash
mkdir -p /tmp/ult-doq-contract
javac -d /tmp/ult-doq-contract \
  dns/src/com/yooshastan/ultmessenger/dns/UltDoqEndpoint.java \
  dns/src/com/yooshastan/ultmessenger/dns/UltDoqResolver.java \
  dns/tests/UltDoqContractTest.java
java -cp /tmp/ult-doq-contract UltDoqContractTest
```

CI runs this test, but passing it does not establish that DNS encryption is active on device. An end-to-end QUIC resolver test is required for a later milestone.

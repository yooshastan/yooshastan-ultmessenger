# Ultgram Alpha 0.10d — proxy privacy verification

**Release status:** experimental read-only audit aid. No new DNS resolver, DoQ/DoH3, VPN, routing hook or kill switch is implemented. **Never infer anonymity, app-wide tunneling, or no DNS leaks from these diagnostics.**

## In-app redacted status report

In **Settings → Data and Storage → Proxy Settings → ULT Proxy Manager**, tap the information icon for a saved SOCKS5/MTProto entry → **Connection diagnostics** → **Copy redacted status**.

- Clipboard copying is **explicit**. Opening the dialog does not copy anything.
- Report contains only transport type (allowlisted), switch state, selected/session state, cached native probe status/latency, and a clear non-guarantee notice.
- No proxy host/IP/port, secret, credentials, chat data or account identifier is included.
- Clipboard content can be read by other apps or shown by Android. Review before sharing.
- An Android/native ping is **not** proof of DNS routing, encrypted DNS, or no direct fallback.

## Phase 2 device test sequence

Record device (Samsung A36 or Pixel 8a), Android version, build/artifact, date, network type, selected profile, and whether auto-switch is enabled. **Never put proxy secrets, phone numbers, account IDs or public IPs in public issues.**

1. **Proxy off / baseline.** Disable Use Proxy; verify diagnostics reads Off, disabled/direct may be used. Test normal chats and media.
2. **Working proxy.** Enable Use Proxy, select your known-good MTProto proxy and turn **Auto-switch proxies off for the controlled trial**. On mobile data, verify that a public endpoint connects and messaging works; on Wi-Fi, separately verify. Capture server ingress and confirm actual native session status.
3. **Cached probe.** Reopen diagnostics after a native check; it must distinguish checking, reachable with latency, failed, and never checked. Open/close without modifying proxy profiles.
4. **Failure case (controlled, reversible).** Save and select an intentionally unreachable *test* MTProto endpoint without disturbing the live Podman server; keep auto-switch off. Attempt a message, note whether session fails or succeeds unexpectedly, then restore known-good profile. This is not an actual strict-routing guarantee.
5. **Handoff.** With the working proxy selected, switch Wi-Fi ↔ mobile data and restart Ultgram; verify reconnection and record status. Keep Tailscale/VPN off for the baseline, then run a separate VPN case if needed.
6. **Regression.** Check translation, chats/media, Tor setting (do not alter Tor during MTProto case), night/day ULT themes, and native proxy controls on both devices.

Do not intentionally shut down a shared proxy or expose private services to simulate failure.

## Capture point matters

The **Fedora proxy host** at the forwarded internal port can confirm incoming TCP and MTProto activity; it **cannot** see ordinary phone-to-internet DNS or unrelated traffic. In controlled testing, incoming packets reached the Fedora proxy after correcting a forwarding rule. That confirms transport reachability only.

For phone egress on **home Wi-Fi**, capture on an authorized router/gateway/AP **before NAT**, or another trusted network topology through which the phone's traffic actually flows. Do not assume the Fedora workstation sees this traffic on a switched LAN. A home gateway cannot see the phone's independent cellular egress when the phone is on 5G.

The following optional, dependency-free utility reads existing **tcpdump text** on stdin and prints **counts only**, never addresses, payloads, hostnames, credentials or raw captures:

- [scripts/alpha10d-netwatch.py](../scripts/alpha10d-netwatch.py) — roles: proxy-host (ingress proof only) and gateway (observed phone egress, unattributed to app).

### Fedora proxy-host check (no DNS verdict)

On the Fedora host, with this project checked out and tcpdump available:

~~~bash
sudo timeout -s INT 30s tcpdump -nn -q -l -ni HOST_INTERFACE \
  'tcp dst port 8443 and dst host PROXY_HOST_IP' |
python3 scripts/alpha10d-netwatch.py \
  --capture-role proxy-host --proxy-ip PROXY_HOST_IP --proxy-port 8443
~~~

Expected report: inbound packet count and number of observed source addresses, **not** a leak verdict. Unsolicited internet port probes may contribute to this count.

### Authorized gateway/AP check (phone on Wi-Fi only)

Replace placeholders with the **interface and IPs visible before NAT at the gateway**. If the phone uses a public proxy address or IPv6, specify each observed alias with another --proxy-ip; specify phone IPv6 with another --phone-ip. Capture point and interface must actually observe phone egress:

~~~bash
sudo timeout -s INT 30s tcpdump -nn -q -l -ni GATEWAY_INTERFACE \
  'src host PHONE_WIFI_IP and (tcp or udp)' |
python3 scripts/alpha10d-netwatch.py \
  --capture-role gateway --phone-ip PHONE_WIFI_IP \
  --proxy-ip PROXY_IP_AS_SEEN_AT_GATEWAY --proxy-port 8443
~~~

**Interpretation limits:**

- plaintext_dns_port53: potential plain DNS egress from the device (not necessarily Ultgram).
- encrypted_dns_port853_candidate: can be DoT, DoQ or another protocol; not proof either way.
- udp443_quic_candidate: QUIC-like traffic; cannot identify HTTP/3 or app from port alone.
- other_direct_candidate: other device egress; system and unrelated apps create background traffic.
- proxy_endpoint: packets directed to configured proxy IP/port; the program does not validate MTProto authentication.
- Zero matched packets means **visibility unproven**, not "no leaks."
- The utility does not map processes, decrypt TLS, distinguish native vs Java DNS or prove fail-closed routing; manual traffic attribution is still required.
- Raw tcpdump text includes IPs even when the summary doesn't. Use the direct pipe above and avoid uploading raw packet logs.
- Do not run a packet capture on networks you do not own or administer.

## Pass criteria and release gate

| Criterion | Status |
| --- | --- |
| WAN TCP reaches Fedora proxy after corrected router rule | **Observed** (separate manually supplied capture) |
| Android native Telegram session through public MTProto endpoint | Reported working by tester; recheck per device |
| Redacted status reporter builds and runs on both phones | **Pending CI/device test** |
| Gateway captures correctly observe Wi-Fi phone egress | **Pending network setup** |
| No unintended direct Telegram traffic when proxy works | **Unverified** |
| No unintended direct fallback with failed proxy | **Unverified** |
| No plaintext DNS from Ultgram over Wi-Fi, mobile, IPv6, restart, VPN | **Unverified** |
| Strict no-direct-fallback and DoQ/DoH3 | **Not implemented** |

Before any strict-routing feature ships, confirm all relevant native, Java, media, translation, DNS and background paths, and verify safe failure behavior across network transitions. The Alpha 0.10d preview itself changes **no routing**.

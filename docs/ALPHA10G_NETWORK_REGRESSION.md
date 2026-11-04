# Ultgram Alpha 0.10g — controlled network regression

**Scope:** (1) native strict-proxy regression, (2) coexistence with a running
WireGuard VPN, (3) observation of DNS/media/external paths, and (4) the existing
Ultgram Settings interface. This update does **not** add DoQ, tracker blocking,
local identity masking, a VPN, or an audited kill switch.

## What changed

- The existing **Network privacy dashboard** shows a read-only Android
  active-network VPN transport hint. `TRANSPORT_VPN` means Android reports a VPN
  on the active network; it says **nothing** about per-app routing, encryption,
  MTProxy correctness or whether WireGuard is the active provider. Unavailable
  API information is shown as *unknown*, never as *protected*.
- `scripts/alpha10g-path-audit.py` accepts **text** from an operator-run
  `tcpdump` and summarizes destination **port categories** without recording
  or printing endpoint addresses, hostnames, packet bodies, account IDs or
  secret values. It never initiates a connection. Its classifications are
  observations, not leak verdicts.
- Synthetic tests guard the settings UI, source-patch anchors and the packet
  classifier. They do not replace controlled device captures.

## Safe test matrix — Pixel 8a first, Samsung A36 second

| Case | WireGuard | MTProxy / native guard | Device observation | Interpretation |
| --- | --- | --- | --- | --- |
| A | Off | Working proxy / guard off | Send a test message; load a channel and small file | Known-good baseline only |
| B | On | Working proxy / guard off | Repeat A; try selected public and home-reachable proxy endpoint separately | VPN coexistence; does not prove routing |
| C | On | Working proxy / guard on | Repeat A, switch Wi-Fi↔cellular; sleep/reopen | Check continuity and native-guard startup state |
| D | On | Deliberately unavailable proxy / guard on | Refresh one safe test channel; observe native reconnects and captures | Investigate any direct native path; **do not** claim fail-closed |
| E | On | Proxy disabled / guard on | Confirm native Telegram networking does not silently resume direct mode | Java/DNS/media traffic may still occur |
| F | On | Restore working proxy / guard on | Messages and downloads recover without disabling guard | Recovery behavior only |
| G | On | Working proxy / guard on | Open/cancel an external link; load a link preview, file, image and voice note | Check HTTP/WebView/media/CDN routes independently |
| H | On/off | Unchanged | Revisit **Ultgram Settings**, Mercurygram Settings, DoT posture and DoQ/tracker drafts | Verify settings persistence and no false protection claim |

For outage case D, intentionally stop only **your own test proxy** during a
maintenance window and restore it promptly. Other users of that proxy may lose
connectivity. Never disable your gateway firewall or expose a new public port
to make a test succeed. A failed message is not itself evidence of safe
blocking; inspect the routed packets.

## Capture placement matters

- **Phone edge:** A router/AP before NAT or root-capable device capture can
  observe VPN-outer UDP plus candidate traffic outside it. A capture of the
  Fedora host's ordinary Ethernet interface is *not* automatically a phone
  edge capture, especially with phone cellular data or switched Wi-Fi.
- **Tunnel inner:** A capture on the Fedora WireGuard interface `wg0` can see
  decrypted tunnel traffic sourced from the Android peer (previously
  `10.77.0.2`). DNS packets on port 53 here are **inside WireGuard**, not
  proof that DNS leaked onto the phone's public interface.
- **Proxy host:** Existing `scripts/alpha10d-netwatch.py --capture-role
  proxy-host` can prove only that packets reached your proxy, not that the
  phone made *no other* connections.
- **App attribution:** All of these aggregate packets by source-device IP,
  not Ultgram UID. Android, other apps, DNS helpers and VPN internals may
  contribute traffic. Use root-enabled PCAPdroid if it provides reliable
  per-app attribution without taking over the Android VPN slot; non-root
  PCAPdroid's VPN capture generally conflicts with Wonhee's VPN.

### Example: 30-second tunnel-inner capture on Fedora

Run from the Ultgram repository root on the Fedora machine with the home
WireGuard peer address verified as `10.77.0.2`. This example assumes your
existing MTProxy host is `10.0.0.169:8443`; verify it before testing. It
captures only text and prints aggregate counts; no PCAP is stored.

```bash
sudo timeout -s INT 30s tcpdump -nn -q -l -ni wg0 \
  'src host 10.77.0.2 and (tcp or udp)' |
python3 scripts/alpha10g-path-audit.py \
  --capture-role tunnel-inner \
  --source-ip 10.77.0.2 \
  --proxy-ip 10.0.0.169 --proxy-port 8443
```

For a verified *phone-edge* capture from a controlled AP, supply its observed
phone source IP with `--source-ip` and your actual WireGuard server public IP
with `--vpn-ip`, and set `--capture-role phone-edge`. Do not literally enter
`PHONE_IP`, `HOST_INTERFACE` or other placeholder values as shell commands.
Repeat with the phone's observed IPv6 address where applicable.

This tool cannot classify TLS/QUIC payloads as DNS/media/Telegram, identify a
secure DoQ handshake, or prove all egress went through the tunnel. A single
zero-packet test is *unproven visibility*, not a passing privacy test.

## Settings and rollback

1. With the guard **off**, verify the existing proxy works and the privacy
   dashboard still says **DoQ QUIC backend: NOT INSTALLED** and DNS tracker
   filtering **NOT ACTIVE**.
2. Turn the experimental guard **on**; ensure the existing VPN continues to
   run and `Ultgram Settings` still works. Check that the copied status omits
   account data and proxy secrets.
3. Turn the guard **off** after the test. If the app stops connecting, restore
   a working proxy first. Do not reset data or uninstall the debug APK as the
   first recovery step.
4. Redact phone/account IDs, proxy secrets, endpoint IPs and hostnames before
   sharing any reports. Never post raw WireGuard configuration or full PCAPs.

**Release gate:** Only source integration and synthetic tests can pass in CI.
VPN coexistence, media routing, direct-fallback and DNS leakage remain
**unverified** until these tests are actually performed on devices with
trusted capture visibility.

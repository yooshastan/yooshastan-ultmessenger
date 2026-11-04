# Ultgram Alpha 0.10f.2 — Android encrypted DNS posture

This preview adds an **Android Private DNS (DoT) status** entry to Ultgram
Settings. It **reads** the active network's `LinkProperties` using Android's
public `ConnectivityManager` APIs and offers to open Android DNS settings.
It does not write system resolver settings, perform a DNS query, add a VPN,
implement DoQ or intercept DNS. Its status text is **not** a DNS leak test.

## What this can report

| Android network state | Ultgram description | Security limitation |
| --- | --- | --- |
| Active, named Private DNS provider | Strict DNS-over-TLS active on the observed network | Applies to Android's system resolver, not necessarily app-owned/custom resolver paths |
| Active, no named provider | Opportunistic Private DNS | No encrypted-only guarantee |
| Inactive | Private DNS not active | DNS may be plaintext |
| Unavailable status/API | Unknown | Never infer encryption |

On Android 9+ the app has `ACCESS_NETWORK_STATE` from the existing Mercurygram
manifest. The status lookup uses no telemetry endpoint, network packet or user
credential. Provider hostname is shown locally and never copied into ULT logs.

## Device verification

1. Install the FOSS ARM64 preview on a test device without wiping an existing
   installation. CI debug signatures can change, so **back up any account state
   before considering an uninstall**.
2. Open **Settings → Ultgram Settings → Android Private DNS (DoT) status**.
   Record the local category, without posting a private DNS hostname publicly.
3. Tap **Open network settings**. Where supported this goes directly to Private
   DNS; otherwise navigate to the Private DNS control in the phone's network
   settings. Configure a DoT provider *hostname* you trust using Android's
   **Private DNS provider hostname** mode. Do not enter an IP address or DoQ
   URI as though it were a DoT hostname.
4. Return to Ultgram, reopen the status entry, and confirm the category becomes
   strict DoT. Repeat with Private DNS off and in automatic/opportunistic mode.
5. Repeat on Wi-Fi and cellular, including VPN/PCAPdroid active and inactive.
   Note that the active network inspected by Android can change while a VPN is
   running; a displayed status is not proof of every DNS path.
6. Separately test **Experimental native proxy-only mode** with the MTProto
   proxy available and during a controlled outage, following
   `docs/ALPHA10F_NATIVE_STRICT_TEST.md`. Do not rely on the guard for
   whole-app fail-closed behavior.

## DoQ and complete strict DNS remain future gates

DoQ per RFC 9250 uses a QUIC connection (typically UDP 853) and certificate
validation. A production implementation needs a dedicated, correctly secured
resolver in every applicable Java/native DNS path, a bootstrap plan, and an
encrypted-only failure policy **without UDP/TCP 53 fallback**. Resolver status
must be demonstrated with root/gateway packet captures and outage tests.
Android Private DNS implements DoT, **not DoQ**. No DoQ or tracker blocker is
present in this preview.

For PCAPdroid, use unfiltered/root or independent gateway capture in addition
to app-only views where possible. Non-root PCAPdroid uses a local VPN interface,
and system DNS requests can originate outside Ultgram's application UID.

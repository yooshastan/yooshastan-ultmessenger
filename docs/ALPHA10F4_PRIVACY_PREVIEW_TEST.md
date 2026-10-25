# Alpha 0.10f.4 — privacy controls spike / device test

**Preview status: do not mistake configuration UI for network enforcement.**

This draft stages the privacy UI and native DNS-engine build experiment. The existing experimental native MTProto direct-socket guard is inherited from Alpha 0.10f.1. **A functioning DoQ transport is not connected to Ultgram's DNS resolution paths**, and tracker-domain rules are not applied to Android network requests. The native AAR experiment is **blocked** on AdGuard-specific Conan packages unavailable from the default public remote. No AAR was produced or installed in the app.

## New features

- `Ultgram Settings → Privacy and Network → Network privacy dashboard`: honest live status of the existing experimental native guard, system DoT posture, stored DoQ configuration, and known unprotected paths. `Copy status` is explicit and never includes MTProxy/SOCKS secrets.
- `DNS-over-QUIC configuration (preview)`: saves a strictly parsed `quic://hostname[:port]` and a **literal IPv4 bootstrap address**. The address is not resolved via Android DNS and is never contacted in this APK. An empty server clears the draft. No DoQ-on switch is exposed.
- `Tracker domain rules (preview)`: saves user-entered blocked and allowlisted DNS suffixes locally. The allowlist takes precedence. These rules have **no effect on actual traffic until resolver integration**. No domain-history logging.
- `UltDnsTrackerPolicy`: independently tested domain-boundary matching and synthetic NXDOMAIN response generation for an explicitly routed DNS query. This class is not automatically invoked by Telegram.
- Manual-only experiment workflow `.github/workflows/build-doq-native.yml` tries to build AdGuard DnsLibs tag `v2.10.2` pinned to commit `0c6e855b12eee2f696e7cc30719532fda4fdd512`. The attempted build failed because `libevent/2.1.11@adguard/oss` and related pinned dependencies are absent from public ConanCenter; upstream uses an external Conan artifact repository. Do not claim an AAR artifact is available. An authorized, reviewable dependency source is required before resuming this experiment.

## Tomorrow's device checks (Pixel 8a and Samsung A36)

1. Install the **f4 FOSS APK** only after its full Android build job is green. Keep the existing known-good Ultgram install available for rollback; Android debug signing identities can change across CI runs. Back up anything you cannot afford to lose.
2. Verify Mercurygram Settings still exists and Ultgram Settings opens.
3. Open the network privacy dashboard. Confirm it explicitly says **DoQ QUIC backend: NOT INSTALLED** and filtering **NOT ACTIVE**. Copy status and verify **no proxy secret, password or Telegram account content** appears.
4. Save a sample `quic://` host and valid IPv4 literal. Confirm invalid schemes, malformed hostnames and invalid bootstrap IPs are rejected. Reopen to check saved drafts. **No DNS packet should be caused by this form.**
5. Save a blocked domain and an allowlisted exception; reopen and verify persistence. No domains should be blocked in live browsing from this preview.
6. Test the inherited experimental native strict mode with the existing known-good MTProxy. Disable it if Telegram connectivity is unreliable. Captures are needed before claiming it protects more than the guarded native sockets.
7. Use PCAPdroid or root/gateway captures to record baseline Ultgram traffic, but **do not interpret this build as an encrypted-DNS or tracker-protection test.** Native engine, routing hooks, secure bootstrap and TLS validation must land first.

## Gates for a REAL DoQ/filtered release

1. Review native AAR license tree/ABI/size and lock reproducible dependency.
2. Inject a certificate-verifying QUIC handler into the resolver contract and reject insecure failover.
3. Prove app-owned `tgnet` and Java DNS routing with case-by-case tests; check CDN/media and WebViews separately.
4. Add explicit opt-in enablement only when paths are actually redirected; switch to a safe blocked/error state on DoQ failure.
5. Capture Wi-Fi/mobile/IPv4/IPv6, bad certificate, offline resolver, proxy-off and Tor-conflict cases on real devices.

**Do not merge or publish as a security release merely because the UI or unit tests pass.**

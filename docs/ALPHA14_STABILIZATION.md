# Ultgram Alpha 0.14 stabilization and upgrade-safety gates

Baseline: `release/alpha14def-combined-rc1` @ `6ad6e61f9125134b61d007c8b7c797371e2abd68`; user has tested it successfully. **Do not replace or rewrite this branch.** Stabilization changes will be a separate candidate.

## Signing and in-place upgrades

A successful Gradle debug build does **not** establish certificate continuity across GitHub Actions machines. `apksigner verify --verbose --print-certs` confirms APK signing validity; `aapt dump badging` identifies package name/versionCode. CI records `SIGNING_INFO.txt` with the actual signer SHA-256 and **Upgrade compatibility NOT CHECKED** until an earlier APK is provided.

To compare two files on a trusted workstation with Android SDK build tools installed:

```bash
python3 scripts/apk_release_gate.py --apk /path/to/new.apk --previous /path/to/current-installed-version.apk
```

The comparison must match **Android package ID**, **signer SHA-256**, and have a nondecreasing version code. An earlier APK file must really correspond to the installed copy to establish a relevant result. If signing differs, **do not uninstall the working app as a workaround**; that can lose its account/session data. Decide on a stable, securely stored release-signing key before public app-store distribution. Debug signing is not distribution signing.

Do not paste private keystore keys or credentials into chat/repository. No secret signing keys are bundled with source or artifacts.

## Media comparison hardening

`Verify saved copy` checks local source versus user-selected document bytes using bounded SHA-256 with no upload. Stabilization also gates the result and any failure toast by current account and protected-content status after background work, so switching accounts during a hash won't expose another account's result. Expand digest tests: identical/different content, differing size, empty, null, 512 MiB limit. Files aren't automatically persisted or uploaded.

## Appearance and restart verification

Native font size preference: `mainconfig/fons_size`; bubble radius preference: Telegram global settings `bubbleRadius`. Existing source checks assert correct writes. **On-device** test 12/30sp, 0/17dp, Cancel/Reset, force-stop/restart, category view and theme/day/night compatibility. CI can't prove actual persistence through Android process death.

## Strict proxy fail-closed: scope and manual capture

Native tgnet socket guards are in **both** attach and late-opening paths. Guarded Java setter applies across accounts; no direct fallback should occur when strict native proxy ON and selected proxy is lost. Static tests inspect this unchanged behavior. No source-level test proves whole-app DNS, CDN, WebView, calls, Java HTTP or device-wide traffic stays inside the proxy.

On a consented test account and device, record baseline with valid selected proxy; switch to invalid proxy, confirm messaging stops, force-stop/relaunch and switch Wi-Fi ↔ cellular; monitor traffic from phone and proxy/edge simultaneously, then restore the valid proxy. Packet captures may reveal identifiers; **do not publish captures publicly**. Record only pass/fail and sanitized evidence. Do not infer leak-free from a working proxy or VPN icon.

## Release decision

1. GitHub: fast Python/JVM contracts, successful full FOSS ARM64 build, APK signed identity metadata and SHA256SUMS.
2. Devices: Pixel 8a GrapheneOS and Samsung A36 QA, no old-version data loss, verified signing/version continuity.
3. Security: explicit native proxy bad/valid network tests and external egress captures; no DoQ/DoH3 or tracker filtering claims.
4. Distribution: GPL/upstream notices and asset rights, build reproducibility and provenance, stable signing key and privacy policy.

Status is **internal candidate only** until these are checked. The old, working Alpha 0.14d+ artifact remains the safe fallback.

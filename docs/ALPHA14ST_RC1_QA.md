# Ultgram Alpha 0.14 stability RC1 — signing and device acceptance

This debug-signed internal candidate is built from `release/alpha14st-combined-rc1`, based on the user-tested 0.14d+ RC. It retains the same user-visible chat/media/story/privacy features.

## Required automated gates
- Full FOSS ARM64 Android CI completes successfully, including native compilation, signature inspection and artifact upload.
- Artifact `ult-alpha14st-combined-foss-android` contains `ult-alpha14st-combined-foss-arm64-debug.apk`, `SHA256SUMS`, `BUILD_INFO.txt`, `SIGNING_INFO.txt`.
- The signature report contains verified package name, version code, SHA-256 signing-certificate fingerprint, and `Upgrade compatibility: NOT CHECKED`.

## Safe install decision
Keep the existing working Alpha 0.14d+ APK and local Telegram data. Before attempting to upgrade, run on your trusted development machine with a retained APK identical to the **current installed app**:

```bash
python3 scripts/apk_release_gate.py --previous /path/to/current-installed.apk --apk /path/to/new-stabilization.apk
```

The package, signing certificate and version must be compatible. If debug signing differs, do **not** uninstall or clear the existing app just to force the update. A successful Actions APK build is not enough to prove signer continuity.

## On-device checks (Pixel 8a GrapheneOS; Samsung A36)
1. Test existing messaging, eligible story saving, translations, chat categories, protected media and download inspector.
2. Verify saved copy using identical, changed and empty files, cancel, 512 MiB limit, and switch Telegram accounts during file hashing. Results must remain private to the initiating account.
3. Change native font size and bubble corners; Cancel/Apply/Reset, force-stop/restart, light/dark themes and custom palettes.
4. Test strict proxy with valid and deliberately invalid proxies on Wi-Fi/cellular and after force-stop; restore the valid proxy. Check native and non-native egress using device + proxy-side captures, without publishing raw sensitive traffic.
5. Review screenshot FLAG_SECURE and external-link warnings.

## Outstanding release gates
Device tests, signer comparison with the previously installed APK, real packet captures/whole-app DNS+media/VoIP audit, license/asset review, stable release signing and store privacy materials. **Do not promote to public release based solely on passing CI.**

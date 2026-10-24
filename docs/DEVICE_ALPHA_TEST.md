# Ultgram Alpha 0.3 preview — device testing

## Download the internal test APK

1. Open [Android Alpha 0.3 proxy profiles preview](https://github.com/yooshastan/yooshastan-ultmessenger/actions/workflows/android-alpha.yml?query=branch%3Afeature%2Falpha03-proxy-profiles).
2. Select a successful run from `feature/alpha03-proxy-profiles`.
3. Download `ult-alpha03-preview-foss-android` from **Artifacts** and extract the ARM64 APK.
4. On your Pixel 8a or other Android device, inspect the app name/package and install the debug APK.

These are internal debug test artifacts, not signed production releases. They expire after 14 days. If Android rejects an update due to a different debug signing key, do not uninstall your existing working build without arranging for reauthentication and local-data backup.

## Core regression checks

- Open the app and confirm existing Telegram sign-in and chat history remain available.
- Send and receive text and a photo with a consenting contact.
- Switch between ULT Midnight, Candy Pink, Neon Stage and Minimal under **Chat Settings → ⋮ → ULT Presets**; restart and restore System.
- Change Telegram language packs, return to your normal language, and watch for the previous UI-thread freeze.
- Confirm notifications while the Google-free build is backgrounded.

## Alpha 0.3 proxy manager checks

1. Open **Settings → Data and Storage → Proxy Settings**. The page should say **ULT Proxy Manager**.
2. Verify the existing native **Use Proxy**, **Add Proxy**, ping/availability, rotation and Tor controls still behave as before.
3. Add a SOCKS5/MTProto endpoint **you control or trust**. Never use a random public proxy for sensitive accounts.
4. Tap the **info (i)** icon beside that endpoint. Choose **Name this profile**, save a harmless name such as `Home gateway`, and confirm it appears beside the endpoint.
5. Reopen the same icon and choose **Edit connection**. Confirm Telegram's normal proxy editor still appears. Rename the label or clear it; make sure the label persists after restarting.
6. Tap the top-right privacy information icon. Confirm the explanation warns that proxies do not hide your Telegram account identity.
7. Toggle the proxy **only when you intend to change connectivity**. Verify the selected endpoint and connection state. Disable it when done.
8. If using Mercurygram's Tor mode, confirm proxy selection cannot silently override Tor routing.

Aliases are stored only on-device, keyed to the proxy protocol/server/port—not password or MTProto secret. Endpoints with different credentials but the same protocol/server/port share one alias. This preview does **not** implement a kill switch, fail-closed routing or anonymity guarantees.

## Fedora ADB installation and logs

```bash
adb devices -l
adb install -r /absolute/path/to/ult-alpha03-preview-foss-arm64-debug.apk
adb logcat -c
adb logcat -v threadtime -s AndroidRuntime:E ActivityManager:I > ult-alpha03-debug.log
```

Reproduce an issue, stop logging with Ctrl+C, and review logs for private messages, proxy endpoints and account information before sharing.

## Still planned

- Advanced proxy controls such as explicit fail-closed connectivity require separate design and testing.
- Nekogram-inspired chat enhancements and the opt-in SMS/MMS / RCS companion remain future branches.
- Firebase-compatible builds require the optional `ULT_FIREBASE_JSON_B64` secret.
- ULT adds no advertising/analytics; upstream Telegram-sponsored content can still exist.

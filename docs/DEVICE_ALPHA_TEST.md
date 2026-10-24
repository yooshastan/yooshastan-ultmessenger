# Alpha 0.1.1 — download and device smoke test

## Get a test APK from GitHub Actions
1. Open **Actions → Android Alpha 0.1.1 downloadable APK**.
2. Choose the most recent successful run on `feature/alpha01-bootstrap`.
3. Under **Artifacts**, download `ult-alpha011-foss-android` (ZIP).
4. Extract the APK from that ZIP on your phone or workstation.
5. Install only after confirming the run belongs to the expected Yooshastan repository.

These artifacts are **internal debug builds**, not signed releases or Play Store packages. They expire after 14 days. Do not redistribute as a stable release. If the artifact is absent, the build failed or was skipped; check the FOSS job logs.

## Pixel 8a / Samsung A36 smoke test
- Confirm independent package installation alongside your existing Telegram app.
- Open ULT, authenticate using your own test account if possible.
- Send/receive a regular text message and a photo with consenting test contacts.
- Test scrolling in chats and loading media.
- Check Settings → Chat Settings → overflow menu → **ULT Presets**.
- Select Candy Pink or ULT Midnight, restart, and reopen the selector to confirm the selection persists. **Colors do not yet apply app-wide on this stability build.**
- After sign-in, switch to **Settings → Language**, select another language, then switch back. Repeat after a cold start. Watch for stalls or Android's **App isn't responding** dialog. This is the Alpha 0.1.1 remote-language parsing regression test.
- Scroll rapidly through chats while changing or downloading a language pack; note any long freeze or ANR.
- Verify notifications while app is backgrounded on the Google-free build.
- Try the same steps on GrapheneOS without requiring Google Play or microG.

## Install with ADB from Fedora
```bash
adb devices -l
adb install -r /absolute/path/to/downloaded.apk
```

## Collect a reproducible problem report
```bash
adb logcat -c
adb logcat -v threadtime -s AndroidRuntime:E ActivityManager:I > ult-alpha011-crash.log
```
Reproduce the issue, then press Ctrl+C. Review logs for sensitive information before sharing.

## Reporting Alpha 0.1.1 test results
Record the device, Android version, installer (Actions ZIP), build/commit SHA, whether login and message exchange worked, and any freezes (include approximate duration). Send redacted logs if a failure recurs. Do not share SMS/Telegram codes or session information.

If Android refuses to install the update because a previous APK used a different signing key, **do not uninstall without planning for session reauthentication and any locally stored data**. A debug build may be signed with a different ephemeral key on each CI runner.

## Known incomplete features
- Presets are selectable and saved but not applied to Telegram's theme engine yet.
- ULT proxy profile UI, Nekogram-inspired chat layouts, SMS/MMS, and RCS handoff remain on separate development branches.
- Firebase-compatible variant requires separately configured `ULT_FIREBASE_JSON_B64` and is skipped otherwise.
- No Yooshastan analytics or advertisements are added; this does not promise the absence of upstream Telegram sponsored content.

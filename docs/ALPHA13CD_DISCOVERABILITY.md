# Ultgram Alpha 0.13c+d — discoverability and explicit Save As

Base: `release/alpha13ab-combined-rc1` at `69ccf3f`, known from user report to function generally. The 0.13a+b binary and branch are unchanged.

## What's visibly different
- **Ultgram Settings → What's New / build information** is the first row in Settings. Shows source candidate and actual installed Android package version name/code. A version-keyed welcome dialog appears once when Ultgram Settings first opens for this build; not a global startup interrupt.
- **Chats → ⋮ → ULT: Downloads** opens Ultgram's native Telegram-backed active/recent downloads view without opening Settings.
- **ULT Downloads → long-press a downloaded document → Save As** uses Android `ACTION_CREATE_DOCUMENT` to choose a user-visible destination. The exported copy is created in a worker thread. It does not require broad filesystem/storage permissions and never silently downloads media or exports to a cloud destination without user interaction.

## Hard safety boundaries
Save As requires an existing readable local file, no `noforwards`/restricted chat media, no secret chat, and resolved chat restriction metadata. It refuses unavailable media; it does not use an alternate download route or override Telegram's protected-media controls. The Android document provider selected by the user controls the export destination and may be a cloud account; make this obvious in the picker. No bulk export, hidden permission prompts, file manager index or automatic external sync is added.

## Device QA before promotion
1. Confirm changelog appears once when opening Ultgram Settings after installing, remains accessible as first row, shows actual package version and clearly describes new versus prior features.
2. Chats overflow → ULT: Downloads opens correct account's downloads. Existing downloads, open/cancel behavior and normal chat gestures unchanged.
3. Long-press a **completed permitted document**, choose Save As, select destination using Android picker, verify byte-for-byte identity and cancellation behavior. Repeat for pending download (refuse), protected/noforwards media and secret chats (refuse). Do not use sensitive data during smoke tests.
4. Test picker cancellation, nonexistent provider, storage failure and rotation during picker. Ensure no unprompted file is created.
5. Check Pixel 8a GrapheneOS and Samsung A36, both with and without screen privacy enabled, strict-proxy cold start + bad proxy, translation, themes, and per-account tabs.
6. Full ARM64 compilation, artifact SHA256SUMS + BUILD_INFO.txt, signing continuity and licensing review before public release.

**Status:** RC1 source staged; full Android compilation and device QA pending. Not a stable or signed-public release.

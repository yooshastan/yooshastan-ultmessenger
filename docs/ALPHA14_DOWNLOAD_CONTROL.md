# Ultgram Alpha 0.14 — Downloads Control Center (feature)

Built on successfully compiled Alpha 0.14 cache + story-download RC2, preserving story protections, native strict-proxy code and cache settings. Includes a clearly labeled **ULT: Download story** action in the ⋮ menu; the original Telegram Save to Gallery Premium lock is not altered. If a story is protected, private, expired, or unavailable, the menu explains why it cannot save.

## New user-facing controls

In **Ultgram Settings → Downloads** (also via the existing Chats → ULT: Downloads), there is a horizontally scrollable toolbar:

- **Pause all:** pause active entries in this Telegram account's native download queue by calling FileLoader.cancelLoadFile, the same behavior the stock UI uses. This does not remove queue entries.
- **Resume all:** restart paused items with FileLoader.loadFile. A report shows how many actions were taken. Queue order remains Telegram-managed.
- **Network rules:** Telegram's native DataAutoDownloadActivity for **mobile data / Wi-Fi / roaming**, including independent photos, stories, video, documents and per-type file-size limits. It does not change routing/proxy or enforce a new cap.
- **Clear history:** explicit confirmation; calls DownloadController.clearRecentDownloadedFiles (history database only). It does NOT delete downloaded files or cancel running transfers; the app clearly explains this. Native DownloadController.deleteRecentFiles intentionally NOT used, as it also deletes local media.

**Long-press a queued file:** pause/resume the individual file, or move that queued file to the top by calling Telegram DownloadController.swapLoadingPriority. Existing permission-gated Inspect SHA-256, Save As and Verify saved copy remain, never accessible for protected/no-forward messages. The native list remains in charge of transfers, progress and default tap-to-open/cancel behavior.

## QA

- On Pixel 8a GrapheneOS and Samsung A36, queue two or more files, pause all, resume all, pause/resume one, move lower item above another. Test on Wi-Fi and cellular, after app restart and with strict proxy enabled / invalid proxy. No silent direct bypass.
- Open native Network rules, change per-network auto-download types and maxima; confirm on reopening (also from Telegram Data and Storage). This is **automatic download behavior**, not a direct blocker for manual transfers or a whole-app VPN.
- Clear history with at least one recently downloaded file; verify the history list empties but the file is still present and readable. Cancel must not change it.
- Long-press protected/no-forward media: no export actions; transfer pause/resume can still apply to a queued item without exporting bytes. No network metadata or file names logged.
- Story save, Cache Run Maintenance, photo/video playback and saved-copy verification still work. Observe 24-hour cleanup behavior unchanged.
- Full Android native compile and signer verification, and matching installed APK certificate/version before upgrade.

**Caveat:** native FileLoader.cancelLoadFile is a pause/cancel of active transfer as exposed by Telegram; resume initiates that same item. This is not an all-network kill switch or a guarantee that already in-flight packets cease instantaneously.

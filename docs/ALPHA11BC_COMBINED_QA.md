# Alpha 0.11b+c combined device and release checks

- Build: single batched FOSS ARM64 Android APK on release/alpha11bc-combined-rc1, after light checks. Preserve source and behavior of tested native strict-proxy guard.
- **Network review:** In Ultgram Settings → Network/DNS source audit matrix, inspect/copy source-level findings. This is an inventory, not app traffic verification. No live DoQ, DoH3, tracker enforcement or packet capture.
- **Chat UI:** In Ultgram Settings → Customize chat category tabs, hide several categories; all must remain. Reorder visible tabs; confirm layout after leaving/reopening Chats. Confirm per-account isolation and reset to default. No Telegram folders or chat data should be changed.
- **Translation:** In Ultgram Settings → Offline translation status, check local provider presence and selected engine. Confirm missing provider + offline mode is disclosed and fails closed on secret chats. No offline translation model is bundled; remote modes require informed consent.
- **Regression:** strict-proxy ON for cold start and no toggle, document and media actions, 0.11a.1 FLAG_SECURE including Settings dialogs, two/three line previews, messaging and channels.
- **Capture gates:** phone edge + tunnel inner + proxy host; IPv4/IPv6, proxy down, handoff. Release still must not claim full egress privacy.

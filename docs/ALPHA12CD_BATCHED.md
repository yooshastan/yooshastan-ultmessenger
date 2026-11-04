# Alpha 0.12c+d — gesture navigation and safe media tools

Branch: `feature/alpha12cd-swipe-media-ux`
Baseline: Alpha 0.12a+b RC1 (`15cc9a1`) reported working on Samsung A36 and Pixel 8a.

## Implemented
- **Opt-in category swipe pad:** a dedicated ↔ target appears beside the optional floating category chips. Swipe horizontally on **that target** only to navigate backward/forward over enabled categories. Tap to advance. Never intercept Telegram chat rows, native folder tabs, or Android edge-back gestures. Disabled by default, saved separately for each account.
- **Category label typography:** Small/Normal/Large (12/13/16 sp), honoring Android font scaling and separate per-account preferences.
- **Media context tools:** explicit long-press Copy safe file name / Copy media citation (message ID, optional UTC/public link, readable sanitized file name, MIME and bytes). Only a valid public channel/supergroup gets a public t.me link. No private/secret permalink, automatic download, export, message content or network call.
- **Lightweight contract tests** and pure Java policy tests on feature-branch pushes.

## Safety / limitations
Native strict proxy and network routing are unchanged. Swipe does **not** operate across the whole chat-list surface: this deliberately avoids collisions with Telegram gestures; only the dedicated control is gesture-enabled. Full download manager, file save/export, advanced editor and direct Nullgram/Nekogram code reuse remain future steps. Clipboard data can be accessed by the OS and keyboards; only copy metadata intentionally.

## Before release
Review pinned upstream patch anchors, compile one combined FOSS ARM64 debug APK, check Galaxy A36 and Pixel 8a (swipe in both directions, tab filtering, per-account preferences, copying private-vs-public media), cold-start strict proxy, translation and screenshot dialogs, and sign only after public-release review.

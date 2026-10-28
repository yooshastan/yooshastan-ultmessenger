# Ultgram 0.13c — find the features

This source-only update adds a **START HERE** section at the top of Ultgram Settings. It does not change Telegram's chat-list home UI or network transport.

- **What's new in Ultgram 0.13** — clear distinction between truly new theme editing and downloads screen; pre-existing Telegram auto-download configuration shortcut; information-only identity/privacy disclosures; prior Alpha 0.12 features; unfinished Save As and file-management items.
- **Downloads — open queue** — visible near the top of Ultgram Settings and still accessible from Chats and Tools, using the previously introduced upstream Telegram downloads view.
- **Version and build details** — dynamically read the *actual installed APK* PackageInfo versionName and versionCode. The named feature milestone is a source-level development label, not a signature or release provenance assertion.

QA: verify on Samsung A36 and Pixel 8a; ensure entry points navigate, version reads correctly, theme editor works, downloads view handles regular files and account switching, and secure-dialog screenshot settings remain effective. More ambitious download status, Save As and a Whats New entry from the main chat screen are future work.

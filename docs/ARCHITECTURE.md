# Architecture — Yooshastan ULT Messenger

**Status:** Alpha 0.1 bootstrap; not yet a compiled Telegram client.

## Purpose
A third-party Android Telegram client with an optional SMS/MMS module, an experimental RCS companion, a proxy manager, and locally implemented customization features. No Yooshastan-provided advertising or tracking.

## Foundation
- Upstream: https://github.com/DrKLO/Telegram (GPL-2.0).
- Keep upstream Android modules intact where practical.
- Place custom additions under `com.yooshastan.ultmessenger` inside the upstream app module, with narrowly scoped integration points.
- Track upstream commit SHA and imported-code provenance before making a distributable build.

## Subsystems (planned, not implemented)
1. Appearance: Nullgram-inspired theming and layout settings.
2. Chat tools: select Nekogram, Goregram, and Plus Messenger features.
3. Proxy: Telegram MTProxy/SOCKS5 profiles, testing, and opt-in routing. A proxy is not account anonymity.
4. Local extras: client-side themes, transcriptions, and media convenience; do not bypass Telegram's server-side Premium entitlements.
5. SMS/MMS: opt-in default SMS app using Android Telephony and RoleManager APIs.
6. RCS companion: preserve Google Messages as provider; use only authorized handoff and supported integration. No guaranteed full RCS access.

## Compliance
- Obtain a project-specific Telegram API ID and hash; never publish signing passwords or private keys.
- Preserve GPL license notices and release corresponding source.
- Do not block official Telegram sponsored messages, manipulate read/typing indicators, or defeat self-destruct semantics.
- Independent icon/branding; clearly disclose Telegram API use.

## Milestone M1
Build official upstream unchanged first. Then rebrand, install alongside Telegram, verify login, send/receive, media, calls, and notifications on Pixel 8a. Only after passing M1 begin appearance changes.

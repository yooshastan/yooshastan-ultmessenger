# Phase 2 — Nullgram-inspired ULT appearance

**Scope:** Bring customizable appearance into ULT Messenger after the first Android APK builds.

## First increment (this branch)
- Five immutable palette presets with an explicit system fallback.
- Store preset names, not ordinal indexes, to preserve settings across upgrades.
- Pure-Java unit tests for stable preset decoding.
- Separate Yooshastan styling from Nullgram's copyrighted assets and artwork.

## Planned UI integration
1. Examine Nullgram's theme and navigation customization at a pinned, GPL-compliant revision.
2. Add a *Yooshastan Appearance* settings entry inside Telegram's existing settings navigation.
3. Persist selected preset per Android app install; future extension may permit per-account/per-chat choices.
4. Apply colors through Telegram's Theme engine, never by rewriting Telegram protocol or message handling.
5. Keep standard Telegram themes selectable, and provide a reset-to-system option.
6. Test light/dark, OLED contrast, dynamic font scaling, RTL, emoji/sticker views, chats, and calls.

## UX palette sketches
| Preset | Accent | Background | Mode |
|---|---|---|---|
| System | #1976D2 | #F5F6FA | Follow system |
| ULT Midnight | #E86BCB | #171327 | Dark |
| Candy Pink | #E3499B | #FFF0FA | Light |
| Neon Stage | #5DF1E6 | #11182A | Dark |
| Minimal | #727B8A | #F7F8FA | Light |

This branch contains a working, build-independent preset model **but no integrated Telegram settings screen yet**. Do not claim these are usable in the APK until wired to the actual theme engine.

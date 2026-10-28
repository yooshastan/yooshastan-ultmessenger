# Ultgram Alpha 0.12a+b — Nullgram/Nekogram-inspired local UX

**Baseline:** Pixel 8a-accepted Alpha 0.11b+c RC1 commit `e593a8e`; strict native proxy unchanged.
**Development:** `feature/alpha12-nullgram-nekogram` (draft). No production signing or APK is implied.

## Delivered in the first lightweight batch

### 0.12a — category-bar appearance
- Compact / Comfortable / Spacious density for the optional six-category chip strip.
- Per-account local preference and reset-to-default in Ultgram Settings.
- Changes only bar height and chip height/padding in the existing guarded DialogsActivity integration; does not change Telegram chat-list cells, server folders, dialog order, message storage or themes.
- Reopening Chats refreshes the chip layout. This is not yet swipe navigation, an editable theme engine, or general chat-list density.

### 0.12a — read-only paired theme previews
- Ultgram Settings offers original day/night palette previews of the six existing ULT presets.
- Uses immutable `UltAppearancePreset.forMode(boolean)` colors already compiled into the app.
- Does not apply or save a theme, borrow third-party assets, or change native day/night settings.

### 0.12b — message reference utility
- Explicit long-press **ULT: Copy message reference** combining message ID, sender peer ID (if present), UTC timestamp (if valid), and link only for an actual public channel/supergroup with validated username and positive message ID.
- No message text, attached media or file content copied. Never synthesizes secret/private links.
- Clipboard transfer is user-triggered and may be visible to the OS/keyboard or other clipboard readers. No background network request or Telegram write.

## Batch build and nonregression
- On feature pushes / PRs, run lightweight Python source/integration contracts and pure Java policy tests.
- Delay pinned Mercurygram SDK/native compilation to a future combined Alpha 0.12a+b RC branch after feature scope and tests are ready.
- Preserve `scripts/integrate-native-strict.py`, prior startup recovery, DoQ non-enforcement disclosures and Alpha 0.11 screenshot protections.
- Before a candidate APK, verify patch anchors against pinned upstream and run the full FOSS ARM64 CI. Device QA must cover strict-proxy ON cold start, media/messages, themes, screenshot dialogs, bar layout per account and metadata copying.

## Next additions in this combined milestone
- Review safe swipe gestures around horizontally scrollable category bar (avoid hijacking chat list and Android back gestures).
- Continue appearance improvements with font/accessibility checks and optional accent editing.
- Expand media/file action UX using Telegram's existing permission-aware storage manager; do not bypass protected-content controls or auto-download/export messages.
- Record upstream Nullgram/Nekogram revision and licenses before any code borrowing. This increment uses original Ultgram code only.

**Honesty gate:** no live tracker blocking, DoQ routing, anonymity guarantee, new offline language model, server Premium bypass or general media downloader is claimed.

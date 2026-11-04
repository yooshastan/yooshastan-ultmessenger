# Ultgram Alpha 0.12a+b RC1 — first on-device candidate

**Branch:** `release/alpha12ab-combined-rc1`
**Source baseline:** `feature/alpha12-nullgram-nekogram`, commit `db81660`
**Build:** FOSS ARM64 debug APK via `.github/workflows/android-alpha.yml`, with SHA256SUMS and BUILD_INFO.txt.
**Purpose:** internal device testing only; not a production-signed GitHub Release.

## Included in this candidate
- Alpha 0.11b+c tested functional baseline: strict native proxy startup, existing dialog screen privacy, local chat categories, translation gates.
- **Alpha 0.12a:** Compact / Comfortable / Spacious per-account category strip appearance, reset.
- **Alpha 0.12a:** read-only paired light/dark swatches for six Ultgram palettes; does not apply themes from the preview.
- **Alpha 0.12b:** optional long-press Copy message reference combining message ID, optional sender, valid UTC and eligible public link. No message text, private/secret links or file export.

## Device QA
1. Back up before upgrading; debug signing identity/versionCode may prevent direct install over an older build. Never uninstall without accounting for local unsynced data or preferences.
2. Start with strict native proxy ON, cold-restart, send message, download a document. Confirm no new toggle recovery or fallback.
3. In Ultgram Settings, select Compact and Spacious category bar appearance, leave/reopen Chats, inspect chip size and per-account persistence; reset to Comfortable.
4. Preview Candy Pink and Aurora (both Day and Night). Ensure preview does not apply a theme; previously chosen palette remains after closing the dialog.
5. Long-press a channel post and choose Copy message reference. Verify ID, UTC and public link; in a private or secret chat, the output must never include a fabricated public link.
6. Verify screenshot protection covers Ultgram Settings and preview dialogs, chat navigation, translation status/consent and existing media details.

## Known limitations
- Swipe gesture navigation, editable accent colors, native chat-list cell density, managed downloads and file exports are **not included** in this first Alpha 0.12 candidate.
- The existing network matrix is read-only, with no new DoQ/DoH3 backend, tracker enforcement or whole-app leak proof.
- Client-only conveniences do not unlock Telegram server-side Premium.
- Do not publish this debug-signed APK as a stable/public app before signing, versioning, source/license and cross-device review.

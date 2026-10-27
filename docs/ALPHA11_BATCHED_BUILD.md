# Ultgram Alpha 0.11b + 0.11c combined development

**Goal:** Reduce native Android compilation overhead while retaining safety regressions.

## Build workflow
- Base: `feature/alpha11a-local-privacy` (Pixel-reported functional, Android CI green).
- Development: `feature/alpha11bc-batched`. Push/PR uses `.github/workflows/scaffold-check.yml` and a lightweight set of Python + pure Java contract tests (network capture classifier, screenshot privacy contract, chat views and translation). No full Gradle, SDK setup, or fresh upstream native compilation on each push.
- One combined integration target: `release/alpha11bc-combined-rc1` (create only when both stages' changes are ready). The Android workflow compiles a FOSS ARM64 APK there, includes SHA256SUMS and BUILD_INFO, and can also be run deliberately with `workflow_dispatch`.
- A failed combined compile should be fixed with targeted Java/Python checks and a subsequent RC build. There is no promise of exactly one build if integration fails.
- No developer needs to keep screenshots, account data, or secrets in CI artifacts.

## 0.11b — network/DNS audit stage (read-only first)
- Inventory Java HTTP and hostname resolution, tgnet native sockets and proxy selection, CDN/media paths, WebViews and voice-call UDP from pinned upstream source.
- Record source entry points, trust boundaries, and missing evidence. Validate manual capture-text classifier against synthetic input, including IPv4/IPv6 and proxy-down test scenarios.
- Use trusted gateway/phone edge plus tunnel-inner and proxy-host capture for any future routing proof. Android VPN transport indicator alone is insufficient. Do not run a second VPN concurrently with Wonhee.
- **No live DoQ, DoH3, tracker filtering, leak prevention or whole-app kill switch is implemented by this batch policy.**

## 0.11c — chat and translation (local UX first)
- Add opt-in per-account category visibility and order, always exposing All and a reset path. Preserve Telegram server folders, pinned/archived data, and selected account boundaries.
- Improve offline translation availability/engine selection. A missing offline provider must fail explicitly; secret chats must not fall back to remote translation.
- Test per-account state, theme, scroll/navigation and translation consent on Pixel 8a/A36.

## Release / safety gates
- Do not alter `scripts/integrate-native-strict.py`, native tgnet guard policy, or enable unreviewed fallback. The new work is isolated from those paths.
- Successful lightweight contracts **do not** prove Android compilation or routing security. Full FOSS Android CI is mandatory before any APK installation.
- Final device QA checks: strict proxy ON at cold start without toggling, messages and media, settings/privacy, chat categories, translation fail-closed, screenshot dialog protection.
- Public distribution still requires owned signing, version/upgrade QA and security/licensing review.

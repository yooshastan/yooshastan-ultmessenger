# Ultgram — Yooshastan ULT Messenger

Ultgram is an **unofficial Android Telegram client**, built on the
[Mercurygram](https://github.com/Mercurygram/Mercurygram) FOSS source and the
[Telegram Android](https://github.com/DrKLO/Telegram) platform. It is not
affiliated with, sponsored by, or endorsed by Telegram or Mercurygram.

## Development status

**Beta 3.2 internal candidate:** `16.0.0-beta03.2`, code `7183014`.
The FOSS ARM64 build passed GitHub Actions, and the user confirmed that it
installed through ADB and passed all five device smoke tests. It is **not a
public release**.

- Internal test package: `com.yooshastan.ultmessenger.foss.beta`
- Latest [verified internal test build](https://github.com/yooshastan/yooshastan-ultmessenger/actions/runs/41776629944)
- Permanent public FOSS package planned: `com.yooshastan.ultmessenger.foss` (separate installation)
- Public release signing: **in preparation**, not complete; the first protected
  workflow run failed its source-test preflight before invoking any signing

## What Ultgram focuses on

- Native Telegram messaging, chat folders, search, notifications and media.
- A working, locally managed strict-proxy guard for Telegram native sockets.
  It is **not** verified whole-app or whole-device leak-proof protection.
- Chat translation, themes, simplified chat controls and compatible story saving.
- One Downloads screen using Telegram's native queue, history, cache and
  per-network download settings.
- Read-only connection diagnostics and clear limitations, rather than inactive
  privacy toggles marketed as enforcement.

The current internal candidate removes duplicate navigation and extra
message-copy actions. **SMS/RCS is not implemented in the APK**; the separate
research branch is excluded. Experimental DoQ/DoH3/tracker controls are not
active network protections.

## Build and test

The pinned Mercurygram source commit is stored in
[`upstream/mercurygram.sha`](upstream/mercurygram.sha). Integration scripts
under `scripts/` apply Ultgram modifications to a **disposable** checkout.
See [the ARM64 internal build workflow](.github/workflows/android-beta.yml)
for the exact dependency, toolchain and integration steps.

Before distributing any APK, verify its package ID, version code and signing
certificate and complete device testing:
[Beta 0.3 QA](docs/BETA03_COMBINED_QA.md).

## Public release preparation

A [protected release workflow](.github/workflows/android-foss-release-sign.yml)
is under review. It produces an unsigned FOSS ARM64 release on a keyless
runner, then signs in a protected GitHub environment with a privately
managed publisher key. The workflow is manual-only and its output is a
**review artifact**, not an automatically published release.

See [signing and upgrade instructions](docs/BETA_RELEASE_SIGNING_PLAN.md).
Do not commit signing keys, Telegram API secrets or developer credentials.
Debug builds and release builds use different package identities; do not
uninstall an existing app merely to force an update.

## Source and licensing

The repository contains the GNU GPL version 2 license text. Ultgram
modifies and builds GPL-licensed Telegram/Mercurygram software, and
corresponding-source, copyright-notice and third-party dependency obligations
apply. See [source attribution](docs/SOURCE_ATTRIBUTION.md) and the pinned
upstream repository for complete third-party notices.

Uses the Telegram API. Distribution must comply with
[Telegram's API terms](https://core.telegram.org/api/terms).

Historical alpha development notes remain available in
[the archive](docs/ALPHA_DEVELOPMENT_ARCHIVE.md).

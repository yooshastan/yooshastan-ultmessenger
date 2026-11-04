# Source and license attribution — Ultgram

Ultgram is an **unofficial, third-party Telegram client**. It is not
affiliated with or endorsed by Telegram, Mercurygram, or their contributors.

## Where the Android application code comes from

- **Telegram Android:** [DrKLO/Telegram](https://github.com/DrKLO/Telegram).
  Telegram Android source is licensed under GPL-2.0; retain applicable notices
  and source obligations.
- **Mercurygram:** [Mercurygram/Mercurygram](https://github.com/Mercurygram/Mercurygram).
  The FOSS Android candidate is built against pinned Mercurygram revision
  `ff1ef95c8dfec5be9d15d50b9f4685a830f3bdd5` (see
  `upstream/mercurygram.sha`). That source contains additional upstream
  modifications and its own acknowledgments/dependency licensing.
- **Ultgram:** this repository provides integration scripts, independent
  branding, locally implemented changes, tests and documentation that are
  applied to the pinned Mercurygram source during a build.

The top-level `LICENSE` contains the verbatim GNU General Public License
version 2 text from the pinned upstream. Copyright in upstream source and
third-party dependencies remains with their respective holders. Please
preserve their notices when distributing source or binaries.

## Reconstructing corresponding source

For a given Ultgram release, retain and publish:

1. The exact Ultgram commit used to build it (not just a moving branch name).
2. The pinned Mercurygram commit from `upstream/mercurygram.sha`.
3. All Ultgram integration scripts and any local patches used by the build.
4. The complete build instructions, dependency and toolchain versions,
   plus appropriate upstream and third-party license notices.
5. An explanation of which artifact is an internal debug test versus a
   privately signed public release.

A link to an unmodified upstream revision is not a substitute for providing
the modifications required to reproduce the shipped application.
Before publishing publicly, audit third-party dependencies, artwork,
trademarks, licenses and Telegram's current API terms.

## Privacy claims

The native strict-proxy guard is not a whole-device VPN or proof that every
DNS query, Java/WebView connection, call or media path uses the proxy.
Do not describe inactive DoQ/tracker prototypes as enforced protection.

The separate SMS/RCS companion experiment is **not** integrated into the
released beta and does not grant SMS permissions.

# Ultgram Alpha 0.14a+b — visible local media and theme tools

Baseline: `release/alpha13cd-combined-rc1`, commit `1a1350f0dc93b24d1588bab72ee847ef8e1684cb`. User reported the build functioned as expected; the prior APK and proxy logic stay untouched.

## Implemented on the feature branch (before native compile)
- **Downloaded document inspector:** ULT Downloads long-press now offers explicit "Inspect file and SHA-256" or "Save As". Only already downloaded readable, nonprotected documents can be inspected. SHA-256 is streamed on a background thread, capped at 512 MiB, without new network requests. The user must explicitly tap "Copy SHA-256" to use the clipboard; Android may expose clipboard contents to other components. The displayed checksum covers the **local downloaded file**, not a separately saved destination.
- **Visual fixed-theme preview:** the custom theme editor now displays sample header, incoming and outgoing bubbles with candidate colors, validates contrast via the existing policy, and does not mutate the active Telegram theme until the separate Apply tap.
- New pure-Java and source integration tests. Reuse existing secure dialogs where practical, preserve upstream file transport, protected-media rules, all per-account chat settings, strict proxy, and screen protection.

## Remaining / next milestone
- Real per-chat local display identity masking, media sending metadata privacy preview (EXIF location stripping only opt-in), richer navigation polish, improved export progress/cancellation, whole-app network egress audit. None is claimed implemented.
- One combined ARM64 FOSS build **after** tests pass and source integration is reviewed, not per minor feature edit.
- Device QA: Pixel 8a GrapheneOS + Samsung A36; protected media must be refused; large-file limit must be clear; Settings theme preview must never apply inadvertently; system day/night themes and existing Downloads, Save As, messaging and strict proxy must work.
- Release signing/distribution, APK upgrade certificate check, GPL notices and privacy review are separate gates.

# Alpha 0.1 build variants (experimental)

| Variant | Source | Firebase | Intended use |
|---|---|---|---|
| `firebase` | Official [Telegram Android](https://github.com/DrKLO/Telegram) at `upstream/telegram.sha` | Yes | Baseline compatibility and messaging |
| `foss` | [Mercurygram](https://github.com/Mercurygram/Mercurygram) at `upstream/mercurygram.sha` | No Google Firebase dependency by default | Google-free push and GrapheneOS evaluation |

These are two separately pinned source trees during Alpha 0.1, **not yet two release flavors from one unified fork**. We will converge source/patch infrastructure after build feasibility is proven. The Google-free branch inherits Mercurygram's GPL-2.0 obligations, upstream attributions, and native build requirements.

## Credentials
The app ID and hash are sourced from the existing GitHub Actions secrets `ULT_TELEGRAM_API_ID` and `ULT_TELEGRAM_API_HASH`. The FOSS configuration writes a temporary `API_KEYS` file in the disposable checkout; do not commit it.

The Firebase-compatible variant additionally requires project-specific Google configuration for both the base package and `.beta` debug package. Set `ULT_FIREBASE_JSON_B64` as the base64-encoded Firebase `google-services.json`. It is currently not configured or verified. Do not embed Telegram's checked-in placeholder Firebase config in a distributed APK.

## Current status
- Two pinned source intake paths and separate credential-injection scripts.
- CI compile probes planned; no successful compiled APK as of this checkpoint.
- Separate debug package names allow simultaneous installation after manifest/provider review.
- Replacing all Telegram/Mercurygram marks and completing end-to-end notification testing are required before distribution.

## Build requirements
Telegram: SDK 36, NDK 27.2.12479018, CMake 3.22.1, JDK as required by pinned Gradle toolchain.
Mercurygram: JDK 17, Ninja, Meson, nasm, make, cmake, pkg-config, gperf, Python, Rust/WASI tooling provisioned by upstream build scripts. Native library compilation can exceed standard CI time, disk, and memory budgets.

## Notification compatibility target

Do **not** require microG for the entire app. It can serve as an optional Firebase Cloud Messaging compatibility provider on supported Android installations, but still uses Google's messaging service infrastructure. A compatible Firebase client must also have its app registration configured correctly.

For the FOSS variant, favor UnifiedPush with a user-selected distributor, then Telegram's keep-alive/background connection as fallback. Existing Mercurygram mechanisms are reference implementations; they must be tested and rebranded rather than assumed to work unchanged.

GrapheneOS users may alternatively opt into sandboxed official Google Play services. microG does not solve Google Messages RCS provisioning or give third-party clients a general-purpose RCS API.

# Local Ultgram ARM64 builds on Fedora 44

The GitHub Actions workflow `.github/workflows/android-beta.yml` is the authoritative
build recipe. `scripts/build-ultgram-local.py` reproduces the pinned FOSS ARM64
**debug** build locally without modifying your regular Telegram/Ultgram apps.

Local builds retain the Android SDK, pinned Mercurygram source, native tool
outputs, and Gradle caches. The first setup is still substantial; later
rebuilds should avoid repeated downloads and much unnecessary compilation.

## 1. One-time workstation prerequisites

Install development utilities:

```bash
sudo dnf install -y git python3 ninja-build meson nasm gperf \
  pkgconf-pkg-config make cmake curl unzip git-lfs android-tools
```

The project's GitHub CI uses **JDK 17**, not Fedora 44's default newer JDK.
On Fedora 44 you can install Temurin 17 through Fedora's Adoptium repository
configuration package (an external, signed package source):

```bash
sudo dnf install -y adoptium-temurin-java-repository
sudo dnf install -y temurin-17-jdk
```

See [Fedora's Adoptium repository package](https://packages.fedoraproject.org/pkgs/adoptium-temurin-java-repository/adoptium-temurin-java-repository/)
and [official Temurin Linux packages](https://adoptium.net/installation/linux/).
The build helper finds a JDK 17 installed under `/usr/lib/jvm/`, even if
another Java version is selected by default.

**Android SDK:** Use Android Studio's SDK Manager to install **SDK Command-line
Tools**; the helper uses `sdkmanager` to install the exact requirements:
Android API 36, Build Tools 36.0.0, NDK 27.2.12479018 and CMake 3.22.1.
Set `ANDROID_HOME` if the SDK isn't in `~/Android/Sdk`.

Example:

```bash
export ANDROID_HOME="$HOME/Android/Sdk"
"$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager" --licenses
```

The build helper never downloads an SDK installer itself.

## 2. Clone the development branch and build

```bash
git clone --branch feature/auto-video-link-downloads \
  https://github.com/yooshastan/yooshastan-ultmessenger.git ~/Ultgram-local
cd ~/Ultgram-local
python3 scripts/build-ultgram-local.py build
```

On first use, the helper downloads the pinned Mercurygram source and native
submodules and locally asks for **your own Telegram API ID and hash** from
`my.telegram.org`. GitHub Actions Secrets are not automatically available
on your PC. The hash prompt is hidden; the local `API_KEYS` file stays within
the ignored `.work/` directory, with restricted file permissions. Never paste
these credentials into chat, commit them, or upload them.

Build output appears in `.work/ultgram-local-artifacts/`, alongside
`SHA256SUMS` and `SIGNING_INFO.txt`. Before installing, the helper calls the
repository's read-only APK signature/package/version verification gate.

## 3. Fast repeat builds, then updates

For another build of the *same* Ultgram commit:

```bash
python3 scripts/build-ultgram-local.py build
```

After pulling a **new Ultgram repository commit**:

```bash
git pull --ff-only
python3 scripts/build-ultgram-local.py refresh
python3 scripts/build-ultgram-local.py build
```

`refresh` explicitly resets and cleans **only the helper-owned disposable
checkout** at `.work/mercurygram-upstream`; it can remove edits and untracked
files there. Do not manually edit that generated source tree. Ignored Gradle
build outputs and the user-level Gradle dependency cache are retained.

Build an internal test APK locally; install it only once its package, version
and signer have been checked. The optional install command is:

```bash
python3 scripts/build-ultgram-local.py install
```

The helper calls **`adb install -r`**, never `adb uninstall`, and never
silently removes app data. If an existing GitHub-built APK has a different
signing certificate, Android will refuse the in-place update. **Do not uninstall
the working app to bypass a certificate mismatch.** The local debug APK uses
the pinned upstream's debug signing configuration, not your permanent release
signing identity. For public distribution, use the separate release-signing
procedure and perform routing/privacy/device QA.

## Troubleshooting

- **git-lfs filter-process: command not found** during first checkout:
  `sudo dnf install -y git-lfs && git lfs install`. Then run `git pull --ff-only`
  from `~/Ultgram-local` to fetch the updated helper and rerun
  `python3 scripts/build-ultgram-local.py build`. The helper now resumes
  the interrupted pinned `wallet-engine` submodule checkout without deleting
  existing downloads. If the checkout has locally edited files or a partial
  `API_KEYS` file, it refuses a destructive retry; inspect that state first.

- **JDK version error:** Install Temurin 17; the helper searches for an installed
  JDK 17 instead of using Fedora's default Java version.
- **sdkmanager missing:** Install the Android SDK Command-line Tools in Android
  Studio; point `ANDROID_HOME` at the SDK directory.
- **Maven HTTP 429:** Retry after the remote limit clears. Once dependencies
  have been downloaded, local Gradle caches reduce repeated network access.
- **Source prepared against an old Ultgram commit:** Run the explicit
  `refresh` command above.
- **INSTALL_FAILED_UPDATE_INCOMPATIBLE:** Keep the existing app. Compare
  `SIGNING_INFO.txt` with the installed APK signer before changing signing.
- **Instagram still fails:** A successful build does not prove a Reel extractor
  can overcome Instagram authentication/bot challenges. This is tracked in
  [issue #71](https://github.com/yooshastan/yooshastan-ultmessenger/issues/71).

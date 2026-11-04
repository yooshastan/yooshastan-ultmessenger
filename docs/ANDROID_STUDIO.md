# Open Yooshastan ULT Messenger in Android Studio

The repository ZIP is a **bootstrap**. The complete Telegram-compatible Android project is pulled from a pinned upstream revision when you run the preparation script. This avoids distributing a stale or incomplete source ZIP and preserves upstream history and licensing.

## Fedora / Linux

1. Download the [Alpha 0.1 source ZIP](https://github.com/yooshastan/yooshastan-ultmessenger/archive/refs/heads/feature/alpha01-bootstrap.zip) and extract it, or clone the branch with Git.
2. Install Git, Python 3, Android Studio, JDK 17, and Android SDK/NDK support. On Fedora, Git/Python prerequisites can be installed with `sudo dnf install git python3`.
3. From the extracted `yooshastan-ultmessenger-feature-alpha01-bootstrap` folder, run:

```bash
bash scripts/prepare-android-studio.sh foss
```

4. Enter your personal Telegram API ID/hash when prompted. The hash is read without terminal echo. The generated source stores it in an ignored/private `API_KEYS` file.
5. In Android Studio, choose **File → Open** and select the prepared folder:
   `.work/mercurygram-upstream`
6. Wait for Gradle sync. Install the SDK, NDK, CMake, and build prerequisites requested by the pinned upstream project, then select an Android debug build variant.

### Build an ARM64 debug APK locally

The pinned Mercurygram revision requires an explicit version tag. In the prepared Android source directory, use your Temurin JDK 17:

```bash
JAVA_HOME=/usr/lib/jvm/java-17-temurin-jdk bash ./gradlew :TMessagesProj_App:assembleAfatFdArm64Debug -PMG_BUILD_TAG=13.0.3.0.1 --no-daemon --console=plain
```

This build targets ARM64 phones such as Pixel 8a and Samsung A36. You can also add `MG_BUILD_TAG=13.0.3.0.1` to the generated source's ignored `local.properties` for Android Studio build buttons. Keep Android Studio's Gradle JDK set to Temurin 17. These instructions are only for the Mercurygram-based FOSS variant.

**Important:** This initial Google-free Android project uses the independently pinned Mercurygram source tree. The setup adds the ULT package/branding and the **ULT Presets** menu in Chat Settings. The preset is currently saved but does **not** yet recolor all Telegram screens.

### Optional Firebase-compatible build

Run `bash scripts/prepare-android-studio.sh firebase` **from a clean source ZIP or when that target folder does not exist**. You also need a Firebase `google-services.json` registered for the ULT package(s). The script prompts for its path. Open `.work/telegram-upstream` in Android Studio.

### Limitations

- The setup downloads large native Git submodules. It requires a stable connection and considerable disk space.
- Neither local Gradle sync nor a complete Android APK build has yet been verified by this specific setup workflow.
- **Do not upload or commit the generated `.work` directory, `API_KEYS`, Firebase configuration, or signing keys.**
- The Android source is GPL-derived; preserve notices and publish corresponding source before distribution.
- If preparation is interrupted, the target directory remains present. Inspect it before moving or deleting it; the script intentionally refuses to overwrite anything.
- Linux is the tested scripting target. Windows can run this through WSL, but opening a project across the Windows/WSL filesystem may require additional setup.

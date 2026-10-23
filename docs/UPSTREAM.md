# Upstream intake

Upstream repository: https://github.com/DrKLO/Telegram

Pinned Git revision: see `upstream/telegram.sha`. This was obtained from the advertised HEAD on 2026-10-23 and must be refreshed deliberately after reviewing upstream changes.

## Fetch and verify

```bash
bash scripts/fetch-upstream.sh
git -C .work/telegram-upstream rev-parse HEAD
```

The helper refuses to overwrite an existing source directory. Its default checkout lives in `.work/telegram-upstream`, which is excluded from this scaffold repository.

## Integration policy
The current repository is an overlay/bootstrap, **not yet a complete fork**. To publish a derivative APK, integrate the corresponding Telegram source into a publicly available source tree or preserve an exact reproducible source+patch build, comply with GPL, and document every patch and build input. Do not imply the upstream APK is a Yooshastan release.

Before compiling, provide independent API credentials, signing configuration, Firebase setup as needed, and replace sample/dummy release artifacts from upstream. Inspect the pinned Gradle variants before automating an APK build.

## Configure local Alpha 0.1 identity (no secrets in Git)

Create your own application at https://my.telegram.org. In a secure local shell, supply the project-specific API ID and hash as environment variables, then run:

```bash
ULT_TELEGRAM_API_ID=<your-id> ULT_TELEGRAM_API_HASH=<your-hash> \\
  python3 scripts/configure-prototype.py .work/telegram-upstream
```

For GitHub Actions, add repository **Actions secrets** named `ULT_TELEGRAM_API_ID` and `ULT_TELEGRAM_API_HASH` under Settings → Secrets and variables → Actions. Do not put their values in repository files, issue comments, build logs, or screenshots. The current CI does not consume them; credentialed build configuration is a separate step.

The patch changes package ID, API identity, passkey support, upstream update checks, and the application label in relevant manifests. It does **not** replace Telegram logos, configure Firebase, produce signing material, or prove successful login. Do not distribute the resulting binary until those remaining changes are done.

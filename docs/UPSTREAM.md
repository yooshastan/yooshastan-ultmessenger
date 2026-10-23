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

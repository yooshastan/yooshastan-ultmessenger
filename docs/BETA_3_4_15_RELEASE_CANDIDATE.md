# Ultgram 16.0.0-beta03.4.15 — release candidate

Internal test versionCode 7183031. Public release candidate uses the same
version metadata but package com.yooshastan.ultmessenger.foss, not .foss.beta.
This is a candidate, not a publication or verified signed production APK.

## Scope

- Retain Beta 3.4.14's shared video extraction and Telegram posting path.
- Replace generic copy icons with owned media-action vectors.
- Exact-version debug installer: verify package, APK version, signature,
  current source commit, SHA-256, and post-install device version.
- Invalidate the build receipt before compiling so a failed build cannot
  cause an old same-version artifact to be installed. Do not select by mtime.
- Use a fresh Gradle process with existing build cache, no clean.
- Align protected release workflow version and integration order with the
  tested debug candidate. Feature-branch compilation is unsigned; private
  signing is manual on main only.

## Before merge and Uptodown upload

1. Compile and install the internal candidate using the local helper; verify
   INSTALL VERIFIED, package, and both version fields.
2. Test video manual posting with automation OFF, automatic video posting,
   Shorts, X, Instagram, duplicate protection, strict proxy, audio formats,
   and ordinary group behavior. Verify fresh links and update preservation.
3. Inspect unsigned release workflow result and review the branch diff.
   Merge only after test approval; do not force-push main.
4. Identify the actual last distributed Uptodown APK or official listing
   package, versionCode and certificate SHA-256 from a trusted retained APK.
   Confirm the existing publisher key, never generate a replacement for an
   existing listing. The release certificate must not be the internal signer.
5. Run the protected release workflow manually on reviewed main. Verify
   non-debuggable public package, exact version, SHA-256, certificate match,
   and strictly higher versionCode against the actual published APK.
6. Test an in-place upgrade on a separate device with the production signing
   identity; no uninstall or data clearing. Upload only the verified signed
   production artifact, not the debug or unsigned APK.

Do not treat source checks as Android compilation or confirmed delivery.
Audio preview and cleanup limitations remain; no new extractor features
are introduced in this candidate.

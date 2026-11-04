# Ultgram beta signing and update plan

**Status: preparation only. No release key is generated, uploaded, or committed
as part of the Beta 0.3 feature work.**

## Choosing the permanent app identity

- Internal test package currently uses `com.yooshastan.ultmessenger.foss.beta`.
  Confirm whether it should be the permanent public beta app ID or remain
  an explicitly separate tester installation.
- An APK with the same package name and a different signer is **not** a normal
  in-place update. Moving from existing debug-signed builds to a release key
  cannot silently preserve installed app data.
- Existing Alpha 0.15 and Beta test APK signing/package combinations must be
  compared using actual retained APK files and the device's package manager.
  Do not guess or tell users to uninstall.
- A public release should have one consistent applicationId, monotonically
  increasing versionCode, and one durable key/upgrade lineage.

## Key generation and custody (separate future step)

- Generate an Android release keystore in a controlled local environment.
- Back it up encrypted in more than one independent secure location. Losing
  the key can permanently block ordinary updates.
- Store keystore contents and passwords only in restricted GitHub Actions
  secrets or a protected local release environment; do not commit or paste
  key files/passwords into issues, PRs or ordinary logs.
- Pin the signing toolchain, check `apksigner verify --print-certs` and
  `aapt dump badging` on every artifact and keep the signing certificate's
  public SHA-256 fingerprint in release metadata.

## CI release-gate design

1. Keep this Beta 0.3 ARM64 workflow debug-signed for internal device QA.
2. Create a separately protected release workflow that only runs on verified
   tags/manual authorized releases.
3. Fail closed if release secrets are missing; never fall back to debug
   signing on a release-tag build.
4. Compile a reproducible package variant, sign with the durable key,
   verify its signer/package/version and publish an attached checksum.
5. For any upgrade claim, use `scripts/apk_release_gate.py --apk <candidate>
   --previous <retained trusted APK>` and verify installed package/signing
   metadata on the target device.
6. Perform install-over-existing-app tests on Pixel 8a and Samsung A36.
   Verify notification permissions, existing chats/settings, Strict Proxy,
   translation, downloads and account logins survive.

## Distribution

- Publish GitHub prereleases only after actual signed APK verification and
  device checks. Then evaluate Uptodown/F-Droid build and signing policies
  separately; their signatures and update channels may differ.
- Do not describe a debug-signed development APK as a public beta release.

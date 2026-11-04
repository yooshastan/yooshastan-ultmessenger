# Ultgram beta signing and update plan

**Status: preparation only. No release key is generated, uploaded, or committed
as part of the Beta 0.3 feature work.**

## Latest tested candidate and signing caveat

- Verified combined beta: `16.0.0-beta03.1`, versionCode `7183013`,
  package `com.yooshastan.ultmessenger.foss.beta`.
- The build's recorded public certificate SHA-256 is
  `a08d7dc323ddf71ef3201944397e0d3cce7d40847263e11f328b68bbe19229ab`.
- This is an **internal build certificate**, not a permanent publisher identity.
  Mercurygram's pinned upstream source includes
  `TMessagesProj/config/release.keystore` and its debug Gradle configuration
  points at that file. Never reuse an upstream source-distributed keystore or
  its signing passwords as an Ultgram production signing identity.
- Recommended stable public package: `com.yooshastan.ultmessenger.foss`.
  It is deliberately distinct from the debug `.foss.beta` package and
  will not replace or migrate its app data automatically.
- Do **not** uninstall the working Pixel 8a or Samsung A36 build to test
  signing. A public-release identity must be verified on a separate device
  install or after explicit backups and an understood migration plan.

## Offline key generation (on your trusted Fedora workstation)

Run from your local clone after fetching the signing-prep feature branch:

```bash
bash scripts/create-ultgram-release-key.sh
```

The script uses an interactive JDK `keytool` prompt. By default it writes
`~/.local/share/ultgram-signing/ultgram-release.p12`, outside the repository,
with private permissions. It refuses to overwrite any existing keystore or
create one inside the repository. Do not paste keystore contents, passwords
or GitHub Actions secret values into a chat.

Back up the `.p12` file in encrypted offline storage. Keep an independent,
recoverable copy of the password in a password manager. The public
certificate fingerprint printed by keytool is safe to record in release
documentation; the private key and passwords are not.

## Choosing the permanent app identity

- Internal test package currently uses `com.yooshastan.ultmessenger.foss.beta`.
  Treat it as a separate **internal** tester installation; the recommended
  permanent publisher app ID is `com.yooshastan.ultmessenger.foss`.
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

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

## Protected CI signing (prepared, not enabled yet)

The release pipeline lives in `.github/workflows/android-foss-release-sign.yml`.
It is **manual-only**, accepts requests from **main** in this repository, and
builds a non-debuggable **unsigned** APK on a runner that has no signing-key
secrets. A separate job under the `ultgram-release` GitHub environment then
aligns and signs the unsigned APK with Android build-tools 36. The environment
job checks the permanent public package name, exact version and versionCode,
the expected certificate SHA-256 and that the application is not debuggable.
There is **no fallback** to the publicly bundled Mercurygram signing key.
The workflow uploads a signed **review artifact**, not a published GitHub
Release; you must still do real-device QA before public distribution.

### Configure the release environment (owner action)

In GitHub, open `yooshastan/yooshastan-ultmessenger` → **Settings** →
**Environments** → **New environment** named `ultgram-release`. Restrict
deployments to the `main` branch. Enable an approval rule if it is supported
on your repository/plan. Environment restrictions and reviewers may have
plan/public-repository constraints; the workflow also enforces a main-branch
condition independently.

Add these **environment secrets** (not PR comments or source files):

| Name | Value |
| --- | --- |
| `ULT_RELEASE_KEYSTORE_B64` | Base64-encoded bytes of the `.p12` signing file |
| `ULT_RELEASE_STORE_PASSWORD` | Its password |
| `ULT_RELEASE_KEY_PASSWORD` | Key password (PKCS12 usually same as store password) |

Add the PUBLIC certificate fingerprint under the exact name
`ULT_RELEASE_CERT_SHA256`, either as a GitHub **Environment variable**
(preferred) or as an **Environment secret** within `ultgram-release`.
A repository-level Variable or Secret with the same exact name is also
supported. The workflow checks Variables first, then Secrets, and
**fails closed** if the value is absent or doesn't match the signer.
Use **64 hex characters with no colons**. This public fingerprint
must **not** equal the inherited internal signer
`a08d7dc323ddf71ef3201944397e0d3cce7d40847263e11f328b68bbe19229ab`.

If you have GitHub CLI installed and authenticated locally, avoid exposing
key bytes or passwords in your terminal history. First create the environment
in GitHub's Settings, then run on your Fedora workstation:

```bash
repo=yooshastan/yooshastan-ultmessenger
env_name=ultgram-release
keystore="$HOME/.local/share/ultgram-signing/ultgram-release.p12"

base64 -w0 "$keystore" |
  gh secret set ULT_RELEASE_KEYSTORE_B64 --env "$env_name" -R "$repo"

read -r -s -p "Release keystore password: " secret; printf '\n'
printf '%s' "$secret" | gh secret set ULT_RELEASE_STORE_PASSWORD --env "$env_name" -R "$repo"
printf '%s' "$secret" | gh secret set ULT_RELEASE_KEY_PASSWORD --env "$env_name" -R "$repo"
unset secret

# Read your PUBLIC fingerprint locally; enter it in the environment variable UI.
keytool -list -v -storetype PKCS12 -keystore "$keystore" -alias ultgram-release |
  grep -i SHA256:
```

Do not send the base64 string, passwords, or keystore to ChatGPT. They are
private. The certificate fingerprint itself is public and safe to share.

### First signed candidate

After this signing-prep branch has been reviewed and merged onto `main`,
choose **Actions → Ultgram FOSS ARM64 protected release candidate → Run
workflow** on `main`. The unsigned build must succeed before the protected
signing job runs. If secrets or expected certificate fingerprint are missing,
the signing job fails without creating a signed artifact.

The candidate is planned as `16.0.0-beta03.2`, versionCode `7183014`,
package `com.yooshastan.ultmessenger.foss`.
This package is **different** from the current test app ID
`com.yooshastan.ultmessenger.foss.beta` and is not an in-place upgrade.
Keep your current test installation until the new signed app has been
independently verified on the Pixel 8a and Samsung A36.

**Alternative:** You can keep the private key entirely offline and sign a
verified unsigned APK on your Fedora workstation instead. Do not upload
environment secrets if you prefer that trust model.

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

#!/usr/bin/env bash
# Run on a trusted local workstation, NEVER on a public GitHub runner.
# This creates a NEW private key. It never uploads a key or sets CI secrets.
set -euo pipefail
umask 077

command -v keytool >/dev/null || {
  printf 'Missing JDK keytool. Install a local JDK first.\n' >&2
  exit 1
}
command -v realpath >/dev/null || {
  printf 'Missing realpath (coreutils).\n' >&2
  exit 1
}

repo="$(realpath "$(dirname "${BASH_SOURCE[0]}")/..")"
target="$(realpath -m "${1:-$HOME/.local/share/ultgram-signing/ultgram-release.p12}")"
case "$target" in
  "$repo"|"$repo"/*)
    printf 'Refusing to create a signing key inside the repository.\n' >&2
    exit 1
    ;;
esac
if [[ -e "$target" || -L "$target" ]]; then
  printf 'Refusing to replace an existing keystore: %s\n' "$target" >&2
  exit 1
fi

mkdir -p -- "$(dirname -- "$target")"
chmod 700 -- "$(dirname -- "$target")"

printf 'Ultgram permanent release key setup (interactive).\n'
printf 'This is a new private identity; it WILL NOT update an app signed by a different key.\n'
printf 'Location: %s\n' "$target"
printf 'Choose a strong password and do not paste it into chat, logs or GitHub.\n'
keytool -genkeypair \
  -keystore "$target" \
  -storetype PKCS12 \
  -alias ultgram-release \
  -keyalg RSA -keysize 4096 \
  -validity 10000 \
  -dname "CN=Yooshastan Studios"
chmod 600 -- "$target"

printf '\nKeep an offline encrypted backup of this file. Never commit it.\n'
printf 'Use the PUBLIC SHA-256 certificate fingerprint below to pin future releases:\n'
keytool -list -v -storetype PKCS12 -keystore "$target" -alias ultgram-release

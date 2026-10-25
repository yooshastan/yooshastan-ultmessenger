#!/usr/bin/env bash
# Prepare a pinned Android Studio source tree on Fedora/Linux.
# The downloaded ZIP is a bootstrap; this fetches the actual licensed Android source.
set -euo pipefail
project_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
variant="${1:-foss}"
case "$variant" in
  foss)
    source_dir="$project_root/.work/mercurygram-upstream"
    fetch_script="$project_root/scripts/fetch-foss.sh"
    configure_script="$project_root/scripts/configure-foss.py"
    ;;
  firebase)
    source_dir="$project_root/.work/telegram-upstream"
    fetch_script="$project_root/scripts/fetch-upstream.sh"
    configure_script="$project_root/scripts/configure-prototype.py"
    ;;
  *)
    printf 'Usage: bash scripts/prepare-android-studio.sh [foss|firebase]\n' >&2
    exit 2
    ;;
esac

for binary in git python3 bash; do
  command -v "$binary" >/dev/null || { printf 'Missing %s\n' "$binary" >&2; exit 2; }
done
if [[ -e "$source_dir" ]]; then
  printf 'Source already exists: %s\nRefusing overwrite.\n' "$source_dir" >&2
  exit 2
fi
if [[ -z "${ULT_TELEGRAM_API_ID:-}" ]]; then
  read -r -p 'Telegram API ID (from my.telegram.org): ' ULT_TELEGRAM_API_ID
  export ULT_TELEGRAM_API_ID
fi
if [[ -z "${ULT_TELEGRAM_API_HASH:-}" ]]; then
  read -r -s -p 'Telegram API hash (input hidden): ' ULT_TELEGRAM_API_HASH
  printf '\n'
  export ULT_TELEGRAM_API_HASH
fi
if [[ "$variant" == "firebase" ]]; then
  if [[ -z "${ULT_FIREBASE_JSON_PATH:-}" ]]; then
    read -r -p 'Path to YOUR Firebase google-services.json: ' ULT_FIREBASE_JSON_PATH
  fi
  [[ -f "$ULT_FIREBASE_JSON_PATH" ]] || { echo 'Firebase configuration file not found' >&2; exit 2; }
fi

printf 'Fetching pinned %s Android source; its native submodules are large.\n' "$variant"
bash "$fetch_script" "$source_dir"
python3 "$configure_script" "$source_dir"
python3 "$project_root/scripts/apply-branding.py" "$source_dir"
if [[ "$variant" == "foss" ]]; then
  python3 "$project_root/scripts/integrate-appearance.py" "$source_dir"
  python3 "$project_root/scripts/patch_locale_background.py" "$source_dir"
  python3 "$project_root/scripts/integrate-proxy.py" "$source_dir"
  python3 "$project_root/scripts/integrate-external-links.py" "$source_dir"
  python3 "$project_root/scripts/integrate-chat-layouts.py" "$source_dir"
  python3 "$project_root/scripts/integrate-chat-tools.py" "$source_dir"
  python3 "$project_root/scripts/integrate-message-tools.py" "$source_dir"
  python3 "$project_root/scripts/integrate-archive-visibility.py" "$source_dir"
  python3 "$project_root/scripts/integrate-translation.py" "$source_dir"
  python3 "$project_root/scripts/integrate-chat-views.py" "$source_dir"
else
  install -m 600 "$ULT_FIREBASE_JSON_PATH" "$source_dir/TMessagesProj/google-services.json"
fi
unset ULT_TELEGRAM_API_HASH ULT_TELEGRAM_API_ID

printf '\nAndroid Studio project prepared:\n%s\n' "$source_dir"
printf 'In Android Studio, File > Open > choose that folder (it contains settings.gradle).\n'
printf 'Allow Gradle to sync; install any SDK/NDK and CMake versions requested by the project.\n'
printf 'This is a development source checkout; API_KEYS is local/private. Do not commit the generated tree.\n'

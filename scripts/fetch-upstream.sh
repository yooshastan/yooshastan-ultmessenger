#!/usr/bin/env bash
set -euo pipefail

# Fetch official Telegram source and recursive submodules at the pinned version.
# Run from the root of yooshastan-ultmessenger.
repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
sha="$(tr -d '\n\r ' < "$repo_root/upstream/telegram.sha")"
target="${1:-$repo_root/.work/telegram-upstream}"

if [[ ! "$sha" =~ ^[0-9a-f]{40}$ ]]; then
  echo "Invalid pinned upstream SHA" >&2
  exit 2
fi

if [[ -e "$target" ]]; then
  echo "Target already exists; refusing to overwrite: $target" >&2
  exit 2
fi

mkdir -p "$(dirname "$target")"
git clone --filter=blob:none --recurse-submodules --shallow-submodules https://github.com/DrKLO/Telegram.git "$target"
git -C "$target" fetch --depth=1 origin "$sha"
git -C "$target" checkout --detach "$sha"
git -C "$target" submodule update --init --recursive --depth=1
actual="$(git -C "$target" rev-parse HEAD)"
[[ "$actual" == "$sha" ]] || { echo "Revision mismatch" >&2; exit 3; }
echo "Pinned Telegram source ready at $target ($actual)"

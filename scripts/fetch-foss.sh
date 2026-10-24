#!/usr/bin/env bash
set -euo pipefail
repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
sha="$(tr -d '\r\n ' < "$repo_root/upstream/mercurygram.sha")"
target="${1:-$repo_root/.work/mercurygram-upstream}"
[[ "$sha" =~ ^[0-9a-f]{40}$ ]] || { echo "Invalid pinned FOSS SHA" >&2; exit 2; }
[[ ! -e "$target" ]] || { echo "Target exists, refusing overwrite" >&2; exit 2; }
mkdir -p "$(dirname "$target")"
git clone --filter=blob:none --recurse-submodules --shallow-submodules --branch Mercurygram https://github.com/Mercurygram/Mercurygram.git "$target"
git -C "$target" fetch --depth=1 origin "$sha"
git -C "$target" checkout --detach "$sha"
git -C "$target" submodule update --init --recursive --depth=1
[[ "$(git -C "$target" rev-parse HEAD)" == "$sha" ]] || exit 3
echo "Pinned FOSS source checkout verified"

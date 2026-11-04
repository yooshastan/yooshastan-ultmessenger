#!/usr/bin/env bash
set -euo pipefail
repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
sha="$(tr -d '\r\n ' < "$repo_root/upstream/mercurygram.sha")"
target="${1:-$repo_root/.work/mercurygram-upstream}"
[[ "$sha" =~ ^[0-9a-f]{40}$ ]] || { echo "Invalid pinned FOSS SHA" >&2; exit 2; }
[[ ! -e "$target" ]] || { echo "Target exists, refusing overwrite" >&2; exit 2; }
mkdir -p "$(dirname "$target")"
# Checkout the pinned source BEFORE initializing its submodules. A shallow
# recursive clone of the moving Mercurygram tip can omit historical gitlinks
# used by the pinned commit (notably the Guardian Project Tor submodule).
git clone --filter=blob:none --no-recurse-submodules --branch Mercurygram https://github.com/Mercurygram/Mercurygram.git "$target"
git -C "$target" fetch --depth=1 origin "$sha"
git -C "$target" checkout --detach "$sha"
git -C "$target" submodule sync --recursive
# Fetch complete submodule history: some pinned native commits are not at a
# current branch tip and a shallow direct fetch returns "not our ref".
git -C "$target" submodule update --init --recursive
[[ "$(git -C "$target" rev-parse HEAD)" == "$sha" ]] || exit 3
echo "Pinned FOSS source checkout verified"

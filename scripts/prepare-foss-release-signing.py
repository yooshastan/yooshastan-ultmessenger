#!/usr/bin/env python3
"""Fail-closed release signing adaptation for the PINNED Mercurygram source.

Runs only against a disposable checkout. Does not copy or expose key material.
Release signing reads private values from the process environment at Gradle
configuration time. Does not touch the internal debug build script.
"""
import os
from pathlib import Path
import re
import sys

APP = Path("TMessagesProj_App/build.gradle")
BUNDLED_KEY = Path("TMessagesProj/config/release.keystore")
ENVIRONMENT = (
    "ULT_RELEASE_STORE_FILE",
    "ULT_RELEASE_STORE_PASSWORD",
    "ULT_RELEASE_KEY_PASSWORD",
    "ULT_RELEASE_KEY_ALIAS",
)
RELEASE_FIELDS = {
    "storeFile file(findProperty('RELEASE_STORE_FILE') ?: \"../TMessagesProj/config/release.keystore\")":
        "storeFile file(System.getenv('ULT_RELEASE_STORE_FILE'))",
    "storePassword RELEASE_STORE_PASSWORD":
        "storePassword System.getenv('ULT_RELEASE_STORE_PASSWORD')",
    "keyAlias RELEASE_KEY_ALIAS":
        "keyAlias System.getenv('ULT_RELEASE_KEY_ALIAS')",
    "keyPassword RELEASE_KEY_PASSWORD":
        "keyPassword System.getenv('ULT_RELEASE_KEY_PASSWORD')",
}
DEBUG_ONLY_VERSION_ANCHOR = 'def isBeta = variant.buildType.name == "debug" && project.hasProperty("MG_BETA_VERSION_CODE")'
RELEASE_ONLY_VERSION_ANCHOR = 'def isBeta = variant.buildType.name == "release" && project.hasProperty("MG_BETA_VERSION_CODE")'


def configure(root: Path, env=None) -> None:
    env = os.environ if env is None else env
    root = root.resolve(strict=True)
    missing = [name for name in ENVIRONMENT if not env.get(name)]
    if missing:
        raise ValueError("Release signing environment missing: " + ", ".join(missing))
    for name in ENVIRONMENT:
        if "\n" in env[name] or "\r" in env[name]:
            raise ValueError("Release signing environment contains a newline: " + name)
    if env["ULT_RELEASE_KEY_ALIAS"] != "ultgram-release":
        raise ValueError("Unexpected release key alias (expected ultgram-release)")
    key = Path(env["ULT_RELEASE_STORE_FILE"]).resolve(strict=True)
    if not key.is_file() or key.stat().st_size < 100:
        raise ValueError("Release key missing or empty")
    if key == root or root in key.parents:
        raise ValueError("Private release key must never be inside the source checkout")
    if key.name == BUNDLED_KEY.name:
        raise ValueError("Refusing a source-style release.keystore")
    gradle = root / APP
    if not gradle.is_file():
        raise ValueError("Pinned Mercurygram app Gradle file missing")
    old = gradle.read_text(encoding="utf-8")
    start_anchor = "    signingConfigs {\n"
    end_anchor = "        }\n    }\n\n    packagingOptions {"
    if old.count(start_anchor) != 1 or old.count(end_anchor) != 1:
        raise ValueError("Pinned signingConfigs structure changed")
    start = old.index("        release {\n", old.index(start_anchor))
    end = old.index(end_anchor, start)
    signing_release = old[start:end]
    for old_line, new_line in RELEASE_FIELDS.items():
        if signing_release.count(old_line) != 1:
            raise ValueError("Unexpected pinned upstream release signing field: " + old_line.split()[0])
        signing_release = signing_release.replace(old_line, new_line, 1)
    if any(token in signing_release for token in ("release.keystore", "RELEASE_STORE_PASSWORD", "RELEASE_KEY_PASSWORD", "RELEASE_KEY_ALIAS")):
        raise ValueError("Upstream release signing fallback remains")
    changed = old[:start] + signing_release + old[end:]
    if changed.count(DEBUG_ONLY_VERSION_ANCHOR) != 1:
        raise ValueError("Unexpected pinned version override")
    changed = changed.replace(DEBUG_ONLY_VERSION_ANCHOR, RELEASE_ONLY_VERSION_ANCHOR, 1)
    if 'signingConfig signingConfigs.release' not in changed:
        raise ValueError("Release build no longer selects release signingConfig")
    if 'applicationIdSuffix ".beta"' not in changed or 'applicationIdSuffix ".web"' not in changed:
        raise ValueError("Pinned package suffix layout changed")
    # The upstream key must not be on disk when Gradle runs, even if someone
    # accidentally reintroduces a source-default signing reference.
    bundled = root / BUNDLED_KEY
    if bundled.exists() or bundled.is_symlink():
        bundled.unlink()
    gradle.write_text(changed, encoding="utf-8")
    print("Private release signing configured in disposable source; bundled key removed.")


def main() -> int:
    if len(sys.argv) != 2:
        print("Usage: prepare-foss-release-signing.py SOURCE_DIRECTORY", file=sys.stderr)
        return 2
    try:
        configure(Path(sys.argv[1]))
    except (OSError, ValueError) as exc:
        # Do not print paths or environment values when a release fails closed.
        print("Release signing configuration rejected: " + str(exc), file=sys.stderr)
        return 1
    return 0


if __name__ == "__main__":
    raise SystemExit(main())

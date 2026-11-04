#!/usr/bin/env python3
"""Configure a pinned Mercurygram checkout to build an UNSIGNED FOSS release.

Never reads signing keys/passwords. The private key only enters a separate,
environment-protected signing job AFTER the Gradle runner has completed.
This modifies only a disposable source checkout, not the repository's source.
"""
from pathlib import Path
import sys

APP = Path("TMessagesProj_App/build.gradle")
UPSTREAM_KEY = Path("TMessagesProj/config/release.keystore")
OLD_VERSION = 'def isBeta = variant.buildType.name == "debug" && project.hasProperty("MG_BETA_VERSION_CODE")'
NEW_VERSION = 'def isBeta = variant.buildType.name == "release" && project.hasProperty("MG_BETA_VERSION_CODE")'


def configure(root: Path) -> None:
    root = root.resolve(strict=True)
    gradle = root / APP
    if not gradle.is_file():
        raise ValueError("Pinned upstream Gradle file missing")
    original = gradle.read_text(encoding="utf-8")
    if original.count(OLD_VERSION) != 1 or NEW_VERSION in original:
        raise ValueError("Pinned upstream version override changed")

    # Select the RELEASE build-type block, not the signingConfigs.release
    # or the standalone/web build type.
    build_anchor = "    buildTypes {\n"
    if original.count(build_anchor) != 1:
        raise ValueError("Pinned Gradle buildTypes structure changed")
    start = original.index("        release {\n", original.index(build_anchor))
    end = original.index("        }\n", start)
    release_block = original[start:end]
    old_signing = "signingConfig signingConfigs.release"
    if release_block.count(old_signing) != 1:
        raise ValueError("Release build must have exactly one signing config")
    if "debuggable false" not in release_block or "jniDebuggable false" not in release_block:
        raise ValueError("Release build must be non-debuggable")
    release_block = release_block.replace(old_signing, "signingConfig null", 1)
    output = original[:start] + release_block + original[end:]
    output = output.replace(OLD_VERSION, NEW_VERSION, 1)
    if output.count("signingConfig null") != 1:
        raise ValueError("Expected precisely one unsigned release build")
    if output.count(NEW_VERSION) != 1:
        raise ValueError("Failed to set release version override")
    gradle.write_text(output, encoding="utf-8")

    # The old publicly committed Mercurygram key cannot be used accidentally.
    key = root / UPSTREAM_KEY
    if key.exists() or key.is_symlink():
        key.unlink()
    print("Unsigned, non-debuggable FOSS release configured; inherited keystore removed.")


def main() -> int:
    if len(sys.argv) != 2:
        print("Usage: prepare-foss-unsigned-release.py SOURCE_DIRECTORY", file=sys.stderr)
        return 2
    try:
        configure(Path(sys.argv[1]))
    except (OSError, ValueError) as exc:
        print("Unsigned release preparation FAILED: " + str(exc), file=sys.stderr)
        return 1
    return 0


if __name__ == "__main__":
    raise SystemExit(main())

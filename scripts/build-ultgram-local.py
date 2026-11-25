#!/usr/bin/env python3
"""Build the same pinned Ultgram FOSS ARM64 test APK locally on Fedora/Linux.

Usage:
  python3 scripts/build-ultgram-local.py build     # first setup + incremental build
  python3 scripts/build-ultgram-local.py refresh   # explicitly reapply updated Ultgram source
  python3 scripts/build-ultgram-local.py install   # adb install -r (never uninstalls)
  python3 scripts/build-ultgram-local.py status

All generated files, including API_KEYS, stay below ignored .work/.
Never manually edit .work/mercurygram-upstream: 'refresh' resets its tracked
sources and removes its untracked files (but preserves ignored Gradle outputs).
"""
import argparse
import getpass
import hashlib
import os
from pathlib import Path
import re
import shutil
import subprocess
import sys

ROOT = Path(__file__).resolve().parents[1]
WORK = ROOT / ".work"
SOURCE = WORK / "mercurygram-upstream"
STAMP = WORK / "ultgram-local-source.sha"
OWNER = WORK / "ultgram-local-managed.marker"
ARTIFACTS = WORK / "ultgram-local-artifacts"
BUILD_TASK = ":TMessagesProj_App:assembleAfatFdArm64Debug"
INTEGRATORS = (
    "integrate-appearance.py",
    "patch_locale_background.py",
    "integrate-proxy.py",
    "integrate-external-links.py",
    "integrate-chat-layouts.py",
    "integrate-chat-tools.py",
    "integrate-message-tools.py",
    "integrate-archive-visibility.py",
    "integrate-translation.py",
    "integrate-chat-views.py",
    "integrate-story-save.py",
    "integrate-ult-settings.py",
    "integrate-link-sanitizer.py",
    "integrate-cache-limit.py",
    "integrate-native-strict.py",
    "integrate-video-links.py",
    "integrate-screen-privacy.py",
)
SDK_PACKAGES = (
    "platforms;android-36",
    "build-tools;36.0.0",
    "ndk;27.2.12479018",
    "cmake;3.22.1",
)


def die(message):
    raise SystemExit("ERROR: " + message)


def run(*args, cwd=ROOT, env=None):
    print("+", " ".join(str(x) for x in args), flush=True)
    subprocess.run([str(x) for x in args], cwd=cwd, env=env, check=True)


def output(*args, cwd=ROOT):
    return subprocess.check_output([str(x) for x in args], cwd=cwd, text=True).strip()


def current_revision():
    return output("git", "rev-parse", "HEAD")


def pinned_revision():
    pinned = (ROOT / "upstream/mercurygram.sha").read_text().strip()
    if not re.fullmatch(r"[0-9a-f]{40}", pinned):
        die("Invalid pinned Mercurygram revision")
    return pinned


def workflow_version():
    workflow = (ROOT / ".github/workflows/android-beta.yml").read_text()
    needle = (r"assembleAfatFdArm64Debug\s+-PMG_BUILD_TAG=([0-9.]+)\s+"
              r"-PMG_BETA_VERSION_CODE=([0-9]+)\s+"
              r"-PMG_BETA_VERSION_NAME=([A-Za-z0-9._-]+)")
    matches = re.findall(needle, workflow)
    if len(matches) != 1:
        die("Cannot identify a single matching local APK version from android-beta.yml")
    return matches[0]


def prerequisites():
    required = ("git", "git-lfs", "python3", "ninja", "meson",
                "nasm", "gperf", "pkg-config", "make", "cmake", "curl")
    missing = [name for name in required if shutil.which(name) is None]
    if missing:
        die("Missing build tools: " + ", ".join(missing) +
            "\nFedora: sudo dnf install git python3 ninja-build meson nasm "
            "gperf pkgconf-pkg-config make cmake curl unzip git-lfs android-tools")
    # Fedora 44's standard repositories no longer provide JDK 17. The
    # Fedora-packaged Adoptium repository makes temurin-17-jdk available.
    homes = []
    if os.environ.get("JAVA_HOME"):
        homes.append(Path(os.environ["JAVA_HOME"]))
    homes.extend(Path("/usr/lib/jvm").glob("*17*"))
    for candidate in homes:
        if (candidate / "bin/javac").is_file():
            version = subprocess.run([str(candidate / "bin/javac"), "-version"],
                                     capture_output=True, text=True, check=True).stdout.strip()
            if version.startswith("javac 17"):
                os.environ["JAVA_HOME"] = str(candidate)
                os.environ["PATH"] = str(candidate / "bin") + os.pathsep + os.environ["PATH"]
                break
    else:
        if shutil.which("javac"):
            version = output("javac", "-version")
            if version.startswith("javac 17"):
                return
        die("JDK 17 is required. On Fedora 44 install:\n"
            "  sudo dnf install adoptium-temurin-java-repository\n"
            "  sudo dnf install temurin-17-jdk\n"
            "Then rerun. JDK 25/27 cannot substitute for the pinned CI JDK 17.")



def sdk_setup():
    home = Path(os.environ.get("ANDROID_HOME")
                or os.environ.get("ANDROID_SDK_ROOT")
                or (Path.home() / "Android/Sdk")).expanduser()
    os.environ["ANDROID_HOME"] = str(home)
    os.environ["ANDROID_SDK_ROOT"] = str(home)
    sdkmanager = shutil.which("sdkmanager")
    if sdkmanager is None:
        for candidate in (home / "cmdline-tools/latest/bin/sdkmanager",
                          home / "cmdline-tools/bin/sdkmanager"):
            if candidate.is_file():
                sdkmanager = str(candidate)
                break
    if sdkmanager is None:
        die("Android SDK Command-line Tools (sdkmanager) not found.\n"
            "Install Android Studio SDK Command-line Tools, then set ANDROID_HOME "
            "to the SDK directory (usually ~/Android/Sdk).")
    required_paths = (home / "platforms/android-36",
                      home / "build-tools/36.0.0",
                      home / "ndk/27.2.12479018",
                      home / "cmake/3.22.1")
    if not all(p.exists() for p in required_paths):
        run(sdkmanager, "--sdk_root=" + str(home), "--install", *SDK_PACKAGES)


def credentials(previous=None):
    if previous:
        payload = previous
        app_id = re.search(r"(?m)^APP_ID\s*=\s*([0-9]+)\s*$", payload)
        app_hash = re.search(r"(?m)^APP_HASH\s*=\s*([0-9a-fA-F]{32})\s*$", payload)
        if app_id and app_hash:
            return app_id.group(1), app_hash.group(1)
        die("Saved local API_KEYS is malformed. Will not delete or replace it.")
    app_id = os.environ.get("ULT_TELEGRAM_API_ID") or input(
        "Telegram API ID (from your own my.telegram.org account): ").strip()
    app_hash = os.environ.get("ULT_TELEGRAM_API_HASH") or getpass.getpass(
        "Telegram API hash (hidden; never sent to GitHub): ").strip()
    if not app_id.isdecimal() or not re.fullmatch(r"[0-9a-fA-F]{32}", app_hash):
        die("API ID/hash invalid. Your existing GitHub Actions secrets are not readable locally.")
    return app_id, app_hash


def apply_integrations(api_id, api_hash):
    env = dict(os.environ)
    env["ULT_TELEGRAM_API_ID"] = api_id
    env["ULT_TELEGRAM_API_HASH"] = api_hash
    # The configure script writes API_KEYS inside ignored .work with permissions 0600.
    run("python3", ROOT / "scripts/configure-foss.py", SOURCE, env=env)
    del env["ULT_TELEGRAM_API_HASH"]
    del env["ULT_TELEGRAM_API_ID"]
    run("python3", ROOT / "scripts/apply-branding.py", SOURCE)
    for script in INTEGRATORS:
        run("python3", ROOT / "scripts" / script, SOURCE)
    verify_submodule_checkouts()
    STAMP.write_text(current_revision() + "\n")
    print("Pinned source prepared for Ultgram commit", current_revision()[:12])


def verify_submodule_checkouts():
    """Verify pinned trees on disk, even when Git status misses absent files."""
    checked = restored = 0

    def scan(repo, pin, is_root=False):
        nonlocal checked, restored
        if not is_root:
            if not (repo / ".git").exists():
                die("Uninitialized submodule: " + str(repo))
            if output("git", "rev-parse", "HEAD", cwd=repo) != pin:
                die("Submodule revision mismatch: " + str(repo))
            checked += 1
        tree = subprocess.check_output(
            ["git", "ls-tree", "-r", "-z", pin], cwd=repo)
        missing = []
        children = []
        for entry in tree.split(b"\0"):
            if not entry:
                continue
            metadata, path = entry.split(b"\t", 1)
            mode, kind, oid = metadata.split()
            target = repo / os.fsdecode(path)
            if kind == b"commit":
                children.append((target, oid.decode("ascii")))
            elif kind == b"blob" and not is_root and not os.path.lexists(target):
                missing.append(path)
        if missing:
            print(f"Restoring {len(missing)} missing tracked files in "
                  f"{repo.relative_to(SOURCE)}", flush=True)
            subprocess.run(
                ["git", "--literal-pathspecs", "restore", "--source=" + pin,
                 "--worktree", "--ignore-skip-worktree-bits",
                 "--pathspec-from-file=-", "--pathspec-file-nul"],
                cwd=repo, input=b"\0".join(missing) + b"\0", check=True)
            if any(not os.path.lexists(repo / os.fsdecode(p)) for p in missing):
                die("Submodule still incomplete: " + str(repo))
            restored += len(missing)
        for child, child_pin in children:
            scan(child, child_pin)

    scan(SOURCE, pinned_revision(), True)
    latex = SOURCE / "TMessagesProj/lib/jlatexmath"
    wallet = SOURCE / "TMessagesProj/jni/wallet-engine"
    for required in (latex / "jlatexmath/build.gradle", wallet / "Cargo.toml",
                     wallet / "src/lib.rs", wallet / "vendor/ton/Cargo.toml"):
        if not required.is_file():
            die("Required dependency missing: " + str(required))
    for name in ("JLatexMathAndroid.java", "JLatexMathDrawable.java"):
        if not any(p.is_file() for p in latex.rglob(name)):
            die("Required LaTeX Java source missing: " + name)
    print(f"Submodule preflight: {checked} checked, {restored} files restored")


def run_gradle_logged(*args):
    """Keep the latest compiler output and propagate Gradle's actual exit code."""
    log_path = WORK / "ultgram-build-latest.log"
    command = [str(x) for x in args]
    print("Gradle log:", log_path, flush=True)
    with log_path.open("w", encoding="utf-8") as log:
        with subprocess.Popen(command, cwd=SOURCE, stdout=subprocess.PIPE,
                              stderr=subprocess.STDOUT, text=True,
                              encoding="utf-8", errors="replace") as process:
            for line in process.stdout:
                log.write(line)
                log.flush()
                print(line, end="", flush=True)
            code = process.wait()
        if code:
            die(f"Gradle exited with {code}; see {log_path}")


def prepare(refresh=False):
    WORK.mkdir(parents=True, exist_ok=True)
    if SOURCE.exists():
        if not OWNER.exists():
            die("Existing Mercurygram source is not marked as script-owned. "
                "Do not overwrite it; move it aside manually if it is disposable.")
        if output("git", "rev-parse", "HEAD", cwd=SOURCE) != pinned_revision():
            die("Local upstream checkout is not the pinned Mercurygram commit.")
        previous = STAMP.read_text().strip() if STAMP.exists() else None
        if previous is None and not refresh:
            # The first setup may have stopped inside an LFS-backed
            # submodule checkout. Resume the pinned, script-owned clone
            # in place; never remove downloaded sources or user files.
            if (SOURCE / "API_KEYS").exists():
                die("An incomplete local setup already contains API_KEYS. "
                    "Refusing to reset or repeat private configuration. "
                    "Inspect the script-owned source before proceeding.")
            print("Resuming interrupted pinned Mercurygram submodule checkout.")
            run("git", "-C", SOURCE, "submodule", "sync", "--recursive")
            run("git", "-C", SOURCE, "submodule", "update", "--init", "--recursive")
            api_id, api_hash = credentials()
            apply_integrations(api_id, api_hash)
            return
        if not refresh:
            if previous != current_revision():
                die("Ultgram source has changed since local preparation.\n"
                    "Run: python3 scripts/build-ultgram-local.py refresh\n"
                    "Note: refresh resets ONLY the script-owned .work/mercurygram-upstream.")
            verify_submodule_checkouts()
            return
        print("REFRESH: resetting script-owned disposable Mercurygram source.")
        print("WARNING: tracked edits and untracked files IN THAT SOURCE TREE are discarded.")
        existing_keys = SOURCE / "API_KEYS"
        if not existing_keys.is_file():
            die("Cannot refresh: local API_KEYS missing (no source modified).")
        api_id, api_hash = credentials(existing_keys.read_text())
        run("git", "reset", "--hard", pinned_revision(), cwd=SOURCE)
        run("git", "clean", "-fd", cwd=SOURCE)  # Preserves ignored build/ and .gradle/
        existing_keys.unlink(missing_ok=True)
        apply_integrations(api_id, api_hash)
        return
    if refresh:
        die("No local source to refresh; run the build command instead.")
    OWNER.write_text("Managed by scripts/build-ultgram-local.py. Disposable source tree.\n")
    print("First local setup: fetching pinned Mercurygram and submodules.")
    run("bash", ROOT / "scripts/fetch-foss.sh")
    api_id, api_hash = credentials()
    apply_integrations(api_id, api_hash)


def build():
    prerequisites()
    sdk_setup()
    prepare()
    run("python3", "-m", "unittest", "discover", "-s", "video/tests", "-p",
        "test_integration.py")
    run("python3", "-m", "unittest", "discover", "-s", "settings/tests", "-p",
        "test_link_sanitizer_integration.py")
    test_classes = WORK / "video-test-classes"
    test_classes.mkdir(parents=True, exist_ok=True)
    video_src = ROOT / "video/src/com/yooshastan/ultmessenger/video"
    for model in ("UltVideoLinkRules", "UltVideoFailure", "UltExtractorPolicy", "UltMediaOptions"):
        run("javac", "-d", test_classes,
            ROOT / "settings/src/com/yooshastan/ultmessenger/settings/UltLinkSanitizerPolicy.java",
            video_src / (model + ".java"),
            ROOT / "video/tests" / (model + "Test.java"))
        run("java", "-cp", test_classes, model + "Test")
    run("javac", "-d", test_classes,
        ROOT / "settings/src/com/yooshastan/ultmessenger/settings/UltLinkSanitizerPolicy.java",
        ROOT / "settings/tests/UltLinkSanitizerPolicyTest.java")
    run("java", "-cp", test_classes, "UltLinkSanitizerPolicyTest")
    tag, code, name = workflow_version()
    print("Compiling", name, "versionCode", code, "on", current_revision()[:12])
    run_gradle_logged(SOURCE / "gradlew", BUILD_TASK, "-PMG_BUILD_TAG=" + tag,
        "-PMG_BETA_VERSION_CODE=" + code,
        "-PMG_BETA_VERSION_NAME=" + name, "--daemon", "--build-cache", "--console=plain")
    outputs = list((SOURCE / "TMessagesProj_App/build/outputs/apk").rglob("*.apk"))
    outputs = [p for p in outputs if "afatFdArm64" in p.as_posix()
               and "debug" in p.as_posix().lower()]
    if len(outputs) != 1:
        die("Expected one ARM64 debug APK, found " + str(len(outputs)) +
            ". Inspect " + str(SOURCE / "TMessagesProj_App/build/outputs/apk"))
    ARTIFACTS.mkdir(parents=True, exist_ok=True)
    artifact = ARTIFACTS / ("ultgram-" + name + "-local-debug.apk")
    shutil.copy2(outputs[0], artifact)
    digest = hashlib.sha256(artifact.read_bytes()).hexdigest()
    (ARTIFACTS / "SHA256SUMS").write_text(digest + "  " + artifact.name + "\n")
    run("python3", ROOT / "scripts/apk_release_gate.py", "--apk", artifact,
        "--report", ARTIFACTS / "SIGNING_INFO.txt")
    print("\nAPK READY:", artifact)
    print("SHA-256:", digest)
    print("This is a DEBUG-signed internal test APK, not a public release.")
    print("To install (ONLY when package/signing identity is compatible):")
    print("  python3 scripts/build-ultgram-local.py install")


def install():
    if not ARTIFACTS.exists():
        die("No local artifact; build first.")
    apks = sorted(ARTIFACTS.glob("ultgram-*-local-debug.apk"),
                  key=lambda path: path.stat().st_mtime, reverse=True)
    if not apks:
        die("No local test APK; build first.")
    if not shutil.which("adb"):
        die("Install android-tools on Fedora for adb.")
    print("Android will refuse incompatible signatures. Never uninstall to force an update.")
    run("adb", "devices", "-l")
    run("python3", ROOT / "scripts/apk_release_gate.py", "--apk", apks[0])
    run("adb", "install", "-r", apks[0])


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("command", nargs="?", choices=("build", "refresh", "install", "status"),
                        default="build")
    args = parser.parse_args()
    os.umask(0o077)
    if args.command == "status":
        print("Repo:", current_revision())
        print("Prepared:", STAMP.read_text().strip() if STAMP.exists() else "no")
        print("Source:", SOURCE if SOURCE.exists() else "(not fetched)")
        print("Artifacts:", ARTIFACTS)
    elif args.command == "refresh":
        prepare(refresh=True)
        print("Source refreshed; run: python3 scripts/build-ultgram-local.py build")
    elif args.command == "install":
        install()
    else:
        build()


if __name__ == "__main__":
    main()

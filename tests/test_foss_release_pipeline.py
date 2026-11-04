"""Offline-only release pipeline contracts; never generates or reads a real key."""
import importlib.util
from pathlib import Path
import sys
from tempfile import TemporaryDirectory
import unittest
from unittest import mock

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / "scripts"))


def load(name, path):
    spec = importlib.util.spec_from_file_location(name, ROOT / "scripts" / path)
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


unsigned = load("unsigned_release", "prepare-foss-unsigned-release.py")
verify = load("verify_release", "verify-foss-release-apk.py")


def fixture(root: Path) -> Path:
    gradle = root / unsigned.APP
    gradle.parent.mkdir(parents=True)
    gradle.write_text("""android {
    signingConfigs {
        debug {
            storeFile file(findProperty('RELEASE_STORE_FILE') ?: "../TMessagesProj/config/release.keystore")
        }
        release {
            storeFile file(findProperty('RELEASE_STORE_FILE') ?: "../TMessagesProj/config/release.keystore")
        }
    }

    packagingOptions {
    }

    buildTypes {
        debug {
            applicationIdSuffix ".beta"
        }
        standalone {
            signingConfig signingConfigs.release
            applicationIdSuffix ".web"
        }
        release {
            debuggable false
            jniDebuggable false
            signingConfig signingConfigs.release
            minifyEnabled true
        }
    }
    def isBeta = variant.buildType.name == "debug" && project.hasProperty("MG_BETA_VERSION_CODE")
}
""", encoding="utf-8")
    key = root / unsigned.UPSTREAM_KEY
    key.parent.mkdir(parents=True)
    key.write_bytes(b"UPSTREAM PUBLIC KEY NOT FOR PRODUCTION")
    return gradle


class UnsignedBuild(unittest.TestCase):
    def test_release_is_unsigned_and_private_key_never_present(self):
        with TemporaryDirectory() as d:
            root = Path(d)
            gradle = fixture(root)
            before = gradle.read_text()
            unsigned.configure(root)
            after = gradle.read_text()
            self.assertNotEqual(before, after)
            self.assertIn('def isBeta = variant.buildType.name == "release"', after)
            block = after.split("    buildTypes {", 1)[1].split("        release {", 1)[1]
            self.assertIn("signingConfig null", block)
            self.assertIn("debuggable false", block)
            self.assertNotIn("applicationIdSuffix \".beta\"", block)
            self.assertFalse((root / unsigned.UPSTREAM_KEY).exists())
            self.assertNotIn("ULT_RELEASE_KEYSTORE_B64", after)
            with self.assertRaises(ValueError):
                unsigned.configure(root)

    def test_mismatched_pinned_gradle_fails_without_mutation(self):
        with TemporaryDirectory() as d:
            root = Path(d)
            gradle = fixture(root)
            gradle.write_text(gradle.read_text().replace(
                "            signingConfig signingConfigs.release\n            minifyEnabled true",
                "            minifyEnabled true"
            ))
            previous = gradle.read_bytes()
            with self.assertRaises(ValueError):
                unsigned.configure(root)
            self.assertEqual(gradle.read_bytes(), previous)
            self.assertTrue((root / unsigned.UPSTREAM_KEY).exists())


class PrivateSignerGate(unittest.TestCase):
    def test_accepts_matching_private_non_debuggable_identity_only(self):
        cert = "2" * 64
        with mock.patch.object(verify, "inspect",
                               return_value=(verify.PUBLIC_PACKAGE, 7183014,
                                             "16.0.0-beta03.2", cert)), \
             mock.patch.object(verify, "find_android_tool", return_value="aapt"), \
             mock.patch.object(verify.subprocess, "run") as proc:
            proc.return_value.stdout = "package: name='com.yooshastan.ultmessenger.foss'\n"
            result = verify.validate(Path("dummy.apk"), 7183014, "16.0.0-beta03.2", cert)
            self.assertIn("RELEASE GATE: PASS", result)
            proc.return_value.stdout += "application-debuggable\n"
            with self.assertRaisesRegex(ValueError, "debuggable"):
                verify.validate(Path("dummy.apk"), 7183014, "16.0.0-beta03.2", cert)

    def test_refuses_upstream_cert_and_wrong_package(self):
        with self.assertRaises(ValueError):
            verify.validate(Path("dummy.apk"), 7183014, "16.0.0-beta03.2",
                            verify.INTERNAL_SIGNER)
        with mock.patch.object(verify, "inspect",
                               return_value=("com.yooshastan.ultmessenger.foss.beta",
                                             7183014, "16.0.0-beta03.2", "2"*64)):
            with self.assertRaisesRegex(ValueError, "permanent public"):
                verify.validate(Path("dummy.apk"), 7183014, "16.0.0-beta03.2", "2"*64)


class WorkflowContract(unittest.TestCase):
    def test_release_workflow_never_builds_with_private_key(self):
        workflow = (ROOT / ".github/workflows/android-foss-release-sign.yml").read_text()
        self.assertIn("workflow_dispatch:", workflow)
        self.assertNotIn("pull_request:", workflow)
        self.assertIn('test ! -e "$source_dir/TMessagesProj/config/release.keystore"', workflow)
        self.assertIn("refs/heads/main", workflow)
        self.assertIn("environment: ultgram-release", workflow)
        build = workflow.split("  build-unsigned:", 1)[1].split("  sign-private:", 1)[0]
        sign = workflow.split("  sign-private:", 1)[1]
        self.assertNotIn("ULT_RELEASE_KEYSTORE_B64", build)
        self.assertNotIn("ULT_RELEASE_STORE_PASSWORD", build)
        self.assertIn("prepare-foss-unsigned-release.py", build)
        self.assertIn("assembleAfatFdArm64Release", build)
        self.assertIn("Unsigned build unexpectedly", build)
        self.assertIn("secrets.ULT_RELEASE_KEYSTORE_B64", sign)
        self.assertIn("base64 --decode", sign)
        self.assertIn("apksigner", sign)
        self.assertIn("--ks-pass env:ULT_RELEASE_STORE_PASSWORD", sign)
        self.assertIn("verify-foss-release-apk.py", sign)
        self.assertIn("vars.ULT_RELEASE_CERT_SHA256 || secrets.ULT_RELEASE_CERT_SHA256", sign)
        self.assertIn('if [[ -z "$ULT_RELEASE_CERT_SHA256" ]]', sign)
        self.assertIn("do not disable verification", sign)
        self.assertIn('--cert-sha256 "$ULT_RELEASE_CERT_SHA256"', sign)
        self.assertNotIn("gh release create", sign)
        self.assertNotIn("generate-key", sign)


if __name__ == "__main__":
    unittest.main()

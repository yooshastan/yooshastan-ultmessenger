"""Offline tests for exact-version installation; no real APK or phone."""
import importlib.util
import json
from pathlib import Path
import sys
from tempfile import TemporaryDirectory
import unittest
from unittest import mock

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / "scripts"))
spec = importlib.util.spec_from_file_location("local_builder", ROOT / "scripts/build-ultgram-local.py")
builder = importlib.util.module_from_spec(spec)
spec.loader.exec_module(builder)

class LocalInstall(unittest.TestCase):
    def setUp(self):
        self.tmp = TemporaryDirectory()
        self.addCleanup(self.tmp.cleanup)
        self.path = Path(self.tmp.name)
        self.artifact = self.path / "ultgram-16.0.0-beta03.4.15-local-debug.apk"
        self.artifact.write_bytes(b"candidate")
        (self.path / "ultgram-16.0.0-beta03.4.12-local-debug.apk").write_bytes(b"newer timestamp")
        self.code, self.name = "7183031", "16.0.0-beta03.4.15"
        self.record = {"commit": "abc", "version_code": int(self.code),
                       "version_name": self.name, "package": builder.BETA_PACKAGE,
                       "sha256": builder.sha256_file(self.artifact)}
        builder.receipt_path(self.artifact).write_text(json.dumps(self.record))
        for key, val in (("ARTIFACTS", self.path), ("current_revision", lambda: "abc"),
                         ("workflow_version", lambda: ("16.0.0.0.10", self.code, self.name))):
            patch = mock.patch.object(builder, key, val)
            patch.start()
            self.addCleanup(patch.stop)
        patch = mock.patch.object(builder.apk_gate, "inspect",
                                  return_value=(builder.BETA_PACKAGE, int(self.code), self.name, "2"*64))
        self.inspect = patch.start()
        self.addCleanup(patch.stop)

    def test_ignores_newer_timestamp_on_old_apk(self):
        self.assertEqual(builder.verified_artifact(self.code, self.name), self.artifact)
        self.inspect.assert_called_once_with(self.artifact)

    def test_missing_receipt_or_apk_refuses_fallback(self):
        builder.receipt_path(self.artifact).unlink()
        with self.assertRaises(SystemExit):
            builder.verified_artifact(self.code, self.name)

    def test_hash_or_identity_mismatch_refused(self):
        self.inspect.return_value = (builder.BETA_PACKAGE, 7183028, "old", "2"*64)
        with self.assertRaises(SystemExit):
            builder.verified_artifact(self.code, self.name)
        self.artifact.write_bytes(b"changed")
        with self.assertRaises(SystemExit):
            builder.verified_artifact(self.code, self.name)

    def test_install_checks_post_version(self):
        with mock.patch.object(builder.shutil, "which", return_value="/usr/bin/adb"), \
             mock.patch.object(builder, "output", return_value="device"), \
             mock.patch.object(builder, "installed_identity", side_effect=[(7183028, "old"), (int(self.code), self.name)]), \
             mock.patch.object(builder, "run") as run:
            builder.install()
            run.assert_called_once_with("adb", "install", "-r", self.artifact)

    def test_post_install_mismatch_fails(self):
        with mock.patch.object(builder.shutil, "which", return_value="/usr/bin/adb"), \
             mock.patch.object(builder, "output", return_value="device"), \
             mock.patch.object(builder, "installed_identity", return_value=(7183028, "old")), \
             mock.patch.object(builder, "run"):
            with self.assertRaises(SystemExit):
                builder.install()

if __name__ == "__main__":
    unittest.main()

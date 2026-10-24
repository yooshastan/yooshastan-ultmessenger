import os
from pathlib import Path
import subprocess
import sys
import tempfile
import unittest


class ConfigurePrototypeTest(unittest.TestCase):
    def test_updates_identity_without_logging_hash(self):
        root = Path(__file__).resolve().parents[1]
        with tempfile.TemporaryDirectory() as tmp:
            source = Path(tmp)
            (source / "gradle.properties").write_text("APP_PACKAGE=org.telegram.messenger\n")
            vars_file = source / "TMessagesProj/src/main/java/org/telegram/messenger/BuildVars.java"
            vars_file.parent.mkdir(parents=True)
            vars_file.write_text(
                'public static int APP_ID = 4;\n'
                'public static String APP_HASH = "014b35b6184100b085b0d0572f9b5103";\n'
                'public static boolean SUPPORTS_PASSKEYS = true;\n'
                'public static boolean CHECK_UPDATES = true;\n'
            )
            manifest = source / "TMessagesProj/config/debug/AndroidManifest.xml"
            manifest.parent.mkdir(parents=True)
            manifest.write_text(
                '<manifest><application android:label="@string/AppNameBeta" '
                'android:icon="@mipmap/ic_launcher"/></manifest>'
            )
            env = dict(os.environ,
                       ULT_TELEGRAM_API_ID="12345",
                       ULT_TELEGRAM_API_HASH="a" * 32)
            command = [sys.executable, str(root / "scripts/configure-prototype.py"), str(source)]
            proc = subprocess.run(command, env=env, text=True, capture_output=True)
            self.assertEqual(proc.returncode, 0, proc.stderr)
            self.assertNotIn("a" * 32, proc.stdout)
            self.assertIn("APP_PACKAGE=com.yooshastan.ultmessenger",
                          (source / "gradle.properties").read_text())
            self.assertIn('APP_ID = 12345', vars_file.read_text())
            self.assertIn('android:label="Yooshastan ULT Messenger"', manifest.read_text())


if __name__ == "__main__":
    unittest.main()

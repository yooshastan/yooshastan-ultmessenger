import os
from pathlib import Path
import subprocess
import sys
import tempfile
import unittest


class DualVariantTests(unittest.TestCase):
    def test_foss_configuration_and_vector_branding(self):
        checkout_root = Path(__file__).resolve().parents[1]
        with tempfile.TemporaryDirectory() as td:
            root = Path(td)
            props = root / "gradle.properties"
            props.write_text("APP_PACKAGE=it.belloworld.mercurygram\n")
            gradle = root / "TMessagesProj/build.gradle"
            gradle.parent.mkdir(parents=True)
            gradle.write_text("apply plugin: 'com.android.library'\n")
            manifest = root / "TMessagesProj/config/debug/AndroidManifest.xml"
            manifest.parent.mkdir(parents=True)
            manifest.write_text(
                '<manifest><application android:label="@string/AppNameBeta" '
                'android:icon="@mipmap/ic_launcher" '
                'android:roundIcon="@mipmap/ic_launcher_round"/></manifest>'
            )
            env = dict(os.environ, ULT_TELEGRAM_API_ID="12345",
                       ULT_TELEGRAM_API_HASH="a" * 32)
            f = subprocess.run([sys.executable, str(checkout_root / "scripts/configure-foss.py"), str(root)],
                               text=True, capture_output=True, env=env)
            self.assertEqual(f.returncode, 0, f.stderr)
            self.assertNotIn("a" * 32, f.stdout)
            self.assertIn("com.yooshastan.ultmessenger.foss", props.read_text())
            self.assertIn("APP_ID = 12345", (root / "API_KEYS").read_text())
            self.assertIn("ULT Messenger FOSS", manifest.read_text())
            b = subprocess.run([sys.executable, str(checkout_root / "scripts/apply-branding.py"), str(root)],
                               text=True, capture_output=True)
            self.assertEqual(b.returncode, 0, b.stderr)
            self.assertIn("@mipmap/ult_launcher", manifest.read_text())
            self.assertTrue((root / "TMessagesProj/src/main/res/mipmap-anydpi/ult_launcher.xml").is_file())
            self.assertTrue((root / "TMessagesProj/src/main/res/mipmap-anydpi-v26/ult_launcher.xml").is_file())


if __name__ == "__main__":
    unittest.main()

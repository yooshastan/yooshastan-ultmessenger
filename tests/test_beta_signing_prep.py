"""Guardrails for offline-only signing preparation. No keys or passwords used."""
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[1]

class SigningPrepContracts(unittest.TestCase):
    def test_key_script_is_local_and_safe(self):
        script = (ROOT / "scripts/create-ultgram-release-key.sh").read_text()
        self.assertIn("umask 077", script)
        self.assertIn("if [[ -e \"$target\" || -L \"$target\" ]]", script)
        self.assertIn("Refusing to create a signing key inside the repository", script)
        self.assertIn("-storetype PKCS12", script)
        self.assertIn("-keysize 4096", script)
        self.assertIn("chmod 600 -- \"$target\"", script)
        self.assertIn("keytool -list -v", script)
        self.assertNotIn("gh secret set", script)
        self.assertNotIn("git add", script)
        self.assertNotIn("curl ", script)
        self.assertNotIn("chmod 700 -- \"$(dirname -- \"$target\")\"", script)

    def test_identity_is_separate_and_public_keystore_not_reused(self):
        notes = (ROOT / "docs/BETA_RELEASE_SIGNING_PLAN.md").read_text()
        self.assertIn("com.yooshastan.ultmessenger.foss.beta", notes)
        self.assertIn("com.yooshastan.ultmessenger.foss", notes)
        self.assertIn("a08d7dc323ddf71ef3201944397e0d3cce7d40847263e11f328b68bbe19229ab", notes)
        self.assertIn("Never reuse an upstream source-distributed keystore", notes)
        self.assertIn("private key and passwords are not", notes)

    def test_existing_debug_apk_workflow_unchanged(self):
        workflow = (ROOT / ".github/workflows/android-beta.yml").read_text()
        self.assertIn("16.0.0-beta03.1", workflow)
        self.assertIn("7183013", workflow)
        self.assertIn("assembleAfatFdArm64Debug", workflow)
        self.assertIn("integrate-native-strict.py", workflow)
        self.assertIn("integrate-translation.py", workflow)
        self.assertNotIn("create-ultgram-release-key.sh", workflow)

if __name__ == "__main__":
    unittest.main()

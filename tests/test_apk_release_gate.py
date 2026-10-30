"""Signature and package identity checks; uses synthetic tool outputs only."""
from pathlib import Path
import importlib.util
from unittest import TestCase
MODULE = Path(__file__).resolve().parents[1] / "scripts/apk_release_gate.py"
spec = importlib.util.spec_from_file_location("apk_release_gate", MODULE)
gate = importlib.util.module_from_spec(spec)
spec.loader.exec_module(gate)
ONE = "ab" * 32
TWO = "cd" * 32

class ApkReleaseGateTest(TestCase):
    def test_package_signer_parse(self):
        package = gate.parse_aapt("package: name='com.yooshastan.ult' versionCode='74' versionName='0.14' platformBuildVersionCode=''\n")
        self.assertEqual(package, ("com.yooshastan.ult",74,"0.14"))
        self.assertEqual(gate.parse_signing("Signer #1 certificate SHA-256 digest: " + ONE + "\n"),ONE)
        with self.assertRaises(ValueError):
            gate.parse_signing("")
        with self.assertRaises(ValueError):
            gate.parse_signing("Signer #1 certificate SHA-256 digest: " + ONE
                               + "\nSigner #2 certificate SHA-256 digest: " + TWO)
        with self.assertRaises(ValueError):
            gate.parse_aapt("bad output")
    def test_safe_upgrade_decisions(self):
        old = ("com.yooshastan.ult",72,"0.14c",ONE)
        self.assertTrue(gate.compare(old, ("com.yooshastan.ult",74,"0.14d",ONE)))
        for new in [
            ("com.yooshastan.other",74,"0.14d",ONE),
            ("com.yooshastan.ult",74,"0.14d",TWO),
            ("com.yooshastan.ult",71,"0.14d",ONE)
        ]:
            with self.subTest(new=new), self.assertRaises(ValueError):
                gate.compare(old,new)
    def test_no_false_upgrade_claim_without_previous(self):
        report = gate.render(("com.yooshastan.ult",74,"0.14d",ONE))
        self.assertIn("NOT CHECKED", report)
        self.assertIn("Debug-signed", report)
        self.assertNotIn("Upgrade compatibility with selected previous APK: PASS",report)

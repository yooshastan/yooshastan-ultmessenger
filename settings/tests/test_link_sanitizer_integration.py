"""Pinned-source contract: sanitizer edits text BEFORE entity extraction and fails closed."""
import importlib.util
from pathlib import Path
import tempfile
import unittest

ROOT = Path(__file__).resolve().parents[2]
SCRIPT = ROOT / "scripts/integrate-link-sanitizer.py"
spec = importlib.util.spec_from_file_location("ult_link_sanitizer", SCRIPT)
module = importlib.util.module_from_spec(spec)
spec.loader.exec_module(module)


class LinkSanitizerIntegrationTest(unittest.TestCase):
    def setup_source(self, root):
        target = root / module.CHAT
        target.parent.mkdir(parents=True)
        target.write_text("header\n" + module.METHOD + "rest\n" + module.SEND + "\nend\n")
        policy_dir = root / "TMessagesProj/src/main/java/com/yooshastan/ultmessenger/settings"
        policy_dir.mkdir(parents=True)
        for name in ("UltLinkSanitizer.java", "UltLinkSanitizerPolicy.java"):
            (policy_dir / name).write_text("dummy model")
        return target

    def test_happens_before_entities_and_preserves_webpage_privacy(self):
        with tempfile.TemporaryDirectory() as d:
            target = self.setup_source(Path(d))
            module.integrate(Path(d))
            output = target.read_text()
            self.assertIn("UltLinkSanitizer.sanitizeOutgoing(", output)
            self.assertIn("ultLinksChanged ? null : messageWebPage", output)
            self.assertIn("!ultLinksChanged && isMessageWebPageSearchEnabled()", output)
            with self.assertRaisesRegex(ValueError, "already integrated"):
                module.integrate(Path(d))

    def test_changed_upstream_fails_without_mutation(self):
        with tempfile.TemporaryDirectory() as d:
            target = self.setup_source(Path(d))
            target.write_text("not the pinned code")
            before = target.read_bytes()
            with self.assertRaisesRegex(ValueError, "Changed upstream"):
                module.integrate(Path(d))
            self.assertEqual(target.read_bytes(), before)


if __name__ == "__main__":
    unittest.main()

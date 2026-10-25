#!/usr/bin/env python3
"""Fail-closed proxy source integration tests without downloading Android."""
import importlib.util
from pathlib import Path
from tempfile import TemporaryDirectory
from unittest import TestCase, main

MODULE = Path(__file__).resolve().parents[2] / "scripts/integrate-proxy.py"
spec = importlib.util.spec_from_file_location("ult_proxy_integration", MODULE)
integration = importlib.util.module_from_spec(spec)
spec.loader.exec_module(integration)


class ProxyIntegrationTest(TestCase):
    def test_injects_native_proxy_labels_and_privacy_action(self):
        with TemporaryDirectory() as temp:
            root = Path(temp)
            target = root / integration.TARGET
            target.parent.mkdir(parents=True)
            original = (integration.IMPORT_ANCHOR + integration.CLASS_ANCHOR +
                        integration.EDIT_ANCHOR + integration.DISPLAY_ANCHOR +
                        integration.MENU_ANCHOR + integration.CLICK_ANCHOR)
            target.write_text(original, encoding="utf-8")
            integration.integrate(root)
            result = target.read_text(encoding="utf-8")
            self.assertIn("ULT Proxy Manager", result)
            self.assertIn("UltProxyLabels.displayName", result)
            self.assertIn("getUltProxyLabel", result)
            self.assertIn("showUltProxyActions", result)
            self.assertIn("showUltProxyPrivacyInfo", result)
            self.assertIn("showUltProxyDiagnostics", result)
            self.assertIn("ULT Connection Diagnostics", result)
            self.assertIn("UltProxyDiagnostics.routeStatus", result)
            self.assertIn("UltProxyDiagnostics.nativeProbeStatus", result)
            self.assertIn("UltProxyDiagnostics.dnsAuditWarning", result)
            self.assertNotIn("native_setProxySettings", result)
            self.assertIn("A proxy is not a VPN", result)
            self.assertIn("info.mgInternal", result)
            self.assertTrue((root / "TMessagesProj/src/main/java/com/yooshastan/ultmessenger/proxy/UltProxyLabels.java").is_file())
            self.assertTrue((root / "TMessagesProj/src/main/java/com/yooshastan/ultmessenger/proxy/UltProxyDiagnostics.java").is_file())
            with self.assertRaises(ValueError):
                integration.integrate(root)

    def test_fail_closed_on_changed_upstream(self):
        with TemporaryDirectory() as temp:
            root = Path(temp)
            target = root / integration.TARGET
            target.parent.mkdir(parents=True)
            target.write_text("upstream refactored")
            with self.assertRaises(ValueError):
                integration.integrate(root)
            self.assertEqual(target.read_text(), "upstream refactored")


if __name__ == "__main__":
    main()

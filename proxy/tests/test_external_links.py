#!/usr/bin/env python3
"""Source-integrity tests for Alpha 0.10e without a full Android checkout."""
import importlib.util
from pathlib import Path
from tempfile import TemporaryDirectory
from unittest import TestCase, main

MODULE = Path(__file__).resolve().parents[2] / "scripts/integrate-external-links.py"
spec = importlib.util.spec_from_file_location("ult_external_links", MODULE)
integration = importlib.util.module_from_spec(spec)
spec.loader.exec_module(integration)


def source_fixture(root):
    browser = root / integration.BROWSER
    browser.parent.mkdir(parents=True, exist_ok=True)
    browser.write_text(
        integration.BROWSER_IMPORT_ANCHOR
        + "\npublic class Browser {\n"
        + integration.BROWSER_METHOD
        + "        // Original dispatch body must survive the wrapper.\n    }\n}\n",
        encoding="utf-8",
    )
    proxy = root / integration.PROXY_UI
    proxy.parent.mkdir(parents=True, exist_ok=True)
    proxy.write_text(
        integration.PROXY_IMPORT_ANCHOR
        + "\nclass ProxyListActivity {\n"
        + "    void showStatus() {\n"
        + integration.PROXY_STATUS_ANCHOR
        + "    }\n"
        + integration.PROXY_PRIVACY_METHOD_ANCHOR
        + "        new android.app.AlertDialog.Builder(null)\n"
        + integration.PROXY_PRIVACY_TITLE_ANCHOR
        + "                .setPositiveButton(\"OK\", null);\n    }\n}\n",
        encoding="utf-8",
    )
    return browser, proxy


class ExternalLinkIntegrationTest(TestCase):
    def test_guards_open_dispatch_and_adds_settings(self):
        with TemporaryDirectory() as temp:
            root = Path(temp)
            browser, proxy = source_fixture(root)
            integration.integrate(root)
            browser_text = browser.read_text(encoding="utf-8")
            proxy_text = proxy.read_text(encoding="utf-8")
            self.assertIn("UltExternalLinkPolicy.shouldConfirm", browser_text)
            self.assertIn('setTitle("Open external website?")', browser_text)
            self.assertIn('setPositiveButton("Open once"', browser_text)
            self.assertIn("private static void openUltApprovedUrl", browser_text)
            self.assertIn("Original dispatch body must survive", browser_text)
            self.assertIn("showUltExternalLinkSettings", proxy_text)
            self.assertIn("Native strict fail-closed: Not implemented", proxy_text)
            self.assertIn("Link guard settings", proxy_text)
            for name in integration.MODELS:
                self.assertTrue((root / integration.ANDROID_PACKAGE / name).is_file())
            with self.assertRaises(ValueError):
                integration.integrate(root)

    def test_changed_upstream_never_partially_written(self):
        with TemporaryDirectory() as temp:
            root = Path(temp)
            browser, proxy = source_fixture(root)
            original_browser = browser.read_text(encoding="utf-8")
            proxy.write_text("refactored UI", encoding="utf-8")
            with self.assertRaises(ValueError):
                integration.integrate(root)
            self.assertEqual(browser.read_text(encoding="utf-8"), original_browser)
            self.assertEqual(proxy.read_text(encoding="utf-8"), "refactored UI")


if __name__ == "__main__":
    main()

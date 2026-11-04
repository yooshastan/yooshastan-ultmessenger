#!/usr/bin/env python3
"""Regression contract: saved strict ON reconnects after native proxy restore."""
from pathlib import Path
from unittest import TestCase

ROOT = Path(__file__).resolve().parents[2]
PATCH = ROOT / "scripts/integrate-native-strict.py"

class StrictStartupRecoveryContract(TestCase):
    def test_early_guard_and_post_init_retry(self):
        text = PATCH.read_text(encoding="utf-8")
        self.assertIn('native_setStrictProxyMode(currentAccount, preferences.getBoolean("ult_strict_native_proxy", false))', text)
        self.assertIn("JAVA_STARTUP_DONE", text)
        self.assertIn("'            native_setStrictProxyMode(currentAccount, true);\\n'", text)
        self.assertIn("const bool awaitingProxy = enabled && proxyAddress.empty();", text)
        self.assertIn("if (!awaitingProxy && networkAvailable && !networkPaused && primary != nullptr)", text)
        self.assertIn("if (!awaitingProxy) {", text)

    def test_native_proxy_application_wakes_strict_connection(self):
        text = PATCH.read_text(encoding="utf-8")
        self.assertIn("CPP_PROXY_FINISH", text)
        self.assertIn("strictProxyMode.load() && !proxyAddress.empty()", text)
        self.assertIn("datacenter->createGenericConnection()->connect();", text)
        self.assertIn("(!enabled || !proxyAddress.empty())", text)
        self.assertIn("overrideProxyAddress.empty()", text)
        self.assertNotIn("setUltStrictNativeProxyEnabled(false);", text)

if __name__ == "__main__":
    from unittest import main
    main()

#!/usr/bin/env python3
"""Alpha 0.10h.2: strict-mode configuration, recovery and native state contract."""
from pathlib import Path
from unittest import TestCase

ROOT = Path(__file__).resolve().parents[2]
PATCH = ROOT / "scripts/integrate-native-strict.py"
UI = ROOT / "settings/src/com/yooshastan/ultmessenger/settings/UltSettingsActivity.java"
POLICY = ROOT / "proxy/src/com/yooshastan/ultmessenger/proxy/UltStrictProxyPolicy.java"
REPORT = ROOT / "settings/src/com/yooshastan/ultmessenger/settings/UltNetworkTestReport.java"
PROXY = ROOT / "proxy/src/com/yooshastan/ultmessenger/proxy/UltProxyDiagnostics.java"


class StrictRecoveryContract(TestCase):
    def test_native_guard_does_not_weaken_automatically(self):
        src = PATCH.read_text(encoding="utf-8")
        for marker in (
                "strictProxyMode.store(enabled)", "std::atomic<bool> strictProxyMode",
                "canEnableUltStrictNativeProxy()", "native_isStrictProxyModeEnabled",
                "if (!enabled && networkAvailable && !networkPaused",
                "if (networkAvailable && !networkPaused && primary != nullptr)",
                "createGenericConnection()->connect()", "processRequestQueue(0, 0)",
                "if (useProxySettings && ConnectionsManager.isUltStrictNativeProxyEnabled())",
                "closeSocket(1, -1);", "overrideProxyAddress.empty()"):
            with self.subTest(marker=marker):
                self.assertIn(marker, src)
        self.assertNotIn("setUltStrictNativeProxyEnabled(false);", src)

    def test_ui_denies_invalid_enable_and_explains_safeguards(self):
        ui = UI.read_text(encoding="utf-8")
        policy = POLICY.read_text(encoding="utf-8")
        for marker in (
                "ConnectionsManager.canEnableUltStrictNativeProxy()",
                "Strict mode cannot be enabled while Use Proxy is OFF",
                "Disable and reconnect",
                "native_isStrictProxyModeEnabled(UserConfig.selectedAccount)",
                "NOT a full firewall"):
            self.assertIn(marker, ui)
        self.assertIn('setNeutralButton("Disable and reconnect"', ui)
        self.assertIn("setUltStrictNativeProxyEnabled(false)", ui)
        self.assertIn('setNegativeButton(requested || observed ? null : "Enable (experimental)"', ui)
        self.assertIn("return !strictRequested;", policy)
        self.assertIn("!torActive", policy)

    def test_reports_never_claim_leak_proof(self):
        report = REPORT.read_text(encoding="utf-8")
        proxy = PROXY.read_text(encoding="utf-8")
        self.assertIn("STATE MISMATCH", report)
        self.assertIn("native transition may be pending", report)
        self.assertIn("NO PER-APP ROUTE, DNS PRIVACY OR FAIL-CLOSED GUARANTEE", report)
        self.assertNotIn("Alpha 0.10d local status", proxy)


if __name__ == "__main__":
    from unittest import main
    main()

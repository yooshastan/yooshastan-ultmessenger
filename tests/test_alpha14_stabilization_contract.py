"""Read-only signing and no-leak-claim regression contracts."""
from pathlib import Path
from unittest import TestCase
ROOT=Path(__file__).resolve().parents[1]
def read(p): return (ROOT/p).read_text(encoding="utf-8")

class StabilizationSourceContract(TestCase):
    def test_copy_compare_never_leaks_result_across_accounts(self):
        src=read("settings/src/com/yooshastan/ultmessenger/settings/UltDownloadsActivity.java")
        for needle in ("currentAccount != account", "isExportAllowed(message, account)",
                       "clearPendingVerify()", "Intent.ACTION_OPEN_DOCUMENT",
                       "UltFileInspection.sameContent(original, copy)",
                       "getParentActivity() != activity"):
            self.assertIn(needle,src)
        self.assertNotIn("new java.net.",src)
    def test_proxy_guard_is_still_on_both_native_socket_paths(self):
        native=read("scripts/integrate-native-strict.py")
        self.assertIn("SOCKET_GUARD =",native)
        self.assertIn("SOCKET_INTERNAL_GUARD =",native)
        self.assertGreaterEqual(native.count("ultManager.strictProxyMode && ultManager.proxyAddress.empty()"),2)
        self.assertIn("overrideProxyAddress.empty()",native)
        self.assertGreaterEqual(native.count("closeSocket(1, -1);"),2)
        self.assertIn("native_setStrictProxyMode(account, enabled)",native)
        self.assertIn("native_isStrictProxyModeEnabled",native)
    def test_signing_gate_does_not_infer_compatibility(self):
        script=read("scripts/apk_release_gate.py")
        for s in ("apksigner", "aapt", "--previous",
                  "Upgrade compatibility: NOT CHECKED",
                  "Signing certificate differs", "Version code goes backwards"):
            self.assertIn(s,script)
        self.assertNotIn("adb uninstall",script)

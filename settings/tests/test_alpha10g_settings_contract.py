#!/usr/bin/env python3
"""Guard the tested settings hub and the read-only VPN indicator from drift."""
from pathlib import Path
from unittest import TestCase, main

ROOT = Path(__file__).resolve().parents[2]
SETTINGS = ROOT / "settings/src/com/yooshastan/ultmessenger/settings/UltSettingsActivity.java"
INSPECTOR = ROOT / "settings/src/com/yooshastan/ultmessenger/settings/UltAndroidDnsInspector.java"
NATIVE_PATCH = ROOT / "scripts/integrate-native-strict.py"


class Alpha10gSettingsContract(TestCase):
    def test_existing_hub_and_inactive_preview_caveats_remain_honest(self):
        src = SETTINGS.read_text(encoding="utf-8")
        for marker in (
                "Ultgram Settings", "ULT theme presets", "Chat views and sorting",
                "Proxy manager", "External link confirmation",
                "Strict proxy (native sockets)", "Android Private DNS status",
                "Network diagnostics", "Privacy limitations explained",
                "DoQ QUIC backend: NOT INSTALLED", "DNS tracker rules: SAVED DRAFT — NOT ACTIVE",
                "UltAndroidDnsInspector.inspectVpn(context)",
                "UltAndroidDnsInspector.inspect(context)"):
            with self.subTest(marker=marker):
                self.assertIn(marker, src)
        main = src.split("fragmentView = scroll;", 1)[0]
        self.assertNotIn('"DNS-over-QUIC configuration (preview)"', main)
        self.assertNotIn('"Tracker domain rules (preview)"', main)
        self.assertIn("showNativeStrictProxy(context)", main)
        self.assertIn("showExternalLinkGuard(context)", main)

    def test_vpn_indicator_is_read_only_and_denies_route_claims(self):
        src = INSPECTOR.read_text(encoding="utf-8")
        self.assertIn("NetworkCapabilities.TRANSPORT_VPN", src)
        self.assertIn("Ultgram routing NOT verified", src)
        self.assertIn("Unknown (Android did not expose VPN state)", src)
        self.assertNotIn("startActivity", src)
        self.assertNotIn("startVpn", src)

    def test_native_guard_keeps_both_socket_checks_without_renaming(self):
        patch = NATIVE_PATCH.read_text(encoding="utf-8")
        self.assertIn("SOCKET_GUARD", patch)
        self.assertIn("SOCKET_INTERNAL_GUARD", patch)
        self.assertIn("overrideProxyAddress.empty()", patch)
        self.assertIn("blocked late direct tgnet socket", patch)
        self.assertIn("native_setStrictProxyMode", patch)


if __name__ == "__main__":
    main()

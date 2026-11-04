#!/usr/bin/env python3
"""Synthetic regression data only; tests do not capture or verify app traffic."""
import importlib.util
from ipaddress import ip_address
from pathlib import Path
from unittest import TestCase, main

SOURCE = Path(__file__).resolve().parents[2] / "scripts/alpha10g-path-audit.py"
spec = importlib.util.spec_from_file_location("ult_alpha10g_audit", SOURCE)
audit = importlib.util.module_from_spec(spec)
spec.loader.exec_module(audit)

PHONE = ip_address("192.0.2.58")
VPN = ip_address("198.51.100.4")
PROXY = ip_address("203.0.113.10")
WG_PHONE = ip_address("10.77.0.2")


class Alpha10gAuditTests(TestCase):
    def test_phone_edge_reports_outer_tunnel_and_candidate_direct_paths(self):
        records = [
            "21:01 IP 192.0.2.58.50000 > 198.51.100.4.51820: UDP, length 112",
            "21:01 IP 192.0.2.58.50001 > 203.0.113.10.8443: Flags [P.], length 24",
            "21:01 IP 192.0.2.58.50002 > 198.51.100.11.53: UDP, length 43",
            "21:01 IP 192.0.2.58.50003 > 198.51.100.12.443: Flags [S], length 0",
            "21:01 IP 203.0.113.10.8443 > 192.0.2.58.50001: Flags [.], length 0",
            "21:01 IP 192.0.2.59.50000 > 198.51.100.4.51820: UDP, length 112",
        ]
        data = audit.summarize(records, "phone-edge", {PHONE}, {VPN}, 51820, {PROXY}, 8443)
        self.assertEqual(data["matched"], 4)
        self.assertEqual(data["counts"]["wireguard_outer_candidate"], 1)
        self.assertEqual(data["counts"]["mtproxy_tcp_candidate"], 1)
        self.assertEqual(data["counts"]["dns_port53_observed"], 1)
        self.assertEqual(data["counts"]["other_endpoint_candidate"], 1)
        summary = audit.report("phone-edge", data)
        self.assertNotIn("198.51.100.11", summary)
        self.assertNotIn("203.0.113.10", summary)
        self.assertIn("not visible", summary)
        self.assertIn("NO LEAK-PROOF", summary)

    def test_tunnel_inner_dns_does_not_claim_phone_edge_leak(self):
        records = [
            "IP 10.77.0.2.40001 > 10.77.0.1.53: UDP, length 20",
            "IP 10.77.0.2.40002 > 203.0.113.10.8443: Flags [S], length 0",
            "IP6 2001:db8::5.10000 > 2001:db8::8.443: UDP, length 87",
        ]
        data = audit.summarize(records, "tunnel-inner", {WG_PHONE, ip_address("2001:db8::5")},
                               {VPN}, 51820, {PROXY}, 8443)
        self.assertEqual(data["matched"], 3)
        self.assertEqual(data["counts"]["dns_port53_observed"], 1)
        self.assertEqual(data["counts"]["mtproxy_tcp_candidate"], 1)
        self.assertEqual(data["counts"]["ipv6_subset"], 1)
        self.assertIn("INSIDE WireGuard", audit.report("tunnel-inner", data))
        self.assertNotIn("phone-edge DNS leak detected", audit.report("tunnel-inner", data))

    def test_empty_or_unparseable_capture_is_not_a_pass(self):
        self.assertIsNone(audit.parse_packet("ICMP or arbitrary garbage"))
        self.assertIsNone(audit.parse_packet("IP 192.0.2.58 > 203.0.113.10: ICMP echo"))
        data = audit.summarize(["no packets"], "phone-edge", {PHONE}, {VPN}, 51820,
                               {PROXY}, 8443)
        self.assertIn("visibility is unproven", audit.report("phone-edge", data))


if __name__ == "__main__":
    main()

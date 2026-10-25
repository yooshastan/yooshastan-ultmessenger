#!/usr/bin/env python3
"""Read-only traffic classifier regression tests; no root or network needed."""
import importlib.util
from ipaddress import ip_address
from pathlib import Path
from unittest import TestCase, main

SCRIPT = Path(__file__).resolve().parents[2] / "scripts/alpha10d-netwatch.py"
spec = importlib.util.spec_from_file_location("ult_netwatch", SCRIPT)
netwatch = importlib.util.module_from_spec(spec)
spec.loader.exec_module(netwatch)

PHONE = ip_address("192.0.2.15")
PROXY = ip_address("198.51.100.20")
V6PHONE = ip_address("2001:db8::5")


class NetwatchTest(TestCase):
    def test_packet_parses_tcp_udp_and_ipv6(self):
        self.assertEqual(
            netwatch.packet("12:00:00 IP 192.0.2.15.53000 > 198.51.100.20.8443: Flags [S], length 0"),
            (PHONE, 53000, PROXY, 8443, "tcp"))
        self.assertEqual(
            netwatch.packet("12:00:01 IP6 2001:db8::5.53000 > 2001:db8::6.53: UDP, length 45"),
            (V6PHONE, 53000, ip_address("2001:db8::6"), 53, "udp"))
        self.assertIsNone(netwatch.packet("garbage text"))
        self.assertIsNone(netwatch.packet("IP 192.0.2.15 > 198.51.100.20: ICMP echo"))

    def test_gateway_classifies_only_phone_outbound(self):
        lines = [
            "12:00:00 IP 192.0.2.15.53000 > 198.51.100.20.8443: Flags [S], length 0",
            "12:00:01 IP 192.0.2.15.53001 > 1.1.1.1.53: UDP, length 22",
            "12:00:01 IP 192.0.2.15.53002 > 1.1.1.1.853: Flags [S], length 0",
            "12:00:01 IP 192.0.2.15.53003 > 1.1.1.1.443: UDP, length 1200",
            "12:00:01 IP6 2001:db8::5.53004 > 2001:db8::6.443: Flags [S], length 0",
            "12:00:01 IP 203.0.113.25.53000 > 198.51.100.20.8443: Flags [S], length 0",
            "12:00:01 IP 198.51.100.20.8443 > 192.0.2.15.53000: Flags [S.], length 0",
        ]
        result = netwatch.summarize(lines, "gateway", {PHONE, V6PHONE}, {PROXY}, 8443)
        self.assertEqual(result["matched"], 5)
        self.assertEqual(result["counts"]["proxy_endpoint"], 1)
        self.assertEqual(result["counts"]["plaintext_dns_port53"], 1)
        self.assertEqual(result["counts"]["encrypted_dns_port853_candidate"], 1)
        self.assertEqual(result["counts"]["udp443_quic_candidate"], 1)
        self.assertEqual(result["counts"]["other_direct_candidate"], 1)
        self.assertEqual(result["counts"]["ipv6_packets"], 1)

    def test_proxy_host_cannot_claim_leak_verdict(self):
        lines = [
            "21:27:54 IP 203.0.113.150.3747 > 192.0.2.169.8443: Flags [S], length 0",
            "21:27:55 IP 192.0.2.169.8443 > 203.0.113.150.3747: Flags [S.], length 0",
            "21:27:55 IP 198.51.100.51.55255 > 192.0.2.169.8443: Flags [S], length 0",
        ]
        result = netwatch.summarize(lines, "proxy-host", set(),
                                    {ip_address("192.0.2.169")}, 8443)
        self.assertEqual(result["matched"], 2)
        self.assertEqual(result["remote_source_count"], 2)
        report = netwatch.format_summary("proxy-host", result)
        self.assertIn("Cannot observe phone DNS", report)
        self.assertNotIn("203.0.113.150", report)
        self.assertNotIn("198.51.100.51", report)
        self.assertIn("NO LEAK-PROOF", report)

    def test_zero_packets_is_not_a_pass(self):
        result = netwatch.summarize(["no matching data"], "gateway",
                                    {PHONE}, {PROXY}, 8443)
        self.assertIn("visibility is unproven",
                      netwatch.format_summary("gateway", result))
        self.assertNotIn("pass", netwatch.format_summary("gateway", result).lower())


if __name__ == "__main__":
    main()

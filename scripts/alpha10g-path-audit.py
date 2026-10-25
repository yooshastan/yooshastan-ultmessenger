#!/usr/bin/env python3
"""Summarize tcpdump TEXT around Ultgram + WireGuard experiments.

No packet capture, network probes, file writes, hostnames, payloads or leak
verdicts. The source-IP filter identifies a *device*, not the Ultgram app.
Capture visibility and Android app attribution must be verified separately.
"""
import argparse
from collections import Counter
from ipaddress import ip_address
import re
import sys

PACKET = re.compile(r"\bIP6?\s+(\S+)\s+>\s+(\S+):\s*(.*)$")
HOST_PORT = re.compile(r"(.+)\.(\d+)$")


def parse_endpoint(text):
    match = HOST_PORT.fullmatch(text)
    if not match:
        return None
    try:
        host = ip_address(match.group(1))
        port = int(match.group(2))
    except ValueError:
        return None
    return (host, port) if 0 <= port <= 65535 else None


def parse_packet(line):
    match = PACKET.search(line)
    if not match:
        return None
    src, dst = parse_endpoint(match.group(1)), parse_endpoint(match.group(2))
    if src is None or dst is None:
        return None
    flags = match.group(3)
    if "Flags [" in flags or re.search(r"\btcp\b", flags, re.I):
        protocol = "tcp"
    elif re.search(r"\bUDP\b", flags, re.I):
        protocol = "udp"
    else:
        return None
    return src[0], src[1], dst[0], dst[1], protocol


def summarize(lines, role, source_ips, vpn_ips, vpn_port, proxy_ips, proxy_port):
    """Classify observed packets, never infer that they came from Ultgram."""
    counts = Counter()
    parsed = matched = 0
    for line in lines:
        packet = parse_packet(line)
        if packet is None:
            continue
        parsed += 1
        src, _, dst, dport, protocol = packet
        if src not in source_ips:
            continue
        matched += 1
        if role == "phone-edge" and protocol == "udp" and dst in vpn_ips and dport == vpn_port:
            category = "wireguard_outer_candidate"
        elif protocol == "tcp" and dst in proxy_ips and dport == proxy_port:
            category = "mtproxy_tcp_candidate"
        elif dport == 53:
            category = "dns_port53_observed"
        elif dport == 853:
            category = "encrypted_dns_port853_candidate"
        elif protocol == "udp" and dport == 443:
            category = "udp443_quic_candidate"
        else:
            category = "other_endpoint_candidate"
        counts[category] += 1
        if src.version == 6 or dst.version == 6:
            counts["ipv6_subset"] += 1
    return {"parsed": parsed, "matched": matched, "counts": counts}


def report(role, result):
    counts = result["counts"]
    lines = [
        "ULT Alpha 0.10g path observation (not a privacy verdict)",
        "Capture role: " + role,
        "Parsed TCP/UDP packets: " + str(result["parsed"]),
        "Matched source-device packets: " + str(result["matched"]),
        "Possible outer WireGuard UDP: " + str(counts["wireguard_outer_candidate"]),
        "To configured MTProxy TCP: " + str(counts["mtproxy_tcp_candidate"]),
        "To port 53 (DNS candidate): " + str(counts["dns_port53_observed"]),
        "To port 853 (encrypted DNS candidate): " + str(counts["encrypted_dns_port853_candidate"]),
        "To UDP port 443 (QUIC candidate): " + str(counts["udp443_quic_candidate"]),
        "Other endpoints (media/CDN/direct/unknown): " + str(counts["other_endpoint_candidate"]),
        "IPv6 packets (subset): " + str(counts["ipv6_subset"]),
    ]
    if role == "phone-edge":
        lines.append("PHONE EDGE: inner tunnel traffic is encrypted and not visible here.")
        lines.append("Unclassified egress can belong to Android or unrelated apps.")
    else:
        lines.append("TUNNEL INNER: port 53 seen INSIDE WireGuard is not a phone-edge DNS leak.")
        lines.append("Tunnel capture alone cannot observe direct phone egress outside VPN.")
    if result["matched"] == 0:
        lines.append("NO MATCHED PACKETS: capture visibility is unproven.")
    lines.append("NO LEAK-PROOF, ENCRYPTED-DNS OR FAIL-CLOSED CLAIM IS JUSTIFIED.")
    return "\n".join(lines)


def cli(argv=None):
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--capture-role", choices=("phone-edge", "tunnel-inner"), required=True)
    parser.add_argument("--source-ip", action="append", required=True,
                        help="Observed phone IP at this capture point; repeat for IPv6")
    parser.add_argument("--vpn-ip", action="append", default=[],
                        help="WireGuard server public IP (phone-edge role only)")
    parser.add_argument("--vpn-port", type=int, default=51820)
    parser.add_argument("--proxy-ip", action="append", default=[],
                        help="Known MTProxy address visible at capture point")
    parser.add_argument("--proxy-port", type=int, default=8443)
    args = parser.parse_args(argv)
    if not (1 <= args.vpn_port <= 65535 and 1 <= args.proxy_port <= 65535):
        parser.error("Ports must be between 1 and 65535")
    if args.capture_role == "phone-edge" and not args.vpn_ip:
        parser.error("--vpn-ip is required for phone-edge captures")
    try:
        source = {ip_address(s) for s in args.source_ip}
        vpn = {ip_address(s) for s in args.vpn_ip}
        proxy = {ip_address(s) for s in args.proxy_ip}
    except ValueError:
        parser.error("All IPs must be numeric IPv4/IPv6 literals")
    result = summarize(sys.stdin, args.capture_role, source, vpn,
                       args.vpn_port, proxy, args.proxy_port)
    print(report(args.capture_role, result))
    return 0


if __name__ == "__main__":
    raise SystemExit(cli())

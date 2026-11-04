#!/usr/bin/env python3
"""Classify tcpdump TEXT for Ultgram's manual Alpha 0.10d proxy audit.

This tool reads stdin only, prints aggregate metadata only, and never captures
packets, opens sockets, writes files, or attributes traffic to an Android app.
For leak assessment, the capture must be taken at a gateway/AP that observes
the device's egress before NAT; a proxy-host capture is connectivity-only.
"""
import argparse
from collections import Counter
from ipaddress import ip_address
import re
import sys

IP_LINE = re.compile(r"\bIP6?\s+(\S+)\s+>\s+(\S+):\s*(.*)$")
ENDPOINT = re.compile(r"(.+)\.(\d+)$")
CATEGORIES = (
    "proxy_endpoint",
    "plaintext_dns_port53",
    "encrypted_dns_port853_candidate",
    "udp443_quic_candidate",
    "other_direct_candidate",
)


def endpoint(token):
    match = ENDPOINT.fullmatch(token)
    if not match:
        return None
    try:
        address = ip_address(match.group(1))
        port = int(match.group(2))
    except ValueError:
        return None
    if port > 65535:
        return None
    return address, port


def packet(line):
    """Return (source IP, source port, destination IP, dest port, protocol)."""
    match = IP_LINE.search(line)
    if not match:
        return None
    source = endpoint(match.group(1))
    dest = endpoint(match.group(2))
    if source is None or dest is None:
        return None
    details = match.group(3)
    if "Flags [" in details or re.search(r"\btcp\b", details, re.I):
        protocol = "tcp"
    elif re.search(r"\bUDP\b", details, re.I):
        protocol = "udp"
    else:
        return None
    return source[0], source[1], dest[0], dest[1], protocol


def summarize(lines, role, phone_ips, proxy_ips, proxy_port):
    """Aggregate observed packets, not flows, apps, DNS names or leak verdicts."""
    counts = Counter()
    remote_sources = set()
    parsed = matched = 0
    for line in lines:
        item = packet(line)
        if item is None:
            continue
        parsed += 1
        source, _, dest, port, protocol = item
        if role == "proxy-host":
            if dest in proxy_ips and port == proxy_port and protocol == "tcp":
                matched += 1
                remote_sources.add(source)
                counts["proxy_endpoint"] += 1
            continue
        if source not in phone_ips:
            continue
        matched += 1
        if dest in proxy_ips and port == proxy_port and protocol == "tcp":
            counts["proxy_endpoint"] += 1
        elif port == 53:
            counts["plaintext_dns_port53"] += 1
        elif port == 853:
            counts["encrypted_dns_port853_candidate"] += 1
        elif port == 443 and protocol == "udp":
            counts["udp443_quic_candidate"] += 1
        else:
            counts["other_direct_candidate"] += 1
        if source.version == 6 or dest.version == 6:
            counts["ipv6_packets"] += 1
    return {
        "parsed": parsed,
        "matched": matched,
        "counts": counts,
        "remote_source_count": len(remote_sources),
    }


def format_summary(role, result):
    counts = result["counts"]
    lines = [
        "ULT Alpha 0.10d network observation (packet counts, not proof of privacy)",
        "Capture role: " + role,
        "Parsed TCP/UDP packets: " + str(result["parsed"]),
        "Matched packets: " + str(result["matched"]),
    ]
    if role == "proxy-host":
        lines += [
            "Inbound packets to configured proxy: " + str(counts["proxy_endpoint"]),
            "Distinct source IPs (not necessarily your phone): "
            + str(result["remote_source_count"]),
            "PROXY-HOST ONLY: Cannot observe phone DNS or direct egress.",
        ]
    else:
        lines += [
            "To configured TCP proxy: " + str(counts["proxy_endpoint"]),
            "To plaintext DNS port 53: " + str(counts["plaintext_dns_port53"]),
            "To encrypted DNS port 853 (candidate): "
            + str(counts["encrypted_dns_port853_candidate"]),
            "To UDP 443 (QUIC candidate): " + str(counts["udp443_quic_candidate"]),
            "Other direct destinations (candidate): "
            + str(counts["other_direct_candidate"]),
            "IPv6 packets (subset of above): " + str(counts["ipv6_packets"]),
            "Traffic from Android system/other apps cannot be attributed to Ultgram.",
            "Unknown destinations and encrypted DNS remain unverified.",
        ]
    if result["matched"] == 0:
        lines.append("NO MATCHING PACKETS: capture visibility is unproven.")
    lines.append("NO LEAK-PROOF OR FAIL-CLOSED CLAIM CAN BE MADE FROM THIS SUMMARY.")
    return "\n".join(lines)


def cli(argv=None):
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--capture-role", choices=("gateway", "proxy-host"),
                        required=True)
    parser.add_argument("--phone-ip", action="append", default=[],
                        help="Phone IP visible BEFORE NAT; repeat for IPv4/IPv6")
    parser.add_argument("--proxy-ip", action="append", required=True,
                        help="Proxy IP as observed at capture point; repeat for aliases")
    parser.add_argument("--proxy-port", type=int, required=True)
    args = parser.parse_args(argv)
    if args.capture_role == "gateway" and not args.phone_ip:
        parser.error("--phone-ip required for gateway capture")
    if not (1 <= args.proxy_port <= 65535):
        parser.error("--proxy-port must be in range 1..65535")
    try:
        phone_ips = {ip_address(ip) for ip in args.phone_ip}
        proxy_ips = {ip_address(ip) for ip in args.proxy_ip}
    except ValueError:
        parser.error("Phone/proxy addresses must be literal IPv4/IPv6 (no DNS)")
    try:
        result = summarize(sys.stdin, args.capture_role, phone_ips,
                           proxy_ips, args.proxy_port)
    except KeyboardInterrupt:
        # Allow terminal Ctrl+C to print the aggregate and discard raw input.
        result = {"parsed": 0, "matched": 0,
                  "counts": Counter(), "remote_source_count": 0}
        print("Interrupted; rerun with a finite capture for a valid summary.",
              file=sys.stderr)
        return 130
    print(format_summary(args.capture_role, result))
    return 0


if __name__ == "__main__":
    raise SystemExit(cli())

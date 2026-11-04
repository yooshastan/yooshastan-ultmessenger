package com.yooshastan.ultmessenger.settings;

/** Read-only source-review findings. Not a traffic monitor, DNS resolver or leak test. */
public final class UltNetworkSourceMatrix {
    private UltNetworkSourceMatrix() {}

    public static String render() {
        return "ULTGRAM 0.11b NETWORK / DNS SOURCE REVIEW\n"
                + "Scope: pinned Mercurygram + Ultgram guarded integration scripts.\n"
                + "Evidence type: source-level, not packet capture.\n\n"
                + "NATIVE TGNET: scripts/integrate-native-strict.py introduces a guarded "
                + "native socket gate. STATUS: implementation present, limited coverage; "
                + "outage/IPv6 route capture NOT VERIFIED.\n\n"
                + "JAVA HTTP / HOSTNAME RESOLUTION: upstream networking paths may bypass "
                + "native tgnet. STATUS: per-call-site ownership NOT COMPLETE; no whole-app "
                + "strict routing guarantee.\n\n"
                + "DNS / DoT / DoQ: Android DNS status is observational. Ultgram DoQ "
                + "endpoint parser and rules are configuration only; no native DoQ "
                + "transport or enforced tracker filter installed.\n\n"
                + "MEDIA / CDN: FileLoader and player/media transport must be independently "
                + "verified from device. STATUS: NOT ROUTE-VERIFIED.\n\n"
                + "WEBVIEW / PREVIEW / LINKS: scripts/integrate-external-links.py guards "
                + "eligible user link taps only; previews and Java WebView traffic "
                + "are NOT ROUTE-VERIFIED.\n\n"
                + "CALLS / UDP: real-time voice/video sockets need independent captures. "
                + "STATUS: NOT ROUTE-VERIFIED.\n\n"
                + "EVIDENCE NEEDED: phone-edge/gateway, tunnel-inner and proxy-host "
                + "captures under strict ON, failed proxy, IPv6 and Wi-Fi/cellular handoff. "
                + "A proxy-host capture alone is insufficient.\n"
                + "NO ACTIVE CAPTURE, DNS FILTERING, DoQ OR WHOLE-APP LEAK PROTECTION "
                + "IS PROVIDED BY THIS REPORT.";
    }
}

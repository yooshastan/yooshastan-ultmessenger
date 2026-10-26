package com.yooshastan.ultmessenger.proxy;

/**
 * Read-only interpretation of Telegram's existing proxy state.
 * No network connections, DNS resolutions or transport changes are made here.
 */
public final class UltProxyDiagnostics {
    private UltProxyDiagnostics() { }

    public static String routeStatus(boolean enabled, boolean selected,
                                     boolean connected, boolean updating) {
        if (!enabled) {
            return "Disabled (direct connections may be used)";
        }
        if (!selected) {
            return "Saved but not selected";
        }
        if (connected) {
            return "Selected; Telegram session connected (routing not proven)";
        }
        if (updating) {
            return "Selected; Telegram session updating (routing not proven)";
        }
        return "Selected; Telegram session not connected";
    }

    public static String nativeProbeStatus(boolean checking, long checkedAt,
                                           boolean available, long pingMillis) {
        if (checking) {
            return "Native Telegram reachability check running";
        }
        if (checkedAt <= 0) {
            return "Not yet checked";
        }
        if (!available) {
            return "Last native Telegram check failed";
        }
        if (pingMillis > 0) {
            return "Reachable in last native Telegram check (" + pingMillis + " ms)";
        }
        return "Reachable in last native Telegram check (latency unavailable)";
    }

    /**
     * Explicit user-triggered, endpoint-free clipboard snapshot.
     * Caller passes only native boolean/state values; transport is allowlisted.
     * This is not a network trace or proof of proxy-only routing.
     */
    public static String redactedAuditSnapshot(String transport, boolean enabled,
                                               boolean selected, boolean connected,
                                               boolean updating, boolean checking,
                                               long checkedAt, boolean available,
                                               long pingMillis) {
        String safeTransport;
        if ("MTPROTO".equals(transport) || "SOCKS5".equals(transport)
                || "WEB".equals(transport)) {
            safeTransport = transport;
        } else {
            safeTransport = "Other/unknown";
        }
        return "Ultgram local status snapshot (NOT a leak test)\n"
                + "Transport: " + safeTransport + "\n"
                + "Global Use Proxy switch: " + (enabled ? "On" : "Off") + "\n"
                + "Selection/session: " + routeStatus(enabled, selected, connected, updating) + "\n"
                + "Cached reachability: "
                + nativeProbeStatus(checking, checkedAt, available, pingMillis) + "\n\n"
                + dnsAuditWarning() + "\n"
                + "No proxy endpoint, credential, secret, chat text or account ID included. "
                + "Copied to Android clipboard only on request; other apps may access clipboard data.";
    }

    public static String dnsAuditWarning() {
        return "DNS routing unverified. DoQ/DoH3 and comprehensive strict no-direct-fallback "
                + "are NOT implemented. The experimental native socket guard is not a full "
                + "firewall. A native proxy check is not proof that Java, media or native "
                + "DNS traffic uses the proxy.";
    }
}

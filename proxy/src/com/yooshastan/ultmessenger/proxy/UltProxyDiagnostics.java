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

    public static String dnsAuditWarning() {
        return "DNS path NOT audited. Encrypted DNS and strict no-direct-fallback "
                + "are NOT implemented. A successful proxy check is not proof "
                + "that all Telegram, media, Java and native lookups use the proxy.";
    }
}

package com.yooshastan.ultmessenger.settings;

/**
 * Interprets Telegram's existing state without changing its sockets,
 * retry policy, DNS, proxy selection or strict-proxy implementation.
 */
public final class UltConnectionGuidance {
    private UltConnectionGuidance() {}

    public static String label(int state) {
        switch (state) {
            case 1: return "Connecting";
            case 2: return "Waiting for network";
            case 3: return "Connected";
            case 4: return "Connecting to proxy";
            case 5: return "Updating";
            default: return "Unknown state";
        }
    }

    public static String advice(int state, boolean strictRequested,
                                boolean nativeGuardActive, String transport) {
        if (strictRequested && !nativeGuardActive) {
            return "Strict guard state mismatch: do not assume protection. "
                    + "Check the selected proxy and native guard status.";
        }
        if ("Offline".equals(transport) || state == 2) {
            return "Network unavailable. Check Android connectivity. "
                    + "Ultgram does not bypass the selected strict proxy.";
        }
        if (state == 4) {
            return "Connecting to selected proxy. If stuck, check the proxy "
                    + "and network; no direct fallback is requested.";
        }
        if (state == 1) {
            return "Telegram is reconnecting. Switching networks may temporarily "
                    + "interrupt messages; do not disable strict mode to retry.";
        }
        if (state == 3 || state == 5) {
            return "Telegram reports a connection. This does not independently "
                    + "verify DNS, media, calls, or whole-device routing.";
        }
        return "Connection state unavailable; check native Telegram network status.";
    }
}

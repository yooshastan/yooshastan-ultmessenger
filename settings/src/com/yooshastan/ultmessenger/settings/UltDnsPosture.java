package com.yooshastan.ultmessenger.settings;

/** Read-only interpretation of Android Private DNS network state. */
public final class UltDnsPosture {
    public enum Mode { STRICT_DOT, OPPORTUNISTIC, INACTIVE, UNKNOWN }

    private UltDnsPosture() { }

    public static Mode classify(boolean networkAvailable, boolean active, String providerHost) {
        if (!networkAvailable) {
            return Mode.UNKNOWN;
        }
        if (!active) {
            return Mode.INACTIVE;
        }
        if (providerHost != null && !providerHost.trim().isEmpty()) {
            return Mode.STRICT_DOT;
        }
        return Mode.OPPORTUNISTIC;
    }

    public static String explain(Mode mode) {
        switch (mode) {
            case STRICT_DOT:
                return "Android reports strict Private DNS (DNS-over-TLS) active on the current network.";
            case OPPORTUNISTIC:
                return "Android reports opportunistic Private DNS active; this is not an encrypted-only guarantee.";
            case INACTIVE:
                return "Android Private DNS is not active on the current network.";
            default:
                return "Unable to determine Android Private DNS state for the current network.";
        }
    }
}

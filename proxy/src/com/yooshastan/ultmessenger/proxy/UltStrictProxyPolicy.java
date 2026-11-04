package com.yooshastan.ultmessenger.proxy;

/**
 * Local UI policy for the experimental native Telegram socket guard.
 * This validates configuration only; it never proves proxy reachability or routing.
 */
public final class UltStrictProxyPolicy {
    private UltStrictProxyPolicy() { }

    public static boolean canEnable(boolean proxySwitchOn, boolean validSelectedProxy,
                                    boolean torActive) {
        return proxySwitchOn && validSelectedProxy && !torActive;
    }

    /** A strict user choice must not be silently weakened by turning a proxy off. */
    public static boolean mayDisableProxy(boolean strictRequested) {
        return !strictRequested;
    }

    public static String nativeStatus(boolean requested, boolean observed) {
        if (requested != observed) {
            return "TRANSITIONING OR OUT OF SYNC (requested "
                    + (requested ? "ON" : "OFF") + ", native observed "
                    + (observed ? "ON" : "OFF") + ")";
        }
        return observed ? "ON (native observed)" : "OFF (native observed)";
    }
}

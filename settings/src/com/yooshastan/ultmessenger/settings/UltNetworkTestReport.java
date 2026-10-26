package com.yooshastan.ultmessenger.settings;

/**
 * A deliberately local, manually marked connectivity regression checklist.
 * Never logs, scans, resolves, uploads, or asserts that any packet is protected.
 */
public final class UltNetworkTestReport {
    public static final String PREFERENCES = "ult_network_test_report";
    public static final String[] CHECKS = {
        "Messages send/receive with Wonhee connected",
        "Channels and history load",
        "Images, voice and document downloads work",
        "External link open/cancel works",
        "Wi-Fi to cellular reconnection works",
        "Proxy unavailable: behavior observed (not leak proof)",
        "Force-stop/reopen restores connectivity",
        "Ultgram settings persist after restart"
    };

    private UltNetworkTestReport() { }

    public static String key(int index) {
        if (index < 0 || index >= CHECKS.length) {
            throw new IllegalArgumentException("Invalid checklist entry");
        }
        return "check_" + index;
    }

    private static String vpnSummary(String hint) {
        if (hint != null && hint.startsWith("VPN transport reported")) {
            return "Android reports VPN transport; Ultgram route NOT verified";
        }
        if (hint != null && hint.startsWith("VPN transport not reported")) {
            return "Android does not report VPN transport; route NOT verified";
        }
        return "Unknown; route NOT verified";
    }

    public static String render(String vpnHint, boolean strictRequested,
                                boolean strictObserved, int androidSdk, boolean[] userChecks) {
        if (userChecks == null || userChecks.length != CHECKS.length) {
            throw new IllegalArgumentException("Checklist length mismatch");
        }
        StringBuilder out = new StringBuilder("ULTGRAM NETWORK TEST REPORT — USER-REPORTED\n");
        out.append("Android SDK: ").append(Math.max(0, androidSdk)).append('\n');
        out.append("Android VPN hint: ").append(vpnSummary(vpnHint)).append('\n');
        out.append("Native proxy-only requested: ").append(strictRequested ? "ON" : "OFF").append('\n');
        out.append("Native socket guard observed (selected account): ")
                .append(strictObserved ? "ON" : "OFF").append('\n');
        if (strictRequested != strictObserved) {
            out.append("STATE MISMATCH: native transition may be pending; restart and recheck.\n");
        }
        out.append("Checklist (not independently verified):\n");
        for (int i = 0; i < CHECKS.length; i++) {
            out.append(userChecks[i] ? "[marked] " : "[not marked] ")
                    .append(CHECKS[i]).append('\n');
        }
        out.append("NO PER-APP ROUTE, DNS PRIVACY OR FAIL-CLOSED GUARANTEE.\n");
        out.append("No server address, token, username, account ID or traffic payload collected.");
        return out.toString();
    }
}

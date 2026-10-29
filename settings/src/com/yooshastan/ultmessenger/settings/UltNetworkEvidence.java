package com.yooshastan.ultmessenger.settings;

/** Manual capture checklist, NOT an active traffic audit or leak test. */
public final class UltNetworkEvidence {
    public static final String PREFS = "ult_manual_egress_evidence";
    public static final String[] PATHS = {
        "Wi-Fi: native Telegram proxy success/failure capture",
        "Cellular: native Telegram proxy success/failure capture",
        "DNS: Android/Java resolver packet capture",
        "Media: CDN/file transfer packet capture",
        "WebView: link/preview external egress capture",
        "Calls: VoIP/UDP traffic capture"
    };
    private UltNetworkEvidence() {}
    public static String key(int index) {
        if (index < 0 || index >= PATHS.length)
            throw new IllegalArgumentException("path");
        return "evidence_" + index;
    }
    public static String render(boolean[] marked) {
        if (marked == null || marked.length != PATHS.length)
            throw new IllegalArgumentException("coverage length");
        StringBuilder text = new StringBuilder("ULTGRAM MANUAL EGRESS EVIDENCE — USER-MARKED ONLY\n");
        for (int i = 0; i < PATHS.length; i++) {
            text.append(marked[i] ? "[user marked] " : "[not marked] ")
                    .append(PATHS[i]).append('\n');
        }
        text.append("NOT ROUTE-VERIFIED. Checkmarks do not prove fail-closed traffic, ")
                .append("DNS privacy, or a whole-app kill switch.\n")
                .append("No packet data, IP address, username or credentials stored.");
        return text.toString();
    }
}

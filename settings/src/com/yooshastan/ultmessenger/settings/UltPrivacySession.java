package com.yooshastan.ultmessenger.settings;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * In-memory, redacted observation history while Privacy Lab is visible.
 * This records Android/native UI state transitions, never packets or addresses.
 */
public final class UltPrivacySession {
    private static final int MAX_EVENTS = 40;
    private final ArrayList<String> events = new ArrayList<>();
    private String lastFingerprint;
    private int transitions;
    private long startedAtMs;

    public boolean observe(long timestampMs, String network, boolean requested,
                           boolean nativeObserved, int telegramState) {
        String transport = safeTransport(network);
        String fingerprint = transport + "|" + requested + "|" + nativeObserved
                + "|" + telegramState;
        if (fingerprint.equals(lastFingerprint)) return false;
        if (startedAtMs == 0L) startedAtMs = Math.max(1L, timestampMs);
        boolean first = lastFingerprint == null;
        lastFingerprint = fingerprint;
        if (!first) transitions++;
        String line = (first ? "Initial" : "Change #" + transitions)
                + " at +" + Math.max(0L, (timestampMs - startedAtMs) / 1000L) + " s"
                + ": network=" + transport
                + "; strict requested=" + (requested ? "ON" : "OFF")
                + "; native guard=" + (nativeObserved ? "ON" : "OFF")
                + "; Telegram connection state=" + telegramState;
        events.add(line);
        if (events.size() > MAX_EVENTS) events.remove(0);
        return true;
    }

    private static String safeTransport(String transport) {
        if ("Wi-Fi".equals(transport) || "Cellular".equals(transport)
                || "VPN".equals(transport) || "Offline".equals(transport)
                || "Other".equals(transport)) return transport;
        return "Unknown";
    }

    public void clear() {
        events.clear();
        lastFingerprint = null;
        transitions = 0;
        startedAtMs = 0L;
    }

    public int transitions() { return transitions; }
    public List<String> events() {
        return Collections.unmodifiableList(new ArrayList<>(events));
    }

    public String redactedReport() {
        StringBuilder out = new StringBuilder(
                "ULTGRAM PRIVACY LAB — LOCAL OBSERVATIONS\n"
                + "No addresses, message contents, credentials or packet payloads stored.\n"
                + "State changes seen while this screen is open: " + transitions + "\n");
        for (String event : events) out.append(event).append('\n');
        out.append("Native guard state and Android transport are NOT packet-route "
                + "verification. DNS, HTTP, media/CDN, WebView and calls remain "
                + "unverified without independent packet capture.");
        return out.toString();
    }
}

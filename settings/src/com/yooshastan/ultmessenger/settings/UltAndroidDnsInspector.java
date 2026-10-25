package com.yooshastan.ultmessenger.settings;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.LinkProperties;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.Build;

/** Queries platform state only; makes no network or DNS requests. */
public final class UltAndroidDnsInspector {
    private UltAndroidDnsInspector() { }

    /** Read-only Android network hint; not evidence of an app's actual route. */
    public static String inspectVpn(Context context) {
        try {
            ConnectivityManager manager = (ConnectivityManager) context.getSystemService(
                    Context.CONNECTIVITY_SERVICE);
            if (manager == null) {
                return "Unknown (network service unavailable)";
            }
            Network network = manager.getActiveNetwork();
            NetworkCapabilities capabilities = network == null ? null
                    : manager.getNetworkCapabilities(network);
            if (capabilities == null) {
                return "Unknown (active network unavailable)";
            }
            if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN)) {
                return "VPN transport reported; Ultgram routing NOT verified";
            }
            return "VPN transport not reported; Ultgram routing NOT verified";
        } catch (RuntimeException inaccessible) {
            return "Unknown (Android did not expose VPN state)";
        }
    }

    public static String inspect(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            return "Android Private DNS network status requires Android 9 or newer.";
        }
        try {
            ConnectivityManager manager = (ConnectivityManager) context.getSystemService(
                    Context.CONNECTIVITY_SERVICE);
            if (manager == null) {
                return UltDnsPosture.explain(UltDnsPosture.Mode.UNKNOWN);
            }
            Network network = manager.getActiveNetwork();
            LinkProperties link = network == null ? null : manager.getLinkProperties(network);
            if (link == null) {
                return UltDnsPosture.explain(UltDnsPosture.Mode.UNKNOWN);
            }
            String provider = link.getPrivateDnsServerName();
            UltDnsPosture.Mode mode = UltDnsPosture.classify(
                    true, link.isPrivateDnsActive(), provider);
            String summary = UltDnsPosture.explain(mode);
            if (mode == UltDnsPosture.Mode.STRICT_DOT) {
                return summary + "\nConfigured resolver: " + provider;
            }
            return summary;
        } catch (RuntimeException error) {
            // Android/OEM/VPN state can restrict link properties. Never infer that
            // DNS is encrypted from an unavailable status API.
            return UltDnsPosture.explain(UltDnsPosture.Mode.UNKNOWN);
        }
    }
}

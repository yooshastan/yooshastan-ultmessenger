package com.yooshastan.ultmessenger.proxy;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;

/**
 * Pure-Java helper for opt-in local proxy aliases.
 *
 * Aliases are keyed only to transport type, server and port, never the
 * SOCKS5 password or MTProto secret. Two credentials for the same public
 * endpoint intentionally share one alias. The alias itself stays on-device.
 */
public final class UltProxyLabels {
    public static final String PREFERENCES = "ult_proxy_labels";

    private UltProxyLabels() { }

    public static String normalizeLabel(String value) {
        if (value == null) {
            throw new IllegalArgumentException("A profile name is required");
        }
        String label = value.trim();
        if (label.length() > 64) {
            throw new IllegalArgumentException("Profile name must be at most 64 characters");
        }
        for (int i = 0; i < label.length(); i++) {
            if (Character.isISOControl(label.charAt(i))) {
                throw new IllegalArgumentException("Control characters are not valid in profile names");
            }
        }
        return label; // An empty value clears an existing alias.
    }

    public static String preferenceKey(String type, String address, int port) {
        if (type == null || type.isEmpty() || address == null || address.trim().isEmpty()
                || port < 0 || port > 65535) {
            throw new IllegalArgumentException("Invalid proxy endpoint");
        }
        String descriptor = type.toUpperCase(Locale.ROOT) + "\u0000"
                + address.trim().toLowerCase(Locale.ROOT) + "\u0000" + port;
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256")
                    .digest(descriptor.getBytes(StandardCharsets.UTF_8));
            char[] hex = "0123456789abcdef".toCharArray();
            StringBuilder key = new StringBuilder("label_");
            for (byte b : hash) {
                key.append(hex[(b >> 4) & 0x0f]).append(hex[b & 0x0f]);
            }
            return key.toString();
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 unavailable", impossible);
        }
    }

    public static String displayName(String savedLabel, String nativeEndpoint) {
        if (savedLabel == null || savedLabel.isEmpty()) {
            return nativeEndpoint;
        }
        return savedLabel + " · " + nativeEndpoint;
    }
}

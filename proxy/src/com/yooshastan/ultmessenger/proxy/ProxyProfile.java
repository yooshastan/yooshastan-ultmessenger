package com.yooshastan.ultmessenger.proxy;

import java.util.Locale;

/**
 * Validated local proxy profile metadata.
 * Credentials/secrets must be stored separately in encrypted Android storage.
 * Creating a profile does NOT route traffic or conceal a Telegram account.
 */
public final class ProxyProfile {
    public enum Protocol { SOCKS5, MTPROTO }

    public final String name;
    public final Protocol protocol;
    public final String host;
    public final int port;
    public final boolean enabled;

    public ProxyProfile(String name, Protocol protocol, String host, int port, boolean enabled) {
        if (name == null || name.trim().isEmpty() || name.trim().length() > 64) {
            throw new IllegalArgumentException("Profile name must be 1–64 characters");
        }
        if (protocol == null) {
            throw new IllegalArgumentException("Proxy protocol required");
        }
        if (host == null || host.trim().isEmpty()) {
            throw new IllegalArgumentException("Proxy hostname required");
        }
        String normalized = host.trim().toLowerCase(Locale.ROOT);
        if (normalized.length() > 253 || normalized.contains("://")
                || normalized.matches(".*[\\s/@?#\\\\].*")) {
            throw new IllegalArgumentException("Use a hostname or IP address, not a URL");
        }
        if (port < 1 || port > 65535) {
            throw new IllegalArgumentException("Proxy port must be between 1 and 65535");
        }
        this.name = name.trim();
        this.protocol = protocol;
        this.host = normalized;
        this.port = port;
        this.enabled = enabled;
    }

    public String displayEndpoint() {
        return host + ":" + port;
    }
}

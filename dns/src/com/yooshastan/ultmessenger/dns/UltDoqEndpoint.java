package com.yooshastan.ultmessenger.dns;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;

/**
 * A DNS-over-QUIC server name, not a socket or a system DNS override.
 * QUIC's default DoQ port is 853 (RFC 9250).
 */
public final class UltDoqEndpoint {
    public static final int DEFAULT_PORT = 853;

    private final String hostname;
    private final int port;

    private UltDoqEndpoint(String hostname, int port) {
        this.hostname = hostname;
        this.port = port;
    }

    public static UltDoqEndpoint parse(String value) {
        if (value == null || !value.equals(value.trim())) {
            throw new IllegalArgumentException("DoQ endpoint must not be empty or padded");
        }
        final URI uri;
        try {
            uri = new URI(value);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Malformed DoQ endpoint", e);
        }
        if (!"quic".equalsIgnoreCase(uri.getScheme())
                || uri.getRawUserInfo() != null
                || uri.getRawPath() != null && !uri.getRawPath().isEmpty()
                || uri.getRawQuery() != null || uri.getRawFragment() != null
                || uri.getHost() == null) {
            throw new IllegalArgumentException("Expected quic://dns.example.org[:port]");
        }
        String host = uri.getHost().toLowerCase(Locale.ROOT);
        if (host.length() > 253 || host.indexOf('.') < 1 || host.endsWith(".")
                || host.equals("localhost") || host.matches("[0-9.]+")
                || !host.matches("[a-z0-9.-]+")) {
            throw new IllegalArgumentException("DoQ endpoint needs a DNS hostname");
        }
        for (String label : host.split("\\.", -1)) {
            if (label.length() == 0 || label.length() > 63
                    || !Character.isLetterOrDigit(label.charAt(0))
                    || !Character.isLetterOrDigit(label.charAt(label.length() - 1))) {
                throw new IllegalArgumentException("Invalid DoQ DNS hostname label");
            }
        }
        int port = uri.getPort();
        // Reject nonnumeric/missing explicit port, which URI may report as -1.
        String authority = uri.getRawAuthority();
        if (authority == null || (!authority.equalsIgnoreCase(host)
                && !authority.equalsIgnoreCase(host + ":" + port))) {
            throw new IllegalArgumentException("Invalid DoQ authority or port");
        }
        if (port == -1) {
            port = DEFAULT_PORT;
        }
        if (port < 1 || port > 65535) {
            throw new IllegalArgumentException("Invalid DoQ port");
        }
        return new UltDoqEndpoint(host, port);
    }

    public String hostname() {
        return hostname;
    }

    public int port() {
        return port;
    }

    public String asUri() {
        return "quic://" + hostname + ":" + port;
    }
}

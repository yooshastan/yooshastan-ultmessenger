package com.yooshastan.ultmessenger.dns;

import java.io.IOException;
import java.util.Arrays;

/**
 * DoQ wire-message contract. It does not implement QUIC or intercept Android DNS.
 * Callers must supply a certificate-verifying QUIC backend; no plaintext fallback exists.
 */
public final class UltDoqResolver {
    public interface Backend {
        byte[] exchange(UltDoqEndpoint server, byte[] dnsQuery) throws IOException;
    }

    private final UltDoqEndpoint server;
    private final Backend backend;
    private final UltDnsTrackerPolicy trackerPolicy;

    public UltDoqResolver(UltDoqEndpoint server, Backend backend) {
        this(server, backend, null);
    }

    public UltDoqResolver(UltDoqEndpoint server, Backend backend,
                          UltDnsTrackerPolicy trackerPolicy) {
        if (server == null) {
            throw new IllegalArgumentException("DoQ server must be configured");
        }
        this.server = server;
        this.backend = backend;
        this.trackerPolicy = trackerPolicy;
    }

    public byte[] exchange(byte[] dnsQuery) throws IOException {
        validateQuery(dnsQuery);
        // Opt-in filter is local. Never send a blocked hostname to the backend.
        if (trackerPolicy != null && trackerPolicy.blocksQuestion(dnsQuery)) {
            return trackerPolicy.nxdomainResponse(dnsQuery);
        }
        if (backend == null) {
            throw new IOException("DoQ QUIC backend is not installed; DNS query not sent");
        }
        byte[] response = backend.exchange(server, Arrays.copyOf(dnsQuery, dnsQuery.length));
        validateResponse(response);
        return Arrays.copyOf(response, response.length);
    }

    private static void validateQuery(byte[] message) throws IOException {
        if (message == null || message.length < 17 || message.length > 65535) {
            throw new IOException("Invalid DNS query length");
        }
        // RFC 9250: the DNS message ID in DoQ is zero; one question per query.
        if (message[0] != 0 || message[1] != 0 || (message[2] & 0x80) != 0
                || message[4] != 0 || message[5] != 1) {
            throw new IOException("Invalid DoQ DNS query header");
        }
        int cursor = 12;
        while (true) {
            if (cursor >= message.length) {
                throw new IOException("Truncated DNS name");
            }
            int labelLength = message[cursor++] & 0xff;
            if (labelLength == 0) {
                break;
            }
            // Don't accept compressed or reserved label forms in outgoing questions.
            if (labelLength > 63 || cursor + labelLength > message.length) {
                throw new IOException("Invalid DNS question name");
            }
            cursor += labelLength;
        }
        if (cursor + 4 > message.length) {
            throw new IOException("Missing DNS question type/class");
        }
    }

    private static void validateResponse(byte[] response) throws IOException {
        if (response == null || response.length < 12 || response.length > 65535
                || response[0] != 0 || response[1] != 0 || (response[2] & 0x80) == 0) {
            throw new IOException("Invalid DoQ DNS response header");
        }
    }
}

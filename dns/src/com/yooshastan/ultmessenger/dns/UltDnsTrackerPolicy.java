package com.yooshastan.ultmessenger.dns;

import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/** Optional filtering of *only* DNS questions explicitly routed through this policy. */
public final class UltDnsTrackerPolicy {
    private final Set<String> blocked;
    private final Set<String> allowed;

    public UltDnsTrackerPolicy(Set<String> blocked, Set<String> allowed) {
        this.blocked = normalizeRules(blocked);
        this.allowed = normalizeRules(allowed);
    }

    public boolean blocks(String hostname) {
        final String name = normalizeDomain(hostname);
        if (matches(allowed, name)) {
            return false;
        }
        return matches(blocked, name);
    }

    public boolean blocksQuestion(byte[] query) throws IOException {
        return blocks(readQuestionHostname(query));
    }

    /** Return a valid synthetic NXDOMAIN response for a blocked one-question DNS query. */
    public byte[] nxdomainResponse(byte[] query) throws IOException {
        int questionEnd = questionEnd(query);
        byte[] answer = new byte[questionEnd];
        System.arraycopy(query, 0, answer, 0, questionEnd);
        // Zero-ID is required by DoQ. Set QR, RA, keep original RD, return NXDOMAIN.
        answer[0] = 0;
        answer[1] = 0;
        answer[2] = (byte) (0x80 | (query[2] & 0x01));
        answer[3] = (byte) 0x83;
        for (int i = 6; i < 12; i++) {
            answer[i] = 0;
        }
        return answer;
    }

    public static String normalizeDomain(String input) {
        if (input == null) {
            throw new IllegalArgumentException("Domain is required");
        }
        String domain = input.trim().toLowerCase(Locale.ROOT);
        if (domain.endsWith(".")) {
            domain = domain.substring(0, domain.length() - 1);
        }
        if (domain.length() < 3 || domain.length() > 253 || !domain.contains(".")
                || !domain.matches("[a-z0-9.-]+")) {
            throw new IllegalArgumentException("Invalid DNS filtering domain");
        }
        for (String label : domain.split("\\.", -1)) {
            if (label.isEmpty() || label.length() > 63 || label.startsWith("-")
                    || label.endsWith("-")) {
                throw new IllegalArgumentException("Invalid DNS filtering label");
            }
        }
        return domain;
    }

    private static Set<String> normalizeRules(Set<String> rules) {
        if (rules == null || rules.size() > 4096) {
            throw new IllegalArgumentException("Invalid DNS rule list");
        }
        Set<String> normalized = new HashSet<>();
        for (String rule : rules) {
            normalized.add(normalizeDomain(rule));
        }
        return Collections.unmodifiableSet(normalized);
    }

    private static boolean matches(Set<String> rules, String name) {
        if (rules.contains(name)) {
            return true;
        }
        int index = name.indexOf('.');
        while (index != -1) {
            if (rules.contains(name.substring(index + 1))) {
                return true;
            }
            index = name.indexOf('.', index + 1);
        }
        return false;
    }

    private static String readQuestionHostname(byte[] query) throws IOException {
        StringBuilder domain = new StringBuilder();
        int pos = 12;
        if (query == null || query.length < 17 || query[0] != 0 || query[1] != 0
                || (query[2] & 0x80) != 0 || query[4] != 0 || query[5] != 1) {
            throw new IOException("Invalid DNS question header");
        }
        while (pos < query.length) {
            int length = query[pos++] & 0xff;
            if (length == 0) {
                if (domain.length() == 0 || pos + 4 > query.length) {
                    throw new IOException("Truncated DNS question");
                }
                return domain.toString();
            }
            if (length > 63 || pos + length > query.length || domain.length() + length > 252) {
                throw new IOException("Invalid DNS label");
            }
            if (domain.length() > 0) {
                domain.append('.');
            }
            for (int i = 0; i < length; i++) {
                int value = query[pos++] & 0xff;
                if (!(value >= 'a' && value <= 'z') && !(value >= 'A' && value <= 'Z')
                        && !(value >= '0' && value <= '9') && value != '-') {
                    throw new IOException("Unsupported non-ASCII DNS label");
                }
                domain.append((char) value);
            }
        }
        throw new IOException("Unterminated DNS question");
    }

    private static int questionEnd(byte[] query) throws IOException {
        readQuestionHostname(query);
        int cursor = 12;
        while (cursor < query.length) {
            int length = query[cursor++] & 0xff;
            if (length == 0) {
                return cursor + 4;
            }
            cursor += length;
        }
        throw new IOException("Unterminated DNS question");
    }
}

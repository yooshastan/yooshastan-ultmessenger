package com.yooshastan.ultmessenger.dns;

/** Literal bootstrap IPv4 address parser. Does not resolve any hostname. */
public final class UltDoqBootstrap {
    private UltDoqBootstrap() {}

    public static byte[] ipv4(String input) {
        if (input == null || !input.equals(input.trim())) {
            throw new IllegalArgumentException("Bootstrap must be an IPv4 literal");
        }
        String[] parts = input.split("\\.", -1);
        if (parts.length != 4) {
            throw new IllegalArgumentException("Bootstrap must be an IPv4 literal");
        }
        byte[] result = new byte[4];
        for (int i = 0; i < parts.length; i++) {
            String part = parts[i];
            if (part.isEmpty() || part.length() > 3
                    || part.length() > 1 && part.charAt(0) == '0'
                    || !part.matches("[0-9]+")) {
                throw new IllegalArgumentException("Invalid bootstrap IPv4 octet");
            }
            int value = Integer.parseInt(part);
            if (value > 255) {
                throw new IllegalArgumentException("Invalid bootstrap IPv4 octet");
            }
            result[i] = (byte) value;
        }
        if ((result[0] & 0xff) == 0 || (result[0] & 0xff) >= 224
                || input.equals("255.255.255.255")) {
            throw new IllegalArgumentException("Bootstrap address must be unicast IPv4");
        }
        return result;
    }
}

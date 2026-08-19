package com.simplehearing.common.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;

/**
 * Single-use link tokens (invitations, password resets) are handed out in the clear
 * but only ever stored as a SHA-256 hash, so a database leak yields nothing usable.
 */
public final class TokenHasher {

    private TokenHasher() {}

    /** Generates a raw token to embed in an emailed link. Never persisted as-is. */
    public static String generateRawToken() {
        return UUID.randomUUID() + "-" + UUID.randomUUID();
    }

    public static String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                String h = Integer.toHexString(0xff & b);
                if (h.length() == 1) hex.append('0');
                hex.append(h);
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}

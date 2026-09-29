package com.simplehearing.common.util;

/**
 * A phone number is an identity here too, once used to log in — same reasoning as
 * {@link EmailNormalizer}. Stored and compared in one canonical form so "+91 98765 43210",
 * "+91-98765-43210" and "9876543210" don't quietly create separate-looking rows: strip
 * everything but digits and a leading '+'. Blank input normalises to null, so clearing the
 * field doesn't leave an empty string that would collide with every other cleared phone under
 * the unique index.
 *
 * <p>This does not validate that the result is a real, dialable number (no country-code
 * assumption, no length check) — only that two equivalent-looking inputs compare equal.
 */
public final class PhoneNormalizer {

    private PhoneNormalizer() {}

    public static String normalize(String phone) {
        if (phone == null) return null;
        String trimmed = phone.trim();
        if (trimmed.isEmpty()) return null;
        String kept = trimmed.replaceAll("[^0-9+]", "");
        return kept.isEmpty() ? null : kept;
    }

    /** True when the identifier looks like an email rather than a phone number — the login split. */
    public static boolean looksLikeEmail(String identifier) {
        return identifier != null && identifier.contains("@");
    }
}

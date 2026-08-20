package com.simplehearing.common.util;

import java.util.Locale;

/**
 * An email address is an identity here — it is how a user is looked up at login, how an invitation
 * is matched to an account, and what the unique constraint is built on. It is therefore stored and
 * compared in one canonical form: trimmed and lower-cased.
 *
 * <p>Normalise on the way in (request DTOs) and again before any repository lookup, so a value read
 * back out of the database is treated the same way as one typed into a form.
 */
public final class EmailNormalizer {

    private EmailNormalizer() {}

    /** Trims surrounding whitespace and lower-cases. Null in, null out. */
    public static String normalize(String email) {
        return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }
}

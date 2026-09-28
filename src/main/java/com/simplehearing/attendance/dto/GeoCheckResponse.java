package com.simplehearing.attendance.dto;

/**
 * A live "how far am I from where I'm supposed to check in" preview, computed the moment the
 * browser reports a location — before the caller actually submits a check-in/verify. Mirrors
 * the same reference (clinic vs. organisation) and geo-fence radius that {@code AttendanceService}
 * will use when the real check-in/verify request comes in, so the preview never disagrees with
 * the outcome.
 */
public record GeoCheckResponse(
        boolean verified,
        /** Null when the reference location has no latitude/longitude configured — nothing to compare against. */
        Double distanceMeters,
        Integer radiusMeters,
        String referenceLabel,
        GeoCheckReferenceType referenceType
) {
    public enum GeoCheckReferenceType { CLINIC, ORGANISATION }
}

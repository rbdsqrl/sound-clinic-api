package com.simplehearing.common.tenant;

import java.util.UUID;

/**
 * Thread-local holder for the current request's tenant context.
 * Set by {@link com.simplehearing.auth.security.JwtAuthFilter} after JWT validation
 * and cleared in the same filter's finally block.
 */
public final class TenantContext {

    private TenantContext() {}

    public record TenantData(UUID orgId, UUID clinicId, UUID userId, String role) {}

    private static final ThreadLocal<TenantData> CONTEXT = new ThreadLocal<>();

    public static void set(UUID orgId, UUID clinicId, UUID userId, String role) {
        CONTEXT.set(new TenantData(orgId, clinicId, userId, role));
    }

    public static UUID getOrgId() {
        TenantData data = CONTEXT.get();
        return data != null ? data.orgId() : null;
    }

    public static UUID getClinicId() {
        TenantData data = CONTEXT.get();
        return data != null ? data.clinicId() : null;
    }

    public static UUID getUserId() {
        TenantData data = CONTEXT.get();
        return data != null ? data.userId() : null;
    }

    public static String getRole() {
        TenantData data = CONTEXT.get();
        return data != null ? data.role() : null;
    }

    public static void clear() {
        CONTEXT.remove();
    }
}

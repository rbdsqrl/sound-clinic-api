package com.simplehearing.clinic.dto;

import com.simplehearing.clinic.entity.Clinic;

import java.time.Instant;
import java.util.UUID;

public record ClinicResponse(
        UUID id,
        UUID orgId,
        String name,
        String address,
        String phone,
        String email,
        String timezone,
        Double latitude,
        Double longitude,
        Integer geoFenceRadiusMeters,
        boolean isActive,
        Instant createdAt
) {
    public static ClinicResponse from(Clinic clinic) {
        return new ClinicResponse(
                clinic.getId(),
                clinic.getOrgId(),
                clinic.getName(),
                clinic.getAddress(),
                clinic.getPhone(),
                clinic.getEmail(),
                clinic.getTimezone(),
                clinic.getLatitude(),
                clinic.getLongitude(),
                clinic.getGeoFenceRadiusMeters(),
                clinic.isActive(),
                clinic.getCreatedAt()
        );
    }
}

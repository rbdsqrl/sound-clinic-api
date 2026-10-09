package com.simplehearing.attendance.dto;

import java.util.List;
import java.util.UUID;

public record CheckInRequest(
        /** Required unless {@code atOrganisation}. */
        UUID clinicId,
        /** Check in at the organisation's own location instead of a clinic — Business Owner only. */
        boolean atOrganisation,

        Double latitude,
        Double longitude,
        List<Double> faceDescriptor,
        boolean forceCheckIn
) {}

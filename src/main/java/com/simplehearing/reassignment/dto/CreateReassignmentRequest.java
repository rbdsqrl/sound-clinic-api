package com.simplehearing.reassignment.dto;

import com.simplehearing.reassignment.enums.ReassignmentType;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Bulk-reassigns selected cases from one therapist to another, permanently or for a window. */
public record CreateReassignmentRequest(
        @NotNull UUID fromTherapistId,
        @NotNull UUID toTherapistId,
        @NotEmpty List<UUID> patientIds,
        @NotNull ReassignmentType type,
        /** Defaults to today when omitted. */
        LocalDate startDate,
        /** Required when type is TEMPORARY; must be null for PERMANENT. */
        LocalDate endDate,
        String reason
) {}

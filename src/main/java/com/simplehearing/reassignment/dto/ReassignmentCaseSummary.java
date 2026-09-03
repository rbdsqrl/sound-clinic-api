package com.simplehearing.reassignment.dto;

import java.util.UUID;

public record ReassignmentCaseSummary(
        UUID patientId,
        String patientName,
        UUID enrollmentId
) {}

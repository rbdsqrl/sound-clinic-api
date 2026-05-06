package com.simplehearing.session.dto;

import com.simplehearing.session.enums.TherapySessionStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateSessionStatusRequest(
        @NotNull TherapySessionStatus status,
        String notes
) {}

package com.simplehearing.iep.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record AddProgressRequest(
        @NotNull LocalDate sessionDate,
        String note,
        Integer trialsPassed,
        Integer trialsTotal
) {}

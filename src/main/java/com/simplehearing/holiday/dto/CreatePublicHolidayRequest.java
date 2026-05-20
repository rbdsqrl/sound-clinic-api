package com.simplehearing.holiday.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record CreatePublicHolidayRequest(
        @NotNull LocalDate holidayDate,
        @NotBlank String name
) {}

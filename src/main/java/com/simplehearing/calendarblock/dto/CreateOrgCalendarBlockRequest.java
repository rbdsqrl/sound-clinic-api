package com.simplehearing.calendarblock.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Set;

public record CreateOrgCalendarBlockRequest(
        @NotBlank String title,
        @NotNull LocalTime startTime,
        @NotNull LocalTime endTime,
        @NotEmpty Set<DayOfWeek> daysOfWeek,
        @NotNull LocalDate startDate,
        LocalDate endDate  // null = ongoing indefinitely
) {}

package com.simplehearing.attendance.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;

/**
 * The corrected login and logout time of an attendance record — the Business Owner only. The working hours follow from
 * the two times. A null {@code checkOutTime} means the employee has not logged out (the record goes back to checked in).
 */
public record EditAttendanceTimesRequest(
        @NotNull Instant checkInTime,
        Instant checkOutTime,
        @Size(max = 500) String note
) {}

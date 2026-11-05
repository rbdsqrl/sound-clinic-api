package com.simplehearing.session.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Moves a session that was marked completed by mistake to a later date: it is cancelled and a fresh session is created
 * for the new date on the same plan.
 *
 * @param newDate      the day for the new session — after the original session's day, and not in the past
 * @param newStartTime the time for the new session, or null to keep the original time (the length is kept either way)
 * @param reason       why it is being moved, kept in the session's activity log; optional
 */
public record MoveSessionRequest(
        @NotNull LocalDate newDate,
        LocalTime newStartTime,
        @Size(max = 500) String reason
) {}

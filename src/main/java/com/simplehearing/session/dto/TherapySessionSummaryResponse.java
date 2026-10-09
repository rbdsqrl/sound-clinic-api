package com.simplehearing.session.dto;

import com.simplehearing.session.enums.TherapySessionStatus;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

/**
 * Just what a session row needs to be drawn in a list (dashboard card, sidebar badge) — none of
 * the notes/feedback text, reschedule allowance or plan totals. Fetch the full session with
 * {@code GET /therapy-sessions/{id}} when one is opened.
 */
public record TherapySessionSummaryResponse(
        UUID id,
        UUID enrollmentId,
        UUID patientId,
        String patientFirstName,
        String patientLastName,
        UUID therapistId,
        String therapistFirstName,
        String therapistLastName,
        String programName,
        LocalDate sessionDate,
        LocalTime startTime,
        LocalTime endTime,
        TherapySessionStatus status
) {}

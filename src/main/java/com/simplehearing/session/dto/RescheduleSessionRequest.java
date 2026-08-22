package com.simplehearing.session.dto;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

/**
 * @param newDate      the day to move to, or null to keep the current day
 * @param newStartTime the time to move to, or null to keep the current time. The session
 *                     keeps its original length — the end time shifts with the start.
 * @param substituteTherapistId hand the session to a different therapist, or null to keep the current one
 * @param reason       shown to the family in the notification email; optional
 */
public record RescheduleSessionRequest(
        LocalDate newDate,
        LocalTime newStartTime,
        UUID substituteTherapistId,
        String reason
) {}

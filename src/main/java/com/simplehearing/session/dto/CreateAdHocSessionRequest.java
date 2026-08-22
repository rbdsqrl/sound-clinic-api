package com.simplehearing.session.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

/**
 * A one-off therapy session booked from the calendar, outside the block generated
 * when the plan was set up.
 *
 * @param countsTowardPlan true to consume one of the sessions the family paid for,
 *                         false to add it on top as an extra
 */
public record CreateAdHocSessionRequest(
        @NotNull UUID enrollmentId,
        @NotNull LocalDate sessionDate,
        @NotNull LocalTime startTime,
        @NotNull LocalTime endTime,
        /** Defaults to the plan's therapist when omitted. */
        UUID therapistId,
        boolean countsTowardPlan,
        /**
         * Whether the family is charged for this session. Ignored when countsTowardPlan
         * is true — that session is already covered by what they paid.
         */
        boolean requiresPayment,
        String notes
) {}

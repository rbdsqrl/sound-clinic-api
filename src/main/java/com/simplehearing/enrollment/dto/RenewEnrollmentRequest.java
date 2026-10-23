package com.simplehearing.enrollment.dto;

import com.simplehearing.subscription.dto.AddPaymentRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import java.math.BigDecimal;
import java.time.LocalDate;

public record RenewEnrollmentRequest(
        /** How many sessions to add on top of the plan's current total. */
        @Min(1) @Max(200) int additionalSessions,
        /** First date the new sessions may land on. Omit to continue straight after the plan's last
         *  session (or today, if that has already passed). Can't be in the past. */
        LocalDate startDate,
        /** Optional — the total for the whole plan after the renewal, to give an ad hoc discount. Omit to add the
         *  new sessions at the plan's usual rate (on top of whatever total is already agreed). */
        @DecimalMin("0") BigDecimal planTotal,
        /** Optional — a payment taken now for the added sessions, added to what has already been paid. */
        @Valid AddPaymentRequest payment
) {}

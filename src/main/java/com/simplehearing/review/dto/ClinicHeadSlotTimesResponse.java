package com.simplehearing.review.dto;

import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

/**
 * A Clinic Head's effective Review Session slot grid — {@code times} is their own override if
 * they have one, otherwise the org-wide default (see Organisation.reviewSlotTimes).
 * {@code usingOrgDefault} tells the Organisation settings UI whether this is inherited or a
 * personal override, so it can label the picker accordingly and offer "reset to org default".
 */
public record ClinicHeadSlotTimesResponse(
        UUID clinicHeadId,
        List<LocalTime> times,
        boolean usingOrgDefault
) {}

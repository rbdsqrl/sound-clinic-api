package com.simplehearing.review.dto;

import java.time.LocalTime;
import java.util.List;

/**
 * One of the org's fixed daily Review Session grid times, for a specific date and the given
 * Clinic Head(s) — {@code available} is false if ANY of them already has an overlapping
 * SCHEDULED review meeting spanning this slot's start time. {@code busyClinicHeadNames} names
 * which ones, for the picker to explain why a slot is greyed out.
 */
public record ReviewSlotResponse(
        LocalTime time,
        boolean available,
        List<String> busyClinicHeadNames
) {}

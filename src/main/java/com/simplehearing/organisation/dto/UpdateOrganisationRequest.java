package com.simplehearing.organisation.dto;

import com.simplehearing.organisation.enums.AiProvider;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Set;

public record UpdateOrganisationRequest(
        String name,
        String contactEmail,
        String contactPhone,
        String address,
        String logoUrl,
        String timezone,
        AiProvider aiProvider,
        /** Write-only. Omit to leave the stored key unchanged; pass an empty string to clear it. */
        String aiApiKey,
        /** Omit to leave unchanged; pass an empty set to clear all weekly off days. */
        Set<DayOfWeek> weeklyOffDays,
        Double latitude,
        Double longitude,
        Integer geoFenceRadiusMeters,
        /** Full replacement of the Review Session daily grid. Omit to leave unchanged; must not be empty. */
        Set<LocalTime> reviewSlotTimes
) {}

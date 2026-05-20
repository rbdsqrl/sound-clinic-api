package com.simplehearing.holiday.dto;

import com.simplehearing.holiday.entity.PublicHoliday;

import java.time.LocalDate;
import java.util.UUID;

public record PublicHolidayResponse(
        UUID id,
        UUID orgId,
        LocalDate holidayDate,
        String name,
        int sessionsAffected
) {
    public static PublicHolidayResponse from(PublicHoliday h, int sessionsAffected) {
        return new PublicHolidayResponse(h.getId(), h.getOrgId(), h.getHolidayDate(), h.getName(), sessionsAffected);
    }
}

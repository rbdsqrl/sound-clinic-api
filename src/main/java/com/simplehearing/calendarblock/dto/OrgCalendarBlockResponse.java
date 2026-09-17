package com.simplehearing.calendarblock.dto;

import com.simplehearing.calendarblock.entity.OrgCalendarBlock;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Set;
import java.util.UUID;

public record OrgCalendarBlockResponse(
        UUID id,
        String title,
        LocalTime startTime,
        LocalTime endTime,
        Set<DayOfWeek> daysOfWeek,
        LocalDate startDate,
        LocalDate endDate
) {
    public static OrgCalendarBlockResponse from(OrgCalendarBlock b) {
        return new OrgCalendarBlockResponse(
                b.getId(), b.getTitle(), b.getStartTime(), b.getEndTime(),
                b.getDaysOfWeek(), b.getStartDate(), b.getEndDate());
    }
}

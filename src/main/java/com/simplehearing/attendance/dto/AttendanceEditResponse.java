package com.simplehearing.attendance.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** One entry of a record's edit history. */
public record AttendanceEditResponse(
        UUID id,
        UUID editedById,
        String editedByName,
        Instant editedAt,
        String note,
        List<FieldChange> changes
) {
    public record FieldChange(String field, String from, String to) {}
}

package com.simplehearing.baseline.dto;

import com.simplehearing.baseline.entity.BaselineProgressEntry;

import java.time.Instant;
import java.util.UUID;

public record BaselineProgressEntryResponse(
        UUID id,
        String entryDate,
        String value,
        /** Optional 0-100 score alongside {@code value}. Null when this entry wasn't scored. */
        Integer scorePercent,
        String loggedByName,
        Instant createdAt
) {
    public static BaselineProgressEntryResponse from(BaselineProgressEntry entry, String loggedByName) {
        return new BaselineProgressEntryResponse(
                entry.getId(),
                entry.getEntryDate().toString(),
                entry.getValue(),
                entry.getScorePercent(),
                loggedByName,
                entry.getCreatedAt()
        );
    }
}

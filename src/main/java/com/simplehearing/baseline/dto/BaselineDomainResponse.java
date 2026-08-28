package com.simplehearing.baseline.dto;

import com.simplehearing.baseline.enums.BaselineDomain;

import java.time.Instant;
import java.util.List;

public record BaselineDomainResponse(
        BaselineDomain domain,
        String baselineValue,
        /** Optional 0-100 score alongside {@code baselineValue}. Null when not scored. */
        Integer baselineScorePercent,
        Instant baselineUpdatedAt,
        List<BaselineProgressEntryResponse> currentEntries
) {
}

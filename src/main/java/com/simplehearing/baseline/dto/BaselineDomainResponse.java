package com.simplehearing.baseline.dto;

import com.simplehearing.baseline.enums.BaselineDomain;

import java.time.Instant;
import java.util.List;

public record BaselineDomainResponse(
        BaselineDomain domain,
        String baselineValue,
        Instant baselineUpdatedAt,
        List<BaselineProgressEntryResponse> currentEntries
) {
}

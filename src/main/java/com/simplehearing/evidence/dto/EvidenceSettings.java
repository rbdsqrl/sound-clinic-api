package com.simplehearing.evidence.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/**
 * An organisation's goal video evidence rules.
 *
 * @param videosRequired   videos needed before a goal can be completed; 0 makes evidence optional
 * @param maxVideoMb       largest single video accepted (the server's hard upload ceiling is 100 MB)
 * @param maxVideoSeconds  longest single video accepted — enforced when the app reports the duration
 */
public record EvidenceSettings(
        @Min(0) @Max(10) int videosRequired,
        @Min(1) @Max(100) int maxVideoMb,
        @Min(5) @Max(900) int maxVideoSeconds
) {}

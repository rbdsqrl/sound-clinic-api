package com.simplehearing.baseline.dto;

import com.simplehearing.baseline.enums.BaselineDomain;

import java.util.Map;

/** Every field is optional — staff can fill what they know and complete the rest later.
 *  {@code domainScores} is a parallel, optional 0-100 score per domain — not every domain
 *  needs one, only those a clinician wants to be able to chart. */
public record CreateBaselineReportRequest(
        String ageAtAdmission,
        String ageOnDate,
        String cdct,
        Map<BaselineDomain, String> domainValues,
        Map<BaselineDomain, Integer> domainScores
) {}

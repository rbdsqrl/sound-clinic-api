package com.simplehearing.baseline.dto;

import com.simplehearing.baseline.enums.BaselineDomain;

import java.util.Map;

/** Every field is optional — staff can fill what they know and complete the rest later. */
public record CreateBaselineReportRequest(
        String ageAtAdmission,
        String ageOnDate,
        String cdct,
        Map<BaselineDomain, String> domainValues
) {}

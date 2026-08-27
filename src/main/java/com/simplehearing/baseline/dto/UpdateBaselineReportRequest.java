package com.simplehearing.baseline.dto;

import com.simplehearing.baseline.enums.BaselineDomain;

import java.util.Map;

/** Partial update — null header fields are left unchanged; only domains present in
 *  {@code domainValues} have their baseline text touched. */
public record UpdateBaselineReportRequest(
        String ageAtAdmission,
        String ageOnDate,
        String cdct,
        Map<BaselineDomain, String> domainValues
) {}

package com.simplehearing.inquiry.dto;

import java.util.Map;

public record InquiryAnalyticsResponse(
        int totalCount,
        int convertedCount,
        double conversionRate,          // percentage, e.g. 23.5
        Double avgResponseTimeHours,    // null if no activity logs yet
        int overdueCount,               // NEW inquiries older than 24 hrs
        int readyToConvertCount,        // VISITED inquiries
        Map<String, Integer> countByStatus
) {}

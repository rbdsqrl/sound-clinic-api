package com.simplehearing.inquiry.dto;

import java.util.List;
import java.util.Map;

public record InquiryAnalyticsResponse(
        int totalCount,
        int convertedCount,
        double conversionRate,          // percentage, e.g. 23.5
        Double avgResponseTimeHours,    // null if no activity logs yet
        int overdueCount,               // NEW inquiries older than 24 hrs
        int readyToConvertCount,        // VISITED inquiries
        Map<String, Integer> countByStatus,
        /** How they reached the clinic, and how well each channel converts. */
        List<SourceBreakdown> bySource
) {
    /**
     * A walk-in and a website lead behave very differently, so the conversion rate is
     * reported per channel rather than only in aggregate.
     */
    public record SourceBreakdown(
            String source,
            int count,
            int convertedCount,
            double conversionRate
    ) {}
}

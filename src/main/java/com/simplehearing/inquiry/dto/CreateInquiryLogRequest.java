package com.simplehearing.inquiry.dto;

import com.simplehearing.inquiry.enums.InquiryLogType;
import jakarta.validation.constraints.NotNull;

public record CreateInquiryLogRequest(
        @NotNull InquiryLogType logType,
        String notes
) {}

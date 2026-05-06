package com.simplehearing.inquiry.dto;

import com.simplehearing.inquiry.entity.InquiryLog;
import com.simplehearing.inquiry.enums.InquiryLogType;

import java.time.Instant;
import java.util.UUID;

public record InquiryLogResponse(
        UUID id,
        UUID inquiryId,
        InquiryLogType logType,
        String notes,
        UUID createdBy,
        String createdByName,
        Instant createdAt
) {
    public static InquiryLogResponse from(InquiryLog log) {
        return new InquiryLogResponse(
                log.getId(),
                log.getInquiryId(),
                log.getLogType(),
                log.getNotes(),
                log.getCreatedBy(),
                log.getCreatedByName(),
                log.getCreatedAt()
        );
    }
}

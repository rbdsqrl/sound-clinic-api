package com.simplehearing.inquiry.dto;

import java.util.UUID;

public record ConvertInquiryResponse(
        UUID patientId,
        String patientName,
        String linkedUserInviteLink   // null when no linked user was requested
) {}

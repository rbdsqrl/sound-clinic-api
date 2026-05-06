package com.simplehearing.inquiry.dto;

import com.simplehearing.inquiry.enums.PreferredTime;
import jakarta.validation.constraints.NotBlank;

import java.util.UUID;

public record CreateInquiryRequest(
        @NotBlank(message = "Name is required") String name,
        String email,
        @NotBlank(message = "Phone is required") String phone,
        String reason,
        PreferredTime preferredTime,
        UUID orgId
) {}

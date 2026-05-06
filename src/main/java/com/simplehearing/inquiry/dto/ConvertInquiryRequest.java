package com.simplehearing.inquiry.dto;

import com.simplehearing.user.enums.Role;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record ConvertInquiryRequest(
        @NotBlank String firstName,
        @NotBlank String lastName,
        @NotNull UUID clinicId,
        // Optional: invite the inquiry submitter and auto-link them to the patient
        String linkedUserEmail,
        String linkedUserFirstName,
        String linkedUserLastName,
        Role linkedUserRole      // PARENT or PATIENT; defaults to PARENT if null
) {}

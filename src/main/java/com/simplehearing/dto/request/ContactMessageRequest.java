package com.simplehearing.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ContactMessageRequest(
    @NotBlank(message = "Full name is required")
    String fullName,

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email address")
    String email,

    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^[+]?[0-9\\s\\-]{7,20}$", message = "Invalid phone number")
    String phone,

    String message
) {}

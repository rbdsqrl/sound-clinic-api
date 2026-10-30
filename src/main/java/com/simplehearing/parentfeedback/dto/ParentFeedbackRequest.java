package com.simplehearing.parentfeedback.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ParentFeedbackRequest(@NotBlank @Size(max = 2000) String message) {}

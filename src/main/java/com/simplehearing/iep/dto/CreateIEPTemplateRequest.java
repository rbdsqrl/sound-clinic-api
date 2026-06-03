package com.simplehearing.iep.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.List;

public record CreateIEPTemplateRequest(
        @NotBlank String name,
        String description,
        List<String> tags
) {}

package com.simplehearing.iep.dto;

import java.util.List;

public record UpdateIEPTemplateRequest(
        String name,
        String description,
        List<String> tags
) {}

package com.simplehearing.organisation.dto;

import com.simplehearing.organisation.enums.AiProvider;

public record UpdateOrganisationRequest(
        String name,
        String contactEmail,
        String contactPhone,
        String address,
        String logoUrl,
        String timezone,
        AiProvider aiProvider,
        /** Write-only. Omit to leave the stored key unchanged; pass an empty string to clear it. */
        String aiApiKey
) {}

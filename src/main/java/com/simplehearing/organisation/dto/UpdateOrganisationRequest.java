package com.simplehearing.organisation.dto;

public record UpdateOrganisationRequest(
        String name,
        String contactEmail,
        String contactPhone,
        String address,
        String logoUrl,
        String timezone
) {}

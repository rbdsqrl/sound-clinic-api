package com.simplehearing.organisation.dto;

import com.simplehearing.organisation.entity.Organisation;
import com.simplehearing.organisation.enums.AiProvider;

import java.time.DayOfWeek;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public record OrganisationResponse(
        UUID id,
        String name,
        String slug,
        String contactEmail,
        String contactPhone,
        String address,
        String logoUrl,
        String timezone,
        boolean isActive,
        AiProvider aiProvider,
        boolean aiKeyConfigured,
        Set<DayOfWeek> weeklyOffDays,
        Instant createdAt
) {
    public static OrganisationResponse from(Organisation org) {
        return new OrganisationResponse(
                org.getId(),
                org.getName(),
                org.getSlug(),
                org.getContactEmail(),
                org.getContactPhone(),
                org.getAddress(),
                org.getLogoUrl(),
                org.getTimezone(),
                org.isActive(),
                org.getAiProvider(),
                org.getAiApiKey() != null && !org.getAiApiKey().isBlank(),
                org.getWeeklyOffDays(),
                org.getCreatedAt()
        );
    }
}

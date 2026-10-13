package com.simplehearing.auditlog.dto;

import com.simplehearing.session.dto.SessionActivityResponse.Change;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** One entry in a profile's activity log. {@code actorName} is "System" when no one acted. */
public record AuditLogResponse(
        UUID id,
        UUID actorId,
        String actorName,
        String type,
        String summary,
        List<Change> changes,
        Instant createdAt
) {}

package com.simplehearing.evidence.dto;

import com.simplehearing.evidence.enums.EvidenceReason;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/** "I couldn't upload a video" — recorded against a goal (to allow completing it) or ad hoc. */
public record CannotUploadRequest(
        @NotNull EvidenceReason reasonCode,
        /** Required when the reason is OTHER; optional detail otherwise. */
        String reasonText,
        UUID goalId,
        UUID sessionId,
        String note
) {}

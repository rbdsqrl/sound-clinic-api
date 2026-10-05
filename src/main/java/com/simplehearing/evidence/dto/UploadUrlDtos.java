package com.simplehearing.evidence.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Map;
import java.util.UUID;

/** Request/response shapes for the direct-to-storage video upload flow. */
public final class UploadUrlDtos {
    private UploadUrlDtos() {}

    /** What the client is about to upload — checked against the org's rules before any URL is issued. */
    public record UploadUrlRequest(
            @NotBlank String fileName,
            @NotBlank String contentType,
            @NotNull @Min(1) Long sizeBytes,
            Integer durationSeconds,
            UUID goalId,
            UUID sessionId,
            String note) {}

    /**
     * @param uploadId      pass back to /complete once the file has been sent
     * @param uploadUrl     PUT the file bytes here (no Authorization header — the URL itself is the credential)
     * @param headers       headers the PUT must carry
     * @param expiresInSeconds how long the URL stays valid
     */
    public record UploadUrlResponse(UUID uploadId, String uploadUrl, Map<String, String> headers, int expiresInSeconds) {}
}

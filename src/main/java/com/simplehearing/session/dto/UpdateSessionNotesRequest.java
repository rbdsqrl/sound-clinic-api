package com.simplehearing.session.dto;

public record UpdateSessionNotesRequest(
        String feedback,
        String progressReport,
        String notes
) {}

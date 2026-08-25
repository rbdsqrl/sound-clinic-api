package com.simplehearing.feed.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateFeedPostRequest(
        @NotBlank String title,
        String body
) {}

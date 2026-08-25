package com.simplehearing.feed.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateFeedCommentRequest(@NotBlank String body) {}

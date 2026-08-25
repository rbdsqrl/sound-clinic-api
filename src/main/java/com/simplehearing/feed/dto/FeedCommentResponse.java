package com.simplehearing.feed.dto;

import com.simplehearing.feed.entity.FeedPostComment;

import java.time.Instant;
import java.util.UUID;

public record FeedCommentResponse(
        UUID id,
        UUID postId,
        UUID authorId,
        String authorFirstName,
        String authorLastName,
        String body,
        Instant createdAt
) {
    public static FeedCommentResponse from(FeedPostComment c, String firstName, String lastName) {
        return new FeedCommentResponse(
                c.getId(), c.getPostId(), c.getAuthorId(), firstName, lastName, c.getBody(), c.getCreatedAt());
    }
}

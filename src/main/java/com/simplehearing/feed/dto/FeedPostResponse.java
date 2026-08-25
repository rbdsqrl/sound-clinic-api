package com.simplehearing.feed.dto;

import com.simplehearing.feed.entity.FeedPost;
import com.simplehearing.user.entity.User;
import com.simplehearing.user.enums.Role;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record FeedPostResponse(
        UUID id,
        UUID orgId,
        UUID authorId,
        String authorFirstName,
        String authorLastName,
        Role authorRole,
        String title,
        String body,
        Instant createdAt,
        Instant updatedAt,
        long likeCount,
        boolean likedByMe,
        long viewCount,
        long commentCount,
        List<FeedPostImageResponse> images
) {
    public static FeedPostResponse from(
            FeedPost post, User author,
            long likeCount, boolean likedByMe, long viewCount, long commentCount,
            List<FeedPostImageResponse> images) {
        return new FeedPostResponse(
                post.getId(),
                post.getOrgId(),
                post.getAuthorId(),
                author != null ? author.getFirstName() : null,
                author != null ? author.getLastName() : null,
                author != null ? author.getRole() : null,
                post.getTitle(),
                post.getBody(),
                post.getCreatedAt(),
                post.getUpdatedAt(),
                likeCount,
                likedByMe,
                viewCount,
                commentCount,
                images
        );
    }
}

package com.simplehearing.feed.dto;

import com.simplehearing.feed.entity.FeedPostImage;

import java.time.Instant;
import java.util.UUID;

public record FeedPostImageResponse(
        UUID id,
        UUID postId,
        String fileName,
        String fileUrl,
        String contentType,
        Long fileSizeBytes,
        int orderIndex,
        Instant createdAt
) {
    public static FeedPostImageResponse from(FeedPostImage img, String presignedUrl) {
        return new FeedPostImageResponse(
                img.getId(), img.getPostId(), img.getFileName(), presignedUrl,
                img.getContentType(), img.getFileSizeBytes(), img.getOrderIndex(), img.getCreatedAt());
    }
}

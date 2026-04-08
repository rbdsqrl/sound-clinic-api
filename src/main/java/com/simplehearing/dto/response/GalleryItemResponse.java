package com.simplehearing.dto.response;

import com.simplehearing.entity.GalleryItem;
import com.simplehearing.enums.GalleryType;

public record GalleryItemResponse(
    Long id,
    GalleryType type,
    String title,
    String category,
    String mediaUrl,
    String thumbnailUrl,
    String duration,
    String colorHex,
    int displayOrder
) {
    public static GalleryItemResponse from(GalleryItem g) {
        return new GalleryItemResponse(
            g.getId(), g.getType(), g.getTitle(), g.getCategory(),
            g.getMediaUrl(), g.getThumbnailUrl(), g.getDuration(),
            g.getColorHex(), g.getDisplayOrder()
        );
    }
}

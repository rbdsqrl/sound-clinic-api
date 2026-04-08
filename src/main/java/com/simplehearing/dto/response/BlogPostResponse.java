package com.simplehearing.dto.response;

import com.simplehearing.entity.BlogPost;

import java.time.LocalDate;

public record BlogPostResponse(
    Long id,
    String title,
    String category,
    String excerpt,
    String body,
    String authorName,
    String authorTitle,
    String readTime,
    boolean featured,
    LocalDate publishedDate,
    String coverColorHex
) {
    /** Summary (no body) for list endpoints */
    public static BlogPostResponse summary(BlogPost p) {
        return new BlogPostResponse(
            p.getId(), p.getTitle(), p.getCategory(), p.getExcerpt(),
            null, p.getAuthorName(), p.getAuthorTitle(),
            p.getReadTime(), p.isFeatured(), p.getPublishedDate(), p.getCoverColorHex()
        );
    }

    /** Full detail including body */
    public static BlogPostResponse detail(BlogPost p) {
        return new BlogPostResponse(
            p.getId(), p.getTitle(), p.getCategory(), p.getExcerpt(),
            p.getBody(), p.getAuthorName(), p.getAuthorTitle(),
            p.getReadTime(), p.isFeatured(), p.getPublishedDate(), p.getCoverColorHex()
        );
    }
}

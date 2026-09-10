package com.simplehearing.feed.repository;

import com.simplehearing.feed.entity.FeedPost;
import com.simplehearing.feed.enums.FeedPostType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface FeedPostRepository extends JpaRepository<FeedPost, UUID> {

    List<FeedPost> findByOrgIdOrderByCreatedAtDesc(UUID orgId);

    /** Paginated form of the above — backs the Dashboard's feed preview + its "View all" fetch. */
    Page<FeedPost> findByOrgIdOrderByCreatedAtDesc(UUID orgId, Pageable pageable);

    /** The regular Feed — POST-type only (MOM never shows here), and only what this viewer is
     *  allowed to see: a post with no recipients is public; otherwise the author, a named
     *  recipient, or a manager (who moderates every post regardless of targeting) can see it. */
    @Query(value = """
            SELECT DISTINCT p FROM FeedPost p
            WHERE p.orgId = :orgId AND p.type = com.simplehearing.feed.enums.FeedPostType.POST
              AND (:isManager = true
                   OR p.authorId = :userId
                   OR p.recipientIds IS EMPTY
                   OR :userId MEMBER OF p.recipientIds)
            ORDER BY p.createdAt DESC
            """,
            countQuery = """
            SELECT COUNT(DISTINCT p) FROM FeedPost p
            WHERE p.orgId = :orgId AND p.type = com.simplehearing.feed.enums.FeedPostType.POST
              AND (:isManager = true
                   OR p.authorId = :userId
                   OR p.recipientIds IS EMPTY
                   OR :userId MEMBER OF p.recipientIds)
            """)
    Page<FeedPost> findVisiblePosts(@Param("orgId") UUID orgId, @Param("userId") UUID userId,
                                     @Param("isManager") boolean isManager, Pageable pageable);

    /** Minutes of Meeting — staff-only (enforced at the controller), never targeted at
     *  individual recipients, so no visibility filtering needed beyond org + type. */
    Page<FeedPost> findByOrgIdAndTypeOrderByCreatedAtDesc(UUID orgId, FeedPostType type, Pageable pageable);
}

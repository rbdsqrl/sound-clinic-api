package com.simplehearing.feed.repository;

import com.simplehearing.feed.entity.FeedPost;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface FeedPostRepository extends JpaRepository<FeedPost, UUID> {

    List<FeedPost> findByOrgIdOrderByCreatedAtDesc(UUID orgId);

    /** Paginated form of the above — backs the Dashboard's feed preview + its "View all" fetch. */
    Page<FeedPost> findByOrgIdOrderByCreatedAtDesc(UUID orgId, Pageable pageable);
}

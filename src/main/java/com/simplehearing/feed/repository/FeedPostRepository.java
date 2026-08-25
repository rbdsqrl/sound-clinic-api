package com.simplehearing.feed.repository;

import com.simplehearing.feed.entity.FeedPost;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface FeedPostRepository extends JpaRepository<FeedPost, UUID> {

    List<FeedPost> findByOrgIdOrderByCreatedAtDesc(UUID orgId);
}

package com.simplehearing.feed.repository;

import com.simplehearing.feed.entity.FeedPostImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface FeedPostImageRepository extends JpaRepository<FeedPostImage, UUID> {

    List<FeedPostImage> findByPostIdOrderByOrderIndexAsc(UUID postId);

    @Query("SELECT i FROM FeedPostImage i WHERE i.postId IN :postIds ORDER BY i.orderIndex ASC")
    List<FeedPostImage> findByPostIdInOrderByOrderIndexAsc(List<UUID> postIds);
}

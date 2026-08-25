package com.simplehearing.feed.repository;

import com.simplehearing.feed.entity.FeedPostLike;
import com.simplehearing.feed.entity.FeedPostLikeId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface FeedPostLikeRepository extends JpaRepository<FeedPostLike, FeedPostLikeId> {

    long countById_PostId(UUID postId);

    @Query("SELECT l.id.postId AS postId, COUNT(l) AS cnt FROM FeedPostLike l WHERE l.id.postId IN :postIds GROUP BY l.id.postId")
    List<CountByPost> countByPostIdIn(List<UUID> postIds);

    @Query("SELECT l.id.postId FROM FeedPostLike l WHERE l.id.userId = :userId AND l.id.postId IN :postIds")
    Set<UUID> findLikedPostIds(UUID userId, List<UUID> postIds);

    interface CountByPost {
        UUID getPostId();
        Long getCnt();
    }
}

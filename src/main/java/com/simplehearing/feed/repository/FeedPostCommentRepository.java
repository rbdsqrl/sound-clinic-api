package com.simplehearing.feed.repository;

import com.simplehearing.feed.entity.FeedPostComment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface FeedPostCommentRepository extends JpaRepository<FeedPostComment, UUID> {

    List<FeedPostComment> findByPostIdOrderByCreatedAtAsc(UUID postId);

    long countByPostId(UUID postId);

    @Query("SELECT c.postId AS postId, COUNT(c) AS cnt FROM FeedPostComment c WHERE c.postId IN :postIds GROUP BY c.postId")
    List<CountByPost> countByPostIdIn(List<UUID> postIds);

    interface CountByPost {
        UUID getPostId();
        Long getCnt();
    }
}

package com.simplehearing.feed.repository;

import com.simplehearing.feed.entity.FeedPostView;
import com.simplehearing.feed.entity.FeedPostViewId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface FeedPostViewRepository extends JpaRepository<FeedPostView, FeedPostViewId> {

    long countById_PostId(UUID postId);

    @Query("SELECT v.id.postId AS postId, COUNT(v) AS cnt FROM FeedPostView v WHERE v.id.postId IN :postIds GROUP BY v.id.postId")
    List<CountByPost> countByPostIdIn(List<UUID> postIds);

    interface CountByPost {
        UUID getPostId();
        Long getCnt();
    }
}

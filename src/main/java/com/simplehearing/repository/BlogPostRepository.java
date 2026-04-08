package com.simplehearing.repository;

import com.simplehearing.entity.BlogPost;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BlogPostRepository extends JpaRepository<BlogPost, Long> {
    Page<BlogPost> findByPublishedTrueOrderByFeaturedDescPublishedDateDesc(Pageable pageable);
    Page<BlogPost> findByPublishedTrueAndCategoryOrderByPublishedDateDesc(String category, Pageable pageable);
    Optional<BlogPost> findFirstByPublishedTrueAndFeaturedTrue();
}

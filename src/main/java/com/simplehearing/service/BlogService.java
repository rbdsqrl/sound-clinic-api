package com.simplehearing.service;

import com.simplehearing.dto.response.BlogPostResponse;
import com.simplehearing.entity.BlogPost;
import com.simplehearing.exception.ResourceNotFoundException;
import com.simplehearing.repository.BlogPostRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class BlogService {

    private final BlogPostRepository repo;

    public BlogService(BlogPostRepository repo) {
        this.repo = repo;
    }

    public Page<BlogPostResponse> getPublished(String category, Pageable pageable) {
        if (category != null && !category.isBlank()) {
            return repo.findByPublishedTrueAndCategoryOrderByPublishedDateDesc(category, pageable)
                .map(BlogPostResponse::summary);
        }
        return repo.findByPublishedTrueOrderByFeaturedDescPublishedDateDesc(pageable)
            .map(BlogPostResponse::summary);
    }

    public BlogPostResponse getFeatured() {
        return repo.findFirstByPublishedTrueAndFeaturedTrue()
            .map(BlogPostResponse::summary)
            .orElseThrow(() -> new ResourceNotFoundException("No featured blog post found"));
    }

    public BlogPostResponse getById(Long id) {
        return repo.findById(id)
            .filter(BlogPost::isPublished)
            .map(BlogPostResponse::detail)
            .orElseThrow(() -> new ResourceNotFoundException("Blog post", id));
    }

    @Transactional
    public BlogPostResponse create(BlogPost post) {
        return BlogPostResponse.detail(repo.save(post));
    }

    @Transactional
    public BlogPostResponse update(Long id, BlogPost updated) {
        BlogPost existing = repo.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Blog post", id));
        existing.setTitle(updated.getTitle());
        existing.setCategory(updated.getCategory());
        existing.setExcerpt(updated.getExcerpt());
        existing.setBody(updated.getBody());
        existing.setReadTime(updated.getReadTime());
        existing.setFeatured(updated.isFeatured());
        existing.setPublished(updated.isPublished());
        existing.setPublishedDate(updated.getPublishedDate());
        existing.setCoverColorHex(updated.getCoverColorHex());
        return BlogPostResponse.detail(repo.save(existing));
    }

    @Transactional
    public void delete(Long id) {
        BlogPost post = repo.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Blog post", id));
        post.setPublished(false);
        repo.save(post);
    }
}

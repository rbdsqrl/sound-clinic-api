package com.simplehearing.controller;

import com.simplehearing.dto.response.BlogPostResponse;
import com.simplehearing.entity.BlogPost;
import com.simplehearing.service.BlogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/blog")
@Tag(name = "Blog", description = "Blog posts and articles")
public class BlogController {

    private final BlogService blogService;

    public BlogController(BlogService blogService) {
        this.blogService = blogService;
    }

    @GetMapping
    @Operation(summary = "List published blog posts")
    public ResponseEntity<Page<BlogPostResponse>> getAll(
        @RequestParam(required = false) String category,
        @PageableDefault(size = 10, sort = "publishedDate") Pageable pageable) {
        return ResponseEntity.ok(blogService.getPublished(category, pageable));
    }

    @GetMapping("/featured")
    @Operation(summary = "Get the featured blog post")
    public ResponseEntity<BlogPostResponse> getFeatured() {
        return ResponseEntity.ok(blogService.getFeatured());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get full blog post by ID")
    public ResponseEntity<BlogPostResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(blogService.getById(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create a blog post (admin)")
    public ResponseEntity<BlogPostResponse> create(@RequestBody BlogPost post) {
        return ResponseEntity.status(HttpStatus.CREATED).body(blogService.create(post));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update a blog post (admin)")
    public ResponseEntity<BlogPostResponse> update(
        @PathVariable Long id, @RequestBody BlogPost post) {
        return ResponseEntity.ok(blogService.update(id, post));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Unpublish a blog post (admin)")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        blogService.delete(id);
        return ResponseEntity.noContent().build();
    }
}

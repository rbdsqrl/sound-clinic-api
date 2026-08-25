package com.simplehearing.feed.controller;

import com.simplehearing.auth.security.UserPrincipal;
import com.simplehearing.common.dto.ApiResponse;
import com.simplehearing.common.exception.ApiException;
import com.simplehearing.common.exception.ResourceNotFoundException;
import com.simplehearing.feed.dto.CreateFeedPostRequest;
import com.simplehearing.feed.dto.FeedPostResponse;
import com.simplehearing.feed.dto.UpdateFeedPostRequest;
import com.simplehearing.feed.entity.FeedPost;
import com.simplehearing.feed.repository.FeedPostRepository;
import com.simplehearing.user.entity.User;
import com.simplehearing.user.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

/**
 * Clinic-wide announcements. Every role in the org can read the feed; only
 * BUSINESS_OWNER and CLINIC_HEAD can post, edit, or remove a post.
 */
@Tag(name = "Feed", description = "Clinic-wide announcement posts")
@RestController
@RequestMapping("/api/v1/feed")
public class FeedController {

    private final FeedPostRepository feedPostRepository;
    private final UserRepository userRepository;

    public FeedController(FeedPostRepository feedPostRepository, UserRepository userRepository) {
        this.feedPostRepository = feedPostRepository;
        this.userRepository = userRepository;
    }

    @Operation(summary = "List feed posts for the org, newest first")
    @GetMapping
    public ResponseEntity<ApiResponse<List<FeedPostResponse>>> list(
            @AuthenticationPrincipal UserPrincipal principal) {

        List<FeedPost> posts = feedPostRepository.findByOrgIdOrderByCreatedAtDesc(principal.getOrgId());

        Map<UUID, User> authorsById = userRepository
                .findAllById(posts.stream().map(FeedPost::getAuthorId).distinct().toList())
                .stream()
                .collect(java.util.stream.Collectors.toMap(User::getId, Function.identity()));

        List<FeedPostResponse> result = posts.stream()
                .map(p -> FeedPostResponse.from(p, authorsById.get(p.getAuthorId())))
                .toList();

        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @Operation(summary = "Post a new clinic-wide update")
    @PostMapping
    @PreAuthorize("hasAnyRole('BUSINESS_OWNER', 'CLINIC_HEAD')")
    public ResponseEntity<ApiResponse<FeedPostResponse>> create(
            @Valid @RequestBody CreateFeedPostRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {

        FeedPost post = new FeedPost();
        post.setOrgId(principal.getOrgId());
        post.setAuthorId(principal.getId());
        post.setTitle(request.title().trim());
        post.setBody(request.body() != null && !request.body().isBlank() ? request.body().trim() : null);

        FeedPost saved = feedPostRepository.save(post);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(FeedPostResponse.from(saved, principal.getUser())));
    }

    @Operation(summary = "Edit a feed post")
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('BUSINESS_OWNER', 'CLINIC_HEAD')")
    public ResponseEntity<ApiResponse<FeedPostResponse>> update(
            @PathVariable UUID id,
            @RequestBody UpdateFeedPostRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {

        FeedPost post = findInOrg(id, principal);

        if (request.title() != null && !request.title().isBlank()) {
            post.setTitle(request.title().trim());
        }
        if (request.body() != null) {
            post.setBody(request.body().isBlank() ? null : request.body().trim());
        }

        FeedPost saved = feedPostRepository.save(post);
        User author = userRepository.findById(saved.getAuthorId()).orElse(null);
        return ResponseEntity.ok(ApiResponse.success(FeedPostResponse.from(saved, author)));
    }

    @Operation(summary = "Remove a feed post")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('BUSINESS_OWNER', 'CLINIC_HEAD')")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal) {

        FeedPost post = findInOrg(id, principal);
        feedPostRepository.delete(post);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    private FeedPost findInOrg(UUID id, UserPrincipal principal) {
        FeedPost post = feedPostRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Feed post not found"));
        if (!post.getOrgId().equals(principal.getOrgId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Access denied");
        }
        return post;
    }
}

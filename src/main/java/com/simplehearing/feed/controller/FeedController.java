package com.simplehearing.feed.controller;

import com.simplehearing.auth.security.UserPrincipal;
import com.simplehearing.common.dto.ApiResponse;
import com.simplehearing.common.exception.ApiException;
import com.simplehearing.common.exception.ResourceNotFoundException;
import com.simplehearing.feed.dto.CreateFeedCommentRequest;
import com.simplehearing.feed.dto.CreateFeedPostRequest;
import com.simplehearing.feed.dto.FeedCommentResponse;
import com.simplehearing.feed.dto.FeedPostImageResponse;
import com.simplehearing.feed.dto.FeedPostResponse;
import com.simplehearing.feed.dto.UpdateFeedPostRequest;
import com.simplehearing.feed.entity.FeedPost;
import com.simplehearing.feed.entity.FeedPostComment;
import com.simplehearing.feed.entity.FeedPostImage;
import com.simplehearing.feed.entity.FeedPostLike;
import com.simplehearing.feed.entity.FeedPostLikeId;
import com.simplehearing.feed.entity.FeedPostView;
import com.simplehearing.feed.entity.FeedPostViewId;
import com.simplehearing.feed.repository.FeedPostCommentRepository;
import com.simplehearing.feed.repository.FeedPostImageRepository;
import com.simplehearing.feed.repository.FeedPostLikeRepository;
import com.simplehearing.feed.repository.FeedPostRepository;
import com.simplehearing.feed.repository.FeedPostViewRepository;
import com.simplehearing.storage.StorageService;
import com.simplehearing.user.entity.User;
import com.simplehearing.user.enums.Role;
import com.simplehearing.user.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Duration;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Clinic-wide announcements. Every role in the org can read the feed and like/comment on a
 * post; only BUSINESS_OWNER and CLINIC_HEAD can post, edit, remove a post, or manage its
 * images.
 */
@Tag(name = "Feed", description = "Clinic-wide announcement posts")
@RestController
@RequestMapping("/api/v1/feed")
public class FeedController {

    private final FeedPostRepository feedPostRepository;
    private final FeedPostLikeRepository likeRepository;
    private final FeedPostViewRepository viewRepository;
    private final FeedPostCommentRepository commentRepository;
    private final FeedPostImageRepository imageRepository;
    private final UserRepository userRepository;
    private final StorageService storageService;

    public FeedController(
            FeedPostRepository feedPostRepository,
            FeedPostLikeRepository likeRepository,
            FeedPostViewRepository viewRepository,
            FeedPostCommentRepository commentRepository,
            FeedPostImageRepository imageRepository,
            UserRepository userRepository,
            StorageService storageService) {
        this.feedPostRepository = feedPostRepository;
        this.likeRepository = likeRepository;
        this.viewRepository = viewRepository;
        this.commentRepository = commentRepository;
        this.imageRepository = imageRepository;
        this.userRepository = userRepository;
        this.storageService = storageService;
    }

    // ── List ─────────────────────────────────────────────────────────────────────

    @Operation(summary = "List feed posts for the org, newest first")
    @GetMapping
    public ResponseEntity<ApiResponse<List<FeedPostResponse>>> list(
            @AuthenticationPrincipal UserPrincipal principal) {

        List<FeedPost> posts = feedPostRepository.findByOrgIdOrderByCreatedAtDesc(principal.getOrgId());
        List<UUID> postIds = posts.stream().map(FeedPost::getId).toList();

        Map<UUID, User> authorsById = userRepository
                .findAllById(posts.stream().map(FeedPost::getAuthorId).distinct().toList())
                .stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));

        Map<UUID, Long> likeCounts = toCountMap(likeRepository.countByPostIdIn(postIds),
                FeedPostLikeRepository.CountByPost::getPostId, FeedPostLikeRepository.CountByPost::getCnt);
        Map<UUID, Long> viewCounts = toCountMap(viewRepository.countByPostIdIn(postIds),
                FeedPostViewRepository.CountByPost::getPostId, FeedPostViewRepository.CountByPost::getCnt);
        Map<UUID, Long> commentCounts = toCountMap(commentRepository.countByPostIdIn(postIds),
                FeedPostCommentRepository.CountByPost::getPostId, FeedPostCommentRepository.CountByPost::getCnt);
        Set<UUID> likedByMe = postIds.isEmpty() ? Set.of() : likeRepository.findLikedPostIds(principal.getId(), postIds);
        Map<UUID, List<FeedPostImageResponse>> imagesByPost = imageRepository.findByPostIdInOrderByOrderIndexAsc(postIds)
                .stream()
                .map(img -> Map.entry(img.getPostId(), presignImage(img)))
                .collect(Collectors.groupingBy(Map.Entry::getKey, Collectors.mapping(Map.Entry::getValue, Collectors.toList())));

        List<FeedPostResponse> result = posts.stream()
                .map(p -> FeedPostResponse.from(
                        p, authorsById.get(p.getAuthorId()),
                        likeCounts.getOrDefault(p.getId(), 0L),
                        likedByMe.contains(p.getId()),
                        viewCounts.getOrDefault(p.getId(), 0L),
                        commentCounts.getOrDefault(p.getId(), 0L),
                        imagesByPost.getOrDefault(p.getId(), List.of())))
                .toList();

        return ResponseEntity.ok(ApiResponse.success(result));
    }

    // ── Create / Update / Delete ────────────────────────────────────────────────

    @Operation(summary = "Post a new clinic-wide update")
    @PostMapping
    @PreAuthorize("hasAnyRole('BUSINESS_OWNER', 'CLINIC_HEAD', 'OFFICE_ADMIN')")
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
                .body(ApiResponse.success(buildResponse(saved, principal.getUser(), principal.getId())));
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
        return ResponseEntity.ok(ApiResponse.success(buildResponse(saved, author, principal.getId())));
    }

    @Operation(summary = "Remove a feed post")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('BUSINESS_OWNER', 'CLINIC_HEAD')")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal) {

        FeedPost post = findInOrg(id, principal);
        // Likes/views/comments/images cascade at the DB level (ON DELETE CASCADE).
        // Stored image files themselves are best-effort cleaned up.
        imageRepository.findByPostIdOrderByOrderIndexAsc(post.getId())
                .forEach(img -> storageService.delete(img.getFileUrl()));
        feedPostRepository.delete(post);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    // ── Likes ────────────────────────────────────────────────────────────────────

    @Operation(summary = "Like a feed post")
    @PostMapping("/{id}/like")
    public ResponseEntity<ApiResponse<FeedPostResponse>> like(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal) {

        FeedPost post = findInOrg(id, principal);
        FeedPostLikeId likeId = new FeedPostLikeId(post.getId(), principal.getId());
        if (!likeRepository.existsById(likeId)) {
            likeRepository.save(new FeedPostLike(post.getId(), principal.getId()));
        }
        User author = userRepository.findById(post.getAuthorId()).orElse(null);
        return ResponseEntity.ok(ApiResponse.success(buildResponse(post, author, principal.getId())));
    }

    @Operation(summary = "Unlike a feed post")
    @DeleteMapping("/{id}/like")
    public ResponseEntity<ApiResponse<FeedPostResponse>> unlike(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal) {

        FeedPost post = findInOrg(id, principal);
        likeRepository.deleteById(new FeedPostLikeId(post.getId(), principal.getId()));
        User author = userRepository.findById(post.getAuthorId()).orElse(null);
        return ResponseEntity.ok(ApiResponse.success(buildResponse(post, author, principal.getId())));
    }

    // ── Views ────────────────────────────────────────────────────────────────────

    @Operation(summary = "Record that the current user has seen a post (idempotent, once per user)")
    @PostMapping("/{id}/view")
    public ResponseEntity<ApiResponse<Void>> recordView(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal) {

        FeedPost post = findInOrg(id, principal);
        FeedPostViewId viewId = new FeedPostViewId(post.getId(), principal.getId());
        if (!viewRepository.existsById(viewId)) {
            viewRepository.save(new FeedPostView(post.getId(), principal.getId()));
        }
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    // ── Comments ─────────────────────────────────────────────────────────────────

    @Operation(summary = "List comments on a feed post")
    @GetMapping("/{id}/comments")
    public ResponseEntity<ApiResponse<List<FeedCommentResponse>>> listComments(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal) {

        findInOrg(id, principal);
        List<FeedPostComment> comments = commentRepository.findByPostIdOrderByCreatedAtAsc(id);
        Map<UUID, User> authorsById = userRepository
                .findAllById(comments.stream().map(FeedPostComment::getAuthorId).distinct().toList())
                .stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));

        List<FeedCommentResponse> result = comments.stream()
                .map(c -> {
                    User author = authorsById.get(c.getAuthorId());
                    return FeedCommentResponse.from(c,
                            author != null ? author.getFirstName() : null,
                            author != null ? author.getLastName() : null);
                })
                .toList();

        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @Operation(summary = "Comment on a feed post")
    @PostMapping("/{id}/comments")
    public ResponseEntity<ApiResponse<FeedCommentResponse>> addComment(
            @PathVariable UUID id,
            @Valid @RequestBody CreateFeedCommentRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {

        FeedPost post = findInOrg(id, principal);

        FeedPostComment comment = new FeedPostComment();
        comment.setOrgId(post.getOrgId());
        comment.setPostId(post.getId());
        comment.setAuthorId(principal.getId());
        comment.setBody(request.body().trim());

        FeedPostComment saved = commentRepository.save(comment);
        User author = principal.getUser();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(FeedCommentResponse.from(saved, author.getFirstName(), author.getLastName())));
    }

    @Operation(summary = "Delete a comment")
    @DeleteMapping("/{id}/comments/{commentId}")
    public ResponseEntity<ApiResponse<Void>> deleteComment(
            @PathVariable UUID id,
            @PathVariable UUID commentId,
            @AuthenticationPrincipal UserPrincipal principal) {

        findInOrg(id, principal);
        FeedPostComment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment not found"));

        if (!comment.getPostId().equals(id) || !comment.getOrgId().equals(principal.getOrgId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Access denied");
        }

        Role role = principal.getUser().getRole();
        if (!isManager(role) && !comment.getAuthorId().equals(principal.getId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "You can only delete your own comments");
        }

        commentRepository.delete(comment);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    // ── Images ───────────────────────────────────────────────────────────────────

    @Operation(summary = "Attach images to a feed post")
    @PostMapping("/{id}/images")
    @PreAuthorize("hasAnyRole('BUSINESS_OWNER', 'CLINIC_HEAD', 'OFFICE_ADMIN')")
    public ResponseEntity<ApiResponse<List<FeedPostImageResponse>>> uploadImages(
            @PathVariable UUID id,
            @RequestParam("files") List<MultipartFile> files,
            @AuthenticationPrincipal UserPrincipal principal) throws IOException {

        FeedPost post = findInOrg(id, principal);
        int nextIndex = imageRepository.findByPostIdOrderByOrderIndexAsc(post.getId()).size();

        List<FeedPostImageResponse> uploaded = new java.util.ArrayList<>();
        for (MultipartFile file : files) {
            String url = storageService.store(file, "feed/" + post.getId());

            FeedPostImage img = new FeedPostImage();
            img.setOrgId(post.getOrgId());
            img.setPostId(post.getId());
            img.setUploadedBy(principal.getId());
            img.setFileName(file.getOriginalFilename() != null ? file.getOriginalFilename() : "image");
            img.setFileUrl(url);
            img.setContentType(file.getContentType());
            img.setFileSizeBytes(file.getSize());
            img.setOrderIndex(nextIndex++);

            FeedPostImage saved = imageRepository.save(img);
            uploaded.add(presignImage(saved));
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(uploaded));
    }

    @Operation(summary = "Remove an image from a feed post")
    @DeleteMapping("/{id}/images/{imageId}")
    @PreAuthorize("hasAnyRole('BUSINESS_OWNER', 'CLINIC_HEAD')")
    public ResponseEntity<ApiResponse<Void>> deleteImage(
            @PathVariable UUID id,
            @PathVariable UUID imageId,
            @AuthenticationPrincipal UserPrincipal principal) {

        findInOrg(id, principal);
        FeedPostImage img = imageRepository.findById(imageId)
                .orElseThrow(() -> new ResourceNotFoundException("Image not found"));

        if (!img.getPostId().equals(id) || !img.getOrgId().equals(principal.getOrgId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Access denied");
        }

        storageService.delete(img.getFileUrl());
        imageRepository.delete(img);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    // ── Helpers ──────────────────────────────────────────────────────────────────

    private FeedPost findInOrg(UUID id, UserPrincipal principal) {
        FeedPost post = feedPostRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Feed post not found"));
        if (!post.getOrgId().equals(principal.getOrgId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Access denied");
        }
        return post;
    }

    private static boolean isManager(Role role) {
        return role == Role.BUSINESS_OWNER || role == Role.CLINIC_HEAD;
    }

    private FeedPostImageResponse presignImage(FeedPostImage img) {
        return FeedPostImageResponse.from(img, storageService.presign(img.getFileUrl(), Duration.ofHours(1)));
    }

    /** Builds a single post's response with freshly-queried (non-batched) engagement data. */
    private FeedPostResponse buildResponse(FeedPost post, User author, UUID viewerId) {
        long likeCount = likeRepository.countById_PostId(post.getId());
        boolean likedByMe = likeRepository.existsById(new FeedPostLikeId(post.getId(), viewerId));
        long viewCount = viewRepository.countById_PostId(post.getId());
        long commentCount = commentRepository.countByPostId(post.getId());
        List<FeedPostImageResponse> images = imageRepository.findByPostIdOrderByOrderIndexAsc(post.getId())
                .stream()
                .sorted(Comparator.comparingInt(FeedPostImage::getOrderIndex))
                .map(this::presignImage)
                .toList();
        return FeedPostResponse.from(post, author, likeCount, likedByMe, viewCount, commentCount, images);
    }

    private <T> Map<UUID, Long> toCountMap(List<T> rows, Function<T, UUID> keyFn, Function<T, Long> valFn) {
        Map<UUID, Long> map = new HashMap<>();
        for (T row : rows) map.put(keyFn.apply(row), valFn.apply(row));
        return map;
    }
}

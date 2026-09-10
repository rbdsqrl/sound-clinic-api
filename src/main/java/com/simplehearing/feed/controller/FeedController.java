package com.simplehearing.feed.controller;

import com.simplehearing.auth.security.UserPrincipal;
import com.simplehearing.common.dto.ApiResponse;
import com.simplehearing.common.dto.PagedResponse;
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
import com.simplehearing.feed.enums.FeedPostType;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
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
    private final com.simplehearing.notification.EmailService emailService;
    private final com.simplehearing.organisation.repository.OrganisationRepository organisationRepository;

    public FeedController(
            FeedPostRepository feedPostRepository,
            FeedPostLikeRepository likeRepository,
            FeedPostViewRepository viewRepository,
            FeedPostCommentRepository commentRepository,
            FeedPostImageRepository imageRepository,
            UserRepository userRepository,
            StorageService storageService,
            com.simplehearing.notification.EmailService emailService,
            com.simplehearing.organisation.repository.OrganisationRepository organisationRepository) {
        this.feedPostRepository = feedPostRepository;
        this.likeRepository = likeRepository;
        this.viewRepository = viewRepository;
        this.commentRepository = commentRepository;
        this.imageRepository = imageRepository;
        this.userRepository = userRepository;
        this.storageService = storageService;
        this.emailService = emailService;
        this.organisationRepository = organisationRepository;
    }

    // ── List ─────────────────────────────────────────────────────────────────────

    @Operation(
        summary = "List feed posts for the org, paginated, newest first",
        description = "Defaults to 20 per page, sorted by createdAt descending. POST-type only — " +
                "MOM entries have their own endpoint. Only posts this viewer can see: public " +
                "(no recipients), one they authored, one they're a named recipient of, or any " +
                "post at all if they're a manager."
    )
    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<FeedPostResponse>>> list(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            @AuthenticationPrincipal UserPrincipal principal) {

        boolean isManager = isManager(principal.getUser().getRole());
        Page<FeedPost> page = feedPostRepository.findVisiblePosts(
                principal.getOrgId(), principal.getId(), isManager, pageable);
        PagedResponse<FeedPostResponse> paged = toPagedResponse(page, principal);
        return ResponseEntity.ok(ApiResponse.success(paged));
    }

    @Operation(
        summary = "List Minutes of Meeting entries — staff only, never shown in the regular feed",
        description = "Defaults to 20 per page, sorted by createdAt descending."
    )
    @GetMapping("/mom")
    @PreAuthorize("hasAnyRole('BUSINESS_OWNER', 'CLINIC_HEAD', 'OFFICE_ADMIN', 'THERAPIST')")
    public ResponseEntity<ApiResponse<PagedResponse<FeedPostResponse>>> listMom(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            @AuthenticationPrincipal UserPrincipal principal) {

        Page<FeedPost> page = feedPostRepository.findByOrgIdAndTypeOrderByCreatedAtDesc(
                principal.getOrgId(), FeedPostType.MOM, pageable);
        PagedResponse<FeedPostResponse> paged = toPagedResponse(page, principal);
        return ResponseEntity.ok(ApiResponse.success(paged));
    }

    private PagedResponse<FeedPostResponse> toPagedResponse(Page<FeedPost> page, UserPrincipal principal) {
        List<FeedPost> posts = page.getContent();
        List<UUID> postIds = posts.stream().map(FeedPost::getId).toList();

        Set<UUID> allUserIds = new java.util.HashSet<>(posts.stream().map(FeedPost::getAuthorId).toList());
        posts.forEach(p -> { allUserIds.addAll(p.getRecipientIds()); allUserIds.addAll(p.getAttendeeIds()); });
        Map<UUID, User> usersById = userRepository.findAllById(allUserIds).stream()
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
                        p, usersById.get(p.getAuthorId()),
                        likeCounts.getOrDefault(p.getId(), 0L),
                        likedByMe.contains(p.getId()),
                        viewCounts.getOrDefault(p.getId(), 0L),
                        commentCounts.getOrDefault(p.getId(), 0L),
                        imagesByPost.getOrDefault(p.getId(), List.of()),
                        summariesOf(p.getRecipientIds(), usersById),
                        summariesOf(p.getAttendeeIds(), usersById)))
                .toList();

        return new PagedResponse<>(result, page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
    }

    private List<FeedPostResponse.RecipientSummary> summariesOf(Set<UUID> ids, Map<UUID, User> usersById) {
        return ids.stream()
                .map(usersById::get)
                .filter(java.util.Objects::nonNull)
                .map(u -> new FeedPostResponse.RecipientSummary(u.getId(), u.getFirstName(), u.getLastName()))
                .toList();
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
        post.setType(request.type() != null ? request.type() : FeedPostType.POST);
        // MOM is staff-wide by definition, not targeted at specific people — recipientIds only
        // apply to a POST. Attendees, the other way round, only make sense for a MOM. Either
        // field sent for the wrong type is ignored rather than rejected, so the frontend doesn't
        // need to special-case clearing fields when the user switches tabs.
        if (post.getType() == FeedPostType.POST) {
            post.setRecipientIds(validRecipientIds(request.recipientIds(), principal.getOrgId()));
        } else {
            post.setAttendeeIds(validRecipientIds(request.attendeeIds(), principal.getOrgId()));
        }

        FeedPost saved = feedPostRepository.save(post);
        notifyOnPublish(saved, principal.getUser());
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
        if (request.recipientIds() != null && post.getType() == FeedPostType.POST) {
            post.setRecipientIds(validRecipientIds(request.recipientIds(), principal.getOrgId()));
        }
        if (request.attendeeIds() != null && post.getType() == FeedPostType.MOM) {
            post.setAttendeeIds(validRecipientIds(request.attendeeIds(), principal.getOrgId()));
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
        if (!isVisibleTo(post, principal)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Access denied");
        }
        return post;
    }

    /** Same rule as {@code findVisiblePosts}, applied to a single post an ID could point at
     *  directly — otherwise a non-recipient could still like/comment/view a targeted post by
     *  guessing or reusing its id, even though it never appears in their own feed list. MOM
     *  entries are staff-only, checked here rather than left to whatever list endpoint found
     *  the id, since like/comment/view take a bare post id with no type context of their own. */
    private boolean isVisibleTo(FeedPost post, UserPrincipal principal) {
        Role role = principal.getUser().getRole();
        if (post.getType() == FeedPostType.MOM) {
            return isStaffRole(role);
        }
        return isManager(role)
                || post.getAuthorId().equals(principal.getId())
                || post.getRecipientIds().isEmpty()
                || post.getRecipientIds().contains(principal.getId());
    }

    private static boolean isManager(Role role) {
        return role == Role.BUSINESS_OWNER || role == Role.CLINIC_HEAD;
    }

    private static boolean isStaffRole(Role role) {
        return role == Role.BUSINESS_OWNER || role == Role.CLINIC_HEAD
                || role == Role.OFFICE_ADMIN || role == Role.THERAPIST;
    }

    /** Silently drops any id that doesn't resolve to a user in this org — same "invalid ids are
     *  dropped, not rejected" convention as ResourceService.getByIds. */
    private Set<UUID> validRecipientIds(Set<UUID> requested, UUID orgId) {
        if (requested == null || requested.isEmpty()) return new java.util.HashSet<>();
        return userRepository.findAllById(requested).stream()
                .filter(u -> u.getOrgId().equals(orgId))
                .map(User::getId)
                .collect(Collectors.toSet());
    }

    /** MOM always notifies every active staff member; a POST only notifies anyone when it has
     *  no specific recipients — i.e. it was actually shared with everyone, not a targeted note. */
    private void notifyOnPublish(FeedPost post, User author) {
        String authorName = author.getFirstName() + " " + author.getLastName();
        String orgName = organisationRepository.findById(post.getOrgId()).map(o -> o.getName()).orElse("Simple Hearing");

        if (post.getType() == FeedPostType.MOM) {
            List<String> staffEmails = userRepository.findByOrgId(post.getOrgId()).stream()
                    .filter(User::isActive)
                    .filter(u -> isStaffRole(u.getRole()))
                    .map(User::getEmail)
                    .toList();
            if (!staffEmails.isEmpty()) {
                emailService.sendMomNotification(staffEmails, authorName, post.getTitle(), orgName);
            }
            return;
        }

        if (post.getRecipientIds().isEmpty()) {
            List<String> everyone = userRepository.findByOrgId(post.getOrgId()).stream()
                    .filter(User::isActive)
                    .map(User::getEmail)
                    .toList();
            if (!everyone.isEmpty()) {
                String snippet = post.getBody() != null ? post.getBody().replaceAll("<[^>]*>", " ").trim() : null;
                if (snippet != null && snippet.length() > 200) snippet = snippet.substring(0, 200) + "…";
                emailService.sendFeedPostNotification(everyone, authorName, post.getTitle(), snippet, orgName);
            }
        }
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
        Set<UUID> namedIds = new java.util.HashSet<>(post.getRecipientIds());
        namedIds.addAll(post.getAttendeeIds());
        Map<UUID, User> namedUsers = userRepository.findAllById(namedIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));
        return FeedPostResponse.from(post, author, likeCount, likedByMe, viewCount, commentCount, images,
                summariesOf(post.getRecipientIds(), namedUsers), summariesOf(post.getAttendeeIds(), namedUsers));
    }

    private <T> Map<UUID, Long> toCountMap(List<T> rows, Function<T, UUID> keyFn, Function<T, Long> valFn) {
        Map<UUID, Long> map = new HashMap<>();
        for (T row : rows) map.put(keyFn.apply(row), valFn.apply(row));
        return map;
    }
}

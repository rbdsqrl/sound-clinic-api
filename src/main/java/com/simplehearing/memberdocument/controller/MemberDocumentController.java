package com.simplehearing.memberdocument.controller;

import com.simplehearing.auth.security.UserPrincipal;
import com.simplehearing.common.dto.ApiResponse;
import com.simplehearing.common.exception.ApiException;
import com.simplehearing.common.exception.ResourceNotFoundException;
import com.simplehearing.memberdocument.dto.MemberDocumentResponse;
import com.simplehearing.memberdocument.entity.MemberDocument;
import com.simplehearing.memberdocument.enums.MemberDocumentCategory;
import com.simplehearing.memberdocument.repository.MemberDocumentRepository;
import com.simplehearing.storage.StorageService;
import com.simplehearing.user.entity.User;
import com.simplehearing.user.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Documents kept on a staff member's record (ID proof, qualifications, contracts…). These are
 * HR-sensitive, so access is deliberately narrower than the rest of the Members area: only the
 * Business Owner and Clinic Heads can view, add or remove them.
 */
@Tag(name = "Member Documents", description = "Files stored on a staff member's record")
@RestController
@RequestMapping("/api/v1/users/{userId}/documents")
@PreAuthorize("hasAnyRole('BUSINESS_OWNER', 'CLINIC_HEAD')")
public class MemberDocumentController {

    private static final long MAX_FILE_BYTES = 25L * 1024 * 1024;

    /** Documents and still images only — no video, and no SVG (it can carry script). */
    private static final Set<String> SUPPORTED_TYPES = Set.of(
            "application/pdf",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.ms-excel",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "application/vnd.ms-powerpoint",
            "application/vnd.openxmlformats-officedocument.presentationml.presentation",
            "text/plain", "text/csv",
            "image/jpeg", "image/png", "image/webp", "image/gif", "image/heic");

    private final MemberDocumentRepository documentRepository;
    private final UserRepository userRepository;
    private final StorageService storageService;

    public MemberDocumentController(MemberDocumentRepository documentRepository,
                                    UserRepository userRepository,
                                    StorageService storageService) {
        this.documentRepository = documentRepository;
        this.userRepository = userRepository;
        this.storageService = storageService;
    }

    @Operation(summary = "List the documents on a staff member's record, newest first")
    @GetMapping
    public ResponseEntity<ApiResponse<List<MemberDocumentResponse>>> list(
            @PathVariable UUID userId,
            @AuthenticationPrincipal UserPrincipal principal) {

        requireMemberInOrg(userId, principal);

        List<MemberDocument> docs = documentRepository
                .findByOrgIdAndMemberIdOrderByCreatedAtDesc(principal.getOrgId(), userId);
        Map<UUID, User> uploaders = userRepository
                .findAllById(docs.stream().map(MemberDocument::getUploadedBy).collect(Collectors.toSet()))
                .stream().collect(Collectors.toMap(User::getId, u -> u));

        List<MemberDocumentResponse> result = docs.stream()
                .map(d -> toResponse(d, uploaders.get(d.getUploadedBy())))
                .toList();
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @Operation(summary = "Add a document to a staff member's record")
    @PostMapping
    public ResponseEntity<ApiResponse<MemberDocumentResponse>> upload(
            @PathVariable UUID userId,
            @RequestParam("file") MultipartFile file,
            @RequestParam("category") String categoryRaw,
            @RequestParam(value = "title", required = false) String title,
            @RequestParam(value = "notes", required = false) String notes,
            @AuthenticationPrincipal UserPrincipal principal) throws IOException {

        User member = requireMemberInOrg(userId, principal);

        MemberDocumentCategory category;
        try {
            category = MemberDocumentCategory.valueOf(categoryRaw.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Unknown document category");
        }

        if (file == null || file.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Choose a file to upload");
        }
        if (file.getSize() > MAX_FILE_BYTES) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "File is too large — the limit is 25 MB");
        }
        if (file.getContentType() == null || !SUPPORTED_TYPES.contains(file.getContentType())) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "Unsupported file type — PDF, Word, Excel, PowerPoint, text and image files (JPEG, PNG, WebP, GIF, HEIC) are supported");
        }

        String fileName = StringUtils.hasText(file.getOriginalFilename()) ? file.getOriginalFilename() : "document";

        MemberDocument doc = new MemberDocument();
        doc.setOrgId(principal.getOrgId());
        doc.setMemberId(member.getId());
        doc.setUploadedBy(principal.getId());
        doc.setCategory(category);
        doc.setTitle(StringUtils.hasText(title) ? title.trim() : fileName);
        doc.setFileName(fileName);
        doc.setFileUrl(storageService.store(file, "member-documents/" + member.getId()));
        doc.setContentType(file.getContentType());
        doc.setFileSizeBytes(file.getSize());
        doc.setNotes(StringUtils.hasText(notes) ? notes.trim() : null);

        MemberDocument saved = documentRepository.save(doc);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(toResponse(saved, principal.getUser())));
    }

    @Operation(summary = "Remove a document from a staff member's record")
    @DeleteMapping("/{documentId}")
    public ResponseEntity<Void> delete(
            @PathVariable UUID userId,
            @PathVariable UUID documentId,
            @AuthenticationPrincipal UserPrincipal principal) {

        requireMemberInOrg(userId, principal);

        MemberDocument doc = documentRepository
                .findByIdAndOrgIdAndMemberId(documentId, principal.getOrgId(), userId)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found"));

        storageService.delete(doc.getFileUrl());
        documentRepository.delete(doc);
        return ResponseEntity.noContent().build();
    }

    private MemberDocumentResponse toResponse(MemberDocument d, User uploader) {
        String name = uploader != null ? (uploader.getFirstName() + " " + uploader.getLastName()).trim() : "Unknown";
        return MemberDocumentResponse.from(d, name, storageService.presign(d.getFileUrl(), Duration.ofHours(1)));
    }

    /** The member must belong to the caller's organisation — never another org's user. */
    private User requireMemberInOrg(UUID userId, UserPrincipal principal) {
        return userRepository.findById(userId)
                .filter(u -> principal.getOrgId() != null && principal.getOrgId().equals(u.getOrgId()))
                .orElseThrow(() -> new ResourceNotFoundException("Member not found"));
    }
}

package com.simplehearing.sharedmedia.controller;

import com.simplehearing.auth.security.UserPrincipal;
import com.simplehearing.common.dto.ApiResponse;
import com.simplehearing.common.exception.ApiException;
import com.simplehearing.common.exception.ResourceNotFoundException;
import com.simplehearing.patient.entity.Patient;
import com.simplehearing.patient.repository.PatientParentRepository;
import com.simplehearing.patient.repository.PatientRepository;
import com.simplehearing.patient.repository.TherapistPatientRepository;
import com.simplehearing.sharedmedia.dto.SharedMediaResponse;
import com.simplehearing.sharedmedia.entity.SharedMedia;
import com.simplehearing.sharedmedia.enums.SharedMediaDirection;
import com.simplehearing.sharedmedia.repository.SharedMediaRepository;
import com.simplehearing.storage.StorageService;
import com.simplehearing.user.entity.User;
import com.simplehearing.user.enums.Role;
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
import java.util.UUID;
import java.util.stream.Collectors;

@Tag(name = "Shared Media", description = "Videos and notes shared between parents and the care team for a patient")
@RestController
@RequestMapping("/api/v1/patients/{patientId}/shared-media")
public class SharedMediaController {

    private final SharedMediaRepository sharedMediaRepository;
    private final PatientRepository patientRepository;
    private final PatientParentRepository patientParentRepository;
    private final TherapistPatientRepository therapistPatientRepository;
    private final UserRepository userRepository;
    private final StorageService storageService;

    public SharedMediaController(SharedMediaRepository sharedMediaRepository,
                                  PatientRepository patientRepository,
                                  PatientParentRepository patientParentRepository,
                                  TherapistPatientRepository therapistPatientRepository,
                                  UserRepository userRepository,
                                  StorageService storageService) {
        this.sharedMediaRepository = sharedMediaRepository;
        this.patientRepository = patientRepository;
        this.patientParentRepository = patientParentRepository;
        this.therapistPatientRepository = therapistPatientRepository;
        this.userRepository = userRepository;
        this.storageService = storageService;
    }

    @Operation(summary = "List videos/notes shared between the parent and the care team for a patient")
    @GetMapping
    @PreAuthorize("hasAnyRole('BUSINESS_OWNER', 'CLINIC_HEAD', 'THERAPIST', 'PARENT')")
    public ResponseEntity<ApiResponse<List<SharedMediaResponse>>> list(
            @PathVariable UUID patientId,
            @AuthenticationPrincipal UserPrincipal principal) {

        Patient patient = requireAccessible(patientId, principal);

        List<SharedMedia> items = sharedMediaRepository.findByPatientIdOrderByCreatedAtDesc(patient.getId());

        Map<UUID, User> uploaders = userRepository
                .findAllById(items.stream().map(SharedMedia::getUploadedBy).collect(Collectors.toSet()))
                .stream().collect(Collectors.toMap(User::getId, u -> u));

        List<SharedMediaResponse> result = items.stream().map(m -> {
            User uploader = uploaders.get(m.getUploadedBy());
            String uploaderName = uploader != null ? uploader.getFirstName() + " " + uploader.getLastName() : "Unknown";
            Role uploaderRole = uploader != null ? uploader.getRole() : null;
            String presignedUrl = m.getFileUrl() != null ? storageService.presign(m.getFileUrl(), Duration.ofHours(1)) : null;
            return SharedMediaResponse.from(m, uploaderName, uploaderRole, presignedUrl);
        }).toList();

        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @Operation(summary = "Share a video and/or a note for a patient — video is optional so a note can be shared alone")
    @PostMapping
    @PreAuthorize("hasAnyRole('BUSINESS_OWNER', 'CLINIC_HEAD', 'THERAPIST', 'PARENT')")
    public ResponseEntity<ApiResponse<SharedMediaResponse>> upload(
            @PathVariable UUID patientId,
            @RequestParam(value = "video", required = false) MultipartFile video,
            @RequestParam(value = "note", required = false) String note,
            @AuthenticationPrincipal UserPrincipal principal) throws IOException {

        Patient patient = requireAccessible(patientId, principal);

        boolean hasVideo = video != null && !video.isEmpty();
        boolean hasNote = StringUtils.hasText(note);
        if (!hasVideo && !hasNote) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Add a video, a note, or both");
        }
        if (hasVideo && (video.getContentType() == null || !video.getContentType().startsWith("video/"))) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Only video files are supported");
        }

        User actor = principal.getUser();
        SharedMediaDirection direction = actor.hasRole(Role.PARENT)
                ? SharedMediaDirection.PARENT_TO_CLINIC
                : SharedMediaDirection.CLINIC_TO_PARENT;

        SharedMedia media = new SharedMedia();
        media.setOrgId(patient.getOrgId());
        media.setPatientId(patient.getId());
        media.setUploadedBy(principal.getId());
        media.setDirection(direction);
        media.setNote(hasNote ? note : null);

        if (hasVideo) {
            String url = storageService.store(video, "shared-media/" + patientId);
            media.setFileName(video.getOriginalFilename() != null ? video.getOriginalFilename() : "video");
            media.setFileUrl(url);
            media.setContentType(video.getContentType());
            media.setFileSizeBytes(video.getSize());
        }

        SharedMedia saved = sharedMediaRepository.save(media);
        String presignedUrl = saved.getFileUrl() != null ? storageService.presign(saved.getFileUrl(), Duration.ofHours(1)) : null;

        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(
                SharedMediaResponse.from(saved, actor.getFirstName() + " " + actor.getLastName(), actor.getRole(), presignedUrl)));
    }

    @Operation(summary = "Delete a shared video/note — the uploader, or BUSINESS_OWNER/CLINIC_HEAD")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('BUSINESS_OWNER', 'CLINIC_HEAD', 'THERAPIST', 'PARENT')")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable UUID patientId,
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal) {

        requireAccessible(patientId, principal);

        SharedMedia media = sharedMediaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Shared media not found"));
        if (!media.getPatientId().equals(patientId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Access denied");
        }

        User actor = principal.getUser();
        boolean isOwner = media.getUploadedBy().equals(principal.getId());
        boolean isAdmin = actor.hasRole(Role.BUSINESS_OWNER) || actor.hasRole(Role.CLINIC_HEAD);
        if (!isOwner && !isAdmin) {
            throw new ApiException(HttpStatus.FORBIDDEN, "You can only delete your own posts");
        }

        if (media.getFileUrl() != null) {
            storageService.delete(media.getFileUrl());
        }
        sharedMediaRepository.delete(media);

        return ResponseEntity.noContent().build();
    }

    // ── Access control ──────────────────────────────────────────────────────────

    private Patient requireAccessible(UUID patientId, UserPrincipal principal) {
        Patient patient = patientRepository.findByIdAndOrgId(patientId, principal.getOrgId())
                .orElseThrow(() -> new ResourceNotFoundException("Patient", patientId));

        User actor = principal.getUser();
        if (actor.hasRole(Role.PARENT)) {
            boolean linked = patientParentRepository.findById_PatientId(patientId).stream()
                    .anyMatch(pp -> pp.getId().getParentId().equals(principal.getId()));
            if (!linked) {
                throw new ApiException(HttpStatus.FORBIDDEN, "You are not linked to this patient");
            }
        } else if (actor.hasRole(Role.THERAPIST)) {
            boolean assigned = therapistPatientRepository.findByPatientIdAndTherapistId(patientId, principal.getId())
                    .map(tp -> tp.isActive())
                    .orElse(false);
            if (!assigned) {
                throw new ApiException(HttpStatus.FORBIDDEN, "You are not assigned to this patient");
            }
        }
        // BUSINESS_OWNER / CLINIC_HEAD — org-wide access, no extra check.

        return patient;
    }
}

package com.simplehearing.sharedmedia.controller;

import com.simplehearing.auth.security.UserPrincipal;
import com.simplehearing.common.dto.ApiResponse;
import com.simplehearing.patient.entity.Patient;
import com.simplehearing.patient.entity.TherapistPatient;
import com.simplehearing.patient.repository.PatientParentRepository;
import com.simplehearing.patient.repository.PatientRepository;
import com.simplehearing.patient.repository.TherapistPatientRepository;
import com.simplehearing.sharedmedia.SharedMediaMapper;
import com.simplehearing.sharedmedia.dto.SharedMediaInboxResponse;
import com.simplehearing.sharedmedia.dto.SharedMediaResponse;
import com.simplehearing.sharedmedia.entity.SharedMedia;
import com.simplehearing.sharedmedia.enums.SharedMediaDirection;
import com.simplehearing.sharedmedia.repository.SharedMediaRepository;
import com.simplehearing.user.enums.Role;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Tag(name = "Shared Media", description = "Files (videos, images, documents) and notes shared between parents and the care team for a patient")
@RestController
@RequestMapping("/api/v1/shared-media")
public class SharedMediaInboxController {

    /** How many posts the dashboard card lists. */
    private static final int INBOX_LIMIT = 20;

    private final SharedMediaRepository sharedMediaRepository;
    private final PatientRepository patientRepository;
    private final PatientParentRepository patientParentRepository;
    private final TherapistPatientRepository therapistPatientRepository;
    private final SharedMediaMapper mapper;

    public SharedMediaInboxController(SharedMediaRepository sharedMediaRepository,
                                      PatientRepository patientRepository,
                                      PatientParentRepository patientParentRepository,
                                      TherapistPatientRepository therapistPatientRepository,
                                      SharedMediaMapper mapper) {
        this.sharedMediaRepository = sharedMediaRepository;
        this.patientRepository = patientRepository;
        this.patientParentRepository = patientParentRepository;
        this.therapistPatientRepository = therapistPatientRepository;
        this.mapper = mapper;
    }

    @Operation(summary = "The caller's dashboard notes card — a parent gets what the care team shared about their children; "
            + "an assigned therapist gets what parents shared about their cases. Unread first, then the latest read ones. Nobody else gets anything")
    @GetMapping("/inbox")
    @PreAuthorize("hasAnyRole('THERAPIST', 'PARENT')")
    public ResponseEntity<ApiResponse<SharedMediaInboxResponse>> inbox(@AuthenticationPrincipal UserPrincipal principal) {
        Role active = principal.getActiveRole();
        Set<UUID> patientIds;
        SharedMediaDirection direction;
        if (active == Role.PARENT) {
            patientIds = patientParentRepository.findById_ParentId(principal.getId()).stream()
                    .map(pp -> pp.getId().getPatientId()).collect(Collectors.toSet());
            direction = SharedMediaDirection.CLINIC_TO_PARENT;
        } else if (active == Role.THERAPIST) {
            patientIds = therapistPatientRepository.findByTherapistIdAndIsActive(principal.getId(), true).stream()
                    .map(TherapistPatient::getPatientId).collect(Collectors.toSet());
            direction = SharedMediaDirection.PARENT_TO_CLINIC;
        } else {
            return ResponseEntity.ok(ApiResponse.success(new SharedMediaInboxResponse(0, List.of())));
        }
        if (patientIds.isEmpty()) {
            return ResponseEntity.ok(ApiResponse.success(new SharedMediaInboxResponse(0, List.of())));
        }

        long unreadCount = sharedMediaRepository.countByPatientIdInAndDirectionAndReadAtIsNull(patientIds, direction);
        List<SharedMedia> posts = new ArrayList<>(sharedMediaRepository
                .findByPatientIdInAndDirectionAndReadAtIsNullOrderByCreatedAtDesc(patientIds, direction, PageRequest.of(0, INBOX_LIMIT)));
        if (posts.size() < INBOX_LIMIT) {
            posts.addAll(sharedMediaRepository.findByPatientIdInAndDirectionAndReadAtIsNotNullOrderByCreatedAtDesc(
                    patientIds, direction, PageRequest.of(0, INBOX_LIMIT - posts.size())));
        }

        Map<UUID, String> patientNames = patientRepository.findAllById(
                        posts.stream().map(SharedMedia::getPatientId).collect(Collectors.toSet())).stream()
                .collect(Collectors.toMap(Patient::getId, p -> p.getFirstName() + " " + p.getLastName()));

        List<SharedMediaResponse> items = mapper.toResponses(posts, patientNames);
        return ResponseEntity.ok(ApiResponse.success(new SharedMediaInboxResponse((int) unreadCount, items)));
    }
}

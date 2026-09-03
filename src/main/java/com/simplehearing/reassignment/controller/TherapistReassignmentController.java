package com.simplehearing.reassignment.controller;

import com.simplehearing.auth.security.UserPrincipal;
import com.simplehearing.common.dto.ApiResponse;
import com.simplehearing.common.exception.ApiException;
import com.simplehearing.common.exception.ResourceNotFoundException;
import com.simplehearing.reassignment.dto.CreateReassignmentRequest;
import com.simplehearing.reassignment.dto.ReassignmentResponse;
import com.simplehearing.reassignment.entity.TherapistReassignment;
import com.simplehearing.reassignment.enums.ReassignmentStatus;
import com.simplehearing.reassignment.enums.ReassignmentType;
import com.simplehearing.reassignment.repository.TherapistReassignmentRepository;
import com.simplehearing.reassignment.service.TherapistReassignmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Tag(name = "Therapist Reassignments", description = "Bulk-move a therapist's cases to another therapist")
@RestController
@RequestMapping("/api/v1/therapist-reassignments")
@PreAuthorize("hasAnyRole('CLINIC_HEAD', 'BUSINESS_OWNER', 'OFFICE_ADMIN')")
public class TherapistReassignmentController {

    private final TherapistReassignmentService reassignmentService;
    private final TherapistReassignmentRepository reassignmentRepository;

    public TherapistReassignmentController(TherapistReassignmentService reassignmentService,
                                           TherapistReassignmentRepository reassignmentRepository) {
        this.reassignmentService = reassignmentService;
        this.reassignmentRepository = reassignmentRepository;
    }

    @Operation(
        summary = "Bulk-reassign selected cases to another therapist",
        description = "Permanent, or bounded to a start/end window that hands the cases back "
                    + "automatically. Moves the scheduled sessions, upcoming review meetings and "
                    + "active IEP plans for each case's active enrollment(s) — work already "
                    + "completed keeps whichever therapist actually did it."
    )
    @PostMapping
    public ResponseEntity<ApiResponse<ReassignmentResponse>> create(
            @Valid @RequestBody CreateReassignmentRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {

        TherapistReassignment saved = reassignmentService.create(request, principal);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(reassignmentService.enrich(saved)));
    }

    @Operation(summary = "List reassignment batches a therapist appears in, either side",
               description = "Optionally filter by status.")
    @GetMapping
    public ResponseEntity<ApiResponse<List<ReassignmentResponse>>> list(
            @RequestParam UUID therapistId,
            @RequestParam(required = false) ReassignmentStatus status,
            @AuthenticationPrincipal UserPrincipal principal) {

        List<TherapistReassignment> batches = reassignmentService
                .findForTherapist(principal.getOrgId(), therapistId).stream()
                .filter(b -> status == null || b.getStatus() == status)
                .toList();

        return ResponseEntity.ok(ApiResponse.success(reassignmentService.enrich(batches)));
    }

    @Operation(
        summary = "End a temporary reassignment early",
        description = "Only for TEMPORARY batches still ACTIVE — a permanent reassignment can't "
                    + "be reversed this way; reassign again by hand instead."
    )
    @PatchMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<ReassignmentResponse>> cancel(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal) {

        TherapistReassignment batch = reassignmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reassignment not found"));
        if (!batch.getOrgId().equals(principal.getOrgId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Access denied");
        }
        if (batch.getStatus() != ReassignmentStatus.ACTIVE) {
            throw new ApiException(HttpStatus.CONFLICT, "This reassignment is no longer active");
        }
        if (batch.getType() != ReassignmentType.TEMPORARY) {
            throw new ApiException(HttpStatus.CONFLICT,
                    "A permanent reassignment can't be cancelled this way — reassign again by hand");
        }

        TherapistReassignment reverted = reassignmentService.revert(batch, true, principal.getId());
        return ResponseEntity.ok(ApiResponse.success(reassignmentService.enrich(reverted)));
    }
}

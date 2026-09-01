package com.simplehearing.casehistory.controller;

import com.simplehearing.auth.security.UserPrincipal;
import com.simplehearing.casehistory.dto.CaseHistoryResponse;
import com.simplehearing.casehistory.dto.UpdateCaseHistoryRequest;
import com.simplehearing.casehistory.service.CaseHistoryService;
import com.simplehearing.common.dto.ApiResponse;
import com.simplehearing.common.exception.ApiException;
import com.simplehearing.patient.repository.PatientParentRepository;
import com.simplehearing.user.enums.Role;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Tag(name = "Case History", description = "Patient clinical intake record — birth, milestones, family and school history")
@RestController
@RequestMapping("/api/v1/patients/{patientId}/case-history")
public class CaseHistoryController {

    private final CaseHistoryService caseHistoryService;
    private final PatientParentRepository patientParentRepository;

    public CaseHistoryController(CaseHistoryService caseHistoryService, PatientParentRepository patientParentRepository) {
        this.caseHistoryService = caseHistoryService;
        this.patientParentRepository = patientParentRepository;
    }

    @Operation(summary = "Get a patient's case history — null data if none recorded yet")
    @GetMapping
    @PreAuthorize("hasAnyRole('BUSINESS_OWNER', 'CLINIC_HEAD', 'THERAPIST', 'PARENT', 'OFFICE_ADMIN')")
    public ResponseEntity<ApiResponse<CaseHistoryResponse>> get(
            @PathVariable UUID patientId,
            @AuthenticationPrincipal UserPrincipal principal) {
        requireViewable(patientId, principal);
        CaseHistoryResponse data = caseHistoryService.get(principal.getOrgId(), patientId);
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    @Operation(summary = "Create or update a patient's case history — saves the whole form at once")
    @PutMapping
    @PreAuthorize("hasAnyRole('BUSINESS_OWNER', 'CLINIC_HEAD', 'THERAPIST', 'OFFICE_ADMIN')")
    public ResponseEntity<ApiResponse<CaseHistoryResponse>> upsert(
            @PathVariable UUID patientId,
            @RequestBody UpdateCaseHistoryRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        CaseHistoryResponse data = caseHistoryService.upsert(principal.getOrgId(), patientId, request, principal.getId());
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    private void requireViewable(UUID patientId, UserPrincipal principal) {
        if (!principal.getUser().hasRole(Role.PARENT)) {
            return;
        }
        boolean linked = patientParentRepository.findById_PatientId(patientId).stream()
                .anyMatch(pp -> pp.getId().getParentId().equals(principal.getId()));
        if (!linked) {
            throw new ApiException(HttpStatus.FORBIDDEN, "You are not linked to this patient");
        }
    }
}

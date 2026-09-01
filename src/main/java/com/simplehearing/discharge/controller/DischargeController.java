package com.simplehearing.discharge.controller;

import com.simplehearing.auth.security.UserPrincipal;
import com.simplehearing.common.dto.ApiResponse;
import com.simplehearing.common.exception.ApiException;
import com.simplehearing.discharge.dto.CreateDischargeRequest;
import com.simplehearing.discharge.dto.DischargePreviewResponse;
import com.simplehearing.discharge.dto.DischargeRecordResponse;
import com.simplehearing.discharge.entity.DischargeRecord;
import com.simplehearing.discharge.service.DischargeService;
import com.simplehearing.patient.repository.PatientParentRepository;
import com.simplehearing.user.enums.Role;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Tag(name = "Discharge", description = "Discharge episodes, success criteria, and reports")
@RestController
@RequestMapping("/api/v1/patients/{patientId}/discharge")
public class DischargeController {

    private final DischargeService dischargeService;
    private final PatientParentRepository patientParentRepository;

    public DischargeController(DischargeService dischargeService, PatientParentRepository patientParentRepository) {
        this.dischargeService = dischargeService;
        this.patientParentRepository = patientParentRepository;
    }

    @Operation(summary = "Dry run — what discharging this patient right now would look like")
    @GetMapping("/preview")
    @PreAuthorize("hasAnyRole('BUSINESS_OWNER', 'CLINIC_HEAD')")
    public ResponseEntity<ApiResponse<DischargePreviewResponse>> preview(
            @PathVariable UUID patientId,
            @AuthenticationPrincipal UserPrincipal principal) {
        DischargePreviewResponse data = dischargeService.preview(principal.getOrgId(), patientId);
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    @Operation(summary = "Discharge a patient — closes every enrollment in their current episode")
    @PostMapping
    @PreAuthorize("hasAnyRole('BUSINESS_OWNER', 'CLINIC_HEAD')")
    public ResponseEntity<ApiResponse<DischargeRecordResponse>> create(
            @PathVariable UUID patientId,
            @RequestBody(required = false) CreateDischargeRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        DischargeRecordResponse data = dischargeService.createDischarge(
                principal.getOrgId(), patientId,
                request != null ? request : new CreateDischargeRequest(null),
                principal.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(data));
    }

    @Operation(summary = "List a patient's discharge episodes, most recent first")
    @GetMapping
    @PreAuthorize("hasAnyRole('BUSINESS_OWNER', 'CLINIC_HEAD', 'THERAPIST', 'PARENT')")
    public ResponseEntity<ApiResponse<List<DischargeRecordResponse>>> list(
            @PathVariable UUID patientId,
            @AuthenticationPrincipal UserPrincipal principal) {
        requireViewable(patientId, principal);
        return ResponseEntity.ok(ApiResponse.success(dischargeService.list(principal.getOrgId(), patientId)));
    }

    @Operation(summary = "One discharge episode's report")
    @GetMapping("/{dischargeId}")
    @PreAuthorize("hasAnyRole('BUSINESS_OWNER', 'CLINIC_HEAD', 'THERAPIST', 'PARENT')")
    public ResponseEntity<ApiResponse<DischargeRecordResponse>> get(
            @PathVariable UUID patientId,
            @PathVariable UUID dischargeId,
            @AuthenticationPrincipal UserPrincipal principal) {
        requireViewable(patientId, principal);
        DischargeRecord record = dischargeService.getInOrg(principal.getOrgId(), dischargeId);
        if (!record.getPatientId().equals(patientId)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Discharge record not found for this patient");
        }
        return ResponseEntity.ok(ApiResponse.success(dischargeService.getResponseInOrg(principal.getOrgId(), dischargeId)));
    }

    @Operation(summary = "Download this discharge episode's PDF report",
               description = "Generates the PDF on first call, then always returns a fresh short-lived URL.")
    @GetMapping("/{dischargeId}/pdf")
    @PreAuthorize("hasAnyRole('BUSINESS_OWNER', 'CLINIC_HEAD', 'THERAPIST', 'PARENT')")
    public ResponseEntity<ApiResponse<java.util.Map<String, String>>> pdf(
            @PathVariable UUID patientId,
            @PathVariable UUID dischargeId,
            @AuthenticationPrincipal UserPrincipal principal) {
        requireViewable(patientId, principal);
        DischargeRecord record = dischargeService.getInOrg(principal.getOrgId(), dischargeId);
        if (!record.getPatientId().equals(patientId)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Discharge record not found for this patient");
        }
        String url = dischargeService.getOrGeneratePdfUrl(principal.getOrgId(), dischargeId);
        return ResponseEntity.ok(ApiResponse.success(java.util.Map.of("url", url)));
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

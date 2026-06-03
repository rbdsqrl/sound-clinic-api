package com.simplehearing.iep.controller;

import com.simplehearing.auth.security.UserPrincipal;
import com.simplehearing.common.dto.ApiResponse;
import com.simplehearing.iep.dto.*;
import com.simplehearing.iep.service.IEPTemplateService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/iep/templates")
public class IEPTemplateController {

    private final IEPTemplateService templateService;

    public IEPTemplateController(IEPTemplateService templateService) {
        this.templateService = templateService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('BUSINESS_OWNER','ADMIN','OFFICE_ADMIN','THERAPIST','DOCTOR')")
    public ResponseEntity<ApiResponse<List<IEPTemplateResponse>>> listTemplates(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success(templateService.listTemplates(principal)));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('BUSINESS_OWNER','ADMIN')")
    public ResponseEntity<ApiResponse<IEPTemplateResponse>> createTemplate(
            @Valid @RequestBody CreateIEPTemplateRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        IEPTemplateResponse response = templateService.createTemplate(request, principal);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('BUSINESS_OWNER','ADMIN')")
    public ResponseEntity<ApiResponse<IEPTemplateResponse>> updateTemplate(
            @PathVariable UUID id,
            @RequestBody UpdateIEPTemplateRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        IEPTemplateResponse response = templateService.updateTemplate(id, request, principal);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('BUSINESS_OWNER','ADMIN')")
    public ResponseEntity<Void> deleteTemplate(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal) {
        templateService.deleteTemplate(id, principal);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{templateId}/goals")
    @PreAuthorize("hasAnyRole('BUSINESS_OWNER','ADMIN')")
    public ResponseEntity<ApiResponse<IEPTemplateResponse>> addGoal(
            @PathVariable UUID templateId,
            @Valid @RequestBody CreateIEPTemplateGoalRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        IEPTemplateResponse response = templateService.addGoal(templateId, request, principal);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    @DeleteMapping("/goals/{goalId}")
    @PreAuthorize("hasAnyRole('BUSINESS_OWNER','ADMIN')")
    public ResponseEntity<Void> deleteGoal(
            @PathVariable UUID goalId,
            @AuthenticationPrincipal UserPrincipal principal) {
        templateService.deleteGoal(goalId, principal);
        return ResponseEntity.noContent().build();
    }
}

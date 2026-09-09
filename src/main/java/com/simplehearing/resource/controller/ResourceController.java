package com.simplehearing.resource.controller;

import com.simplehearing.auth.security.UserPrincipal;
import com.simplehearing.common.dto.ApiResponse;
import com.simplehearing.common.exception.ApiException;
import com.simplehearing.patient.repository.PatientParentRepository;
import com.simplehearing.resource.dto.*;
import com.simplehearing.resource.service.ResourceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Tag(name = "Resources", description = "Org-wide folder of external activities/worksheets — links, videos, images")
@RestController
@RequestMapping("/api/v1/resources")
public class ResourceController {

    /** Who can assign a library item to a patient — Admin Roles plus the treating Therapist. */
    private static final String ASSIGN_ROLES = "'BUSINESS_OWNER', 'CLINIC_HEAD', 'OFFICE_ADMIN', 'THERAPIST'";

    private final ResourceService resourceService;
    private final PatientParentRepository patientParentRepository;

    public ResourceController(ResourceService resourceService, PatientParentRepository patientParentRepository) {
        this.resourceService = resourceService;
        this.patientParentRepository = patientParentRepository;
    }

    @Operation(summary = "Browse a folder — its breadcrumb, subfolders, and resources. Omit folderId for the root. " +
            "Pass search to instead get a flat, folder-less list of name matches across the whole org.")
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<ResourceFolderContentsResponse>> browse(
            @RequestParam(required = false) UUID folderId,
            @RequestParam(required = false) String search,
            @AuthenticationPrincipal UserPrincipal principal) {

        return ResponseEntity.ok(ApiResponse.success(resourceService.getContents(principal.getOrgId(), folderId, search)));
    }

    @Operation(summary = "Assign a resource to a patient — this is what makes it visible in the Parent app")
    @PostMapping("/{id}/assign")
    @PreAuthorize("hasAnyRole(" + ASSIGN_ROLES + ")")
    public ResponseEntity<ApiResponse<ResourceAssignmentResponse>> assign(
            @PathVariable UUID id,
            @Valid @RequestBody AssignResourceRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {

        ResourceAssignmentResponse assigned = resourceService.assignToPatient(principal.getOrgId(), id, principal.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(assigned));
    }

    @Operation(summary = "Unassign a resource from a patient")
    @DeleteMapping("/assignments/{id}")
    @PreAuthorize("hasAnyRole(" + ASSIGN_ROLES + ")")
    public ResponseEntity<ApiResponse<Void>> unassign(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal) {

        resourceService.unassign(principal.getOrgId(), id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "List resources assigned to one patient — staff can view any patient in their org; " +
            "a Parent only their own linked child")
    @GetMapping("/assignments")
    @PreAuthorize("hasAnyRole(" + ASSIGN_ROLES + ", 'PARENT')")
    public ResponseEntity<ApiResponse<List<ResourceAssignmentResponse>>> listAssignments(
            @RequestParam UUID patientId,
            @AuthenticationPrincipal UserPrincipal principal) {

        requirePatientAccess(patientId, principal);
        return ResponseEntity.ok(ApiResponse.success(resourceService.listForPatient(principal.getOrgId(), patientId)));
    }

    private void requirePatientAccess(UUID patientId, UserPrincipal principal) {
        boolean isParent = principal.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_PARENT"));
        if (!isParent) return;
        boolean linked = patientParentRepository.findById_ParentId(principal.getId()).stream()
                .anyMatch(pp -> pp.getId().getPatientId().equals(patientId));
        if (!linked) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Access denied");
        }
    }

    @Operation(summary = "Create a folder — top-level if parentFolderId is omitted")
    @PostMapping("/folders")
    @PreAuthorize("hasAnyRole('BUSINESS_OWNER', 'CLINIC_HEAD', 'OFFICE_ADMIN')")
    public ResponseEntity<ApiResponse<ResourceFolderResponse>> createFolder(
            @Valid @RequestBody CreateResourceFolderRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {

        ResourceFolderResponse created = resourceService.createFolder(principal.getOrgId(), principal.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(created));
    }

    @Operation(summary = "Rename a folder")
    @PatchMapping("/folders/{id}")
    @PreAuthorize("hasAnyRole('BUSINESS_OWNER', 'CLINIC_HEAD', 'OFFICE_ADMIN')")
    public ResponseEntity<ApiResponse<ResourceFolderResponse>> renameFolder(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateResourceFolderRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {

        return ResponseEntity.ok(ApiResponse.success(resourceService.renameFolder(principal.getOrgId(), id, request)));
    }

    @Operation(summary = "Delete a folder — cascades to its subfolders and resources")
    @DeleteMapping("/folders/{id}")
    @PreAuthorize("hasAnyRole('BUSINESS_OWNER', 'CLINIC_HEAD', 'OFFICE_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteFolder(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal) {

        resourceService.deleteFolder(principal.getOrgId(), id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Add a resource (link, video, or image) — placed at the root if folderId is omitted")
    @PostMapping
    @PreAuthorize("hasAnyRole('BUSINESS_OWNER', 'CLINIC_HEAD', 'OFFICE_ADMIN')")
    public ResponseEntity<ApiResponse<ResourceResponse>> createResource(
            @Valid @RequestBody CreateResourceRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {

        ResourceResponse created = resourceService.createResource(principal.getOrgId(), principal.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(created));
    }

    @Operation(summary = "Update a resource's name, type, or URL")
    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('BUSINESS_OWNER', 'CLINIC_HEAD', 'OFFICE_ADMIN')")
    public ResponseEntity<ApiResponse<ResourceResponse>> updateResource(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateResourceRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {

        return ResponseEntity.ok(ApiResponse.success(resourceService.updateResource(principal.getOrgId(), id, request)));
    }

    @Operation(summary = "Delete a resource")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('BUSINESS_OWNER', 'CLINIC_HEAD', 'OFFICE_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteResource(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal) {

        resourceService.deleteResource(principal.getOrgId(), id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Upload a file (any type) and get back a URL to use as a resource's URL — lets Add/Edit Resource offer 'upload a file' alongside 'paste a link'")
    @PostMapping("/upload")
    @PreAuthorize("hasAnyRole('BUSINESS_OWNER', 'CLINIC_HEAD', 'OFFICE_ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, String>>> upload(
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal UserPrincipal principal) {

        String url = resourceService.uploadFile(principal.getOrgId(), file);
        return ResponseEntity.ok(ApiResponse.success(Map.of("url", url)));
    }

    @Operation(summary = "Bulk-import folders/resources from a CSV (columns: folder_path,name,type,url). " +
            "Not wired into any UI — call directly against the org/environment you intend to seed.")
    @PostMapping("/import")
    @PreAuthorize("hasAnyRole('BUSINESS_OWNER', 'CLINIC_HEAD', 'OFFICE_ADMIN')")
    public ResponseEntity<ApiResponse<ImportResourcesResponse>> importCsv(
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal UserPrincipal principal) {

        return ResponseEntity.ok(ApiResponse.success(
                resourceService.importCsv(principal.getOrgId(), principal.getId(), file)));
    }
}

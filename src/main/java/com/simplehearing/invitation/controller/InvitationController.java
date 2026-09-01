package com.simplehearing.invitation.controller;

import com.simplehearing.auth.security.UserPrincipal;
import com.simplehearing.common.dto.ApiResponse;
import com.simplehearing.invitation.dto.AcceptInviteRequest;
import com.simplehearing.invitation.dto.InvitePreviewResponse;
import com.simplehearing.invitation.dto.InviteRequest;
import com.simplehearing.invitation.dto.InviteResponse;
import com.simplehearing.invitation.service.InvitationService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/invitations")
public class InvitationController {

    private final InvitationService invitationService;

    public InvitationController(InvitationService invitationService) {
        this.invitationService = invitationService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('BUSINESS_OWNER', 'CLINIC_HEAD', 'OFFICE_ADMIN')")
    public ResponseEntity<ApiResponse<List<InviteResponse>>> list(
            @AuthenticationPrincipal UserPrincipal principal) {

        return ResponseEntity.ok(ApiResponse.success(null, invitationService.listForOrg(principal)));
    }

    /**
     * Send an invitation to a new staff or family member. BUSINESS_OWNER / CLINIC_HEAD may invite
     * any role; OFFICE_ADMIN is further restricted to non-leadership roles in the service layer.
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('BUSINESS_OWNER', 'CLINIC_HEAD', 'OFFICE_ADMIN')")
    public ResponseEntity<ApiResponse<InviteResponse>> invite(
            @Valid @RequestBody InviteRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {

        InviteResponse response = invitationService.invite(request, principal);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Invitation sent", response));
    }

    @PostMapping("/{id}/resend")
    @PreAuthorize("hasAnyRole('BUSINESS_OWNER', 'CLINIC_HEAD', 'OFFICE_ADMIN')")
    public ResponseEntity<ApiResponse<InviteResponse>> resend(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal) {

        InviteResponse response = invitationService.resend(id, principal.getOrgId());
        return ResponseEntity.ok(ApiResponse.success("Invitation resent", response));
    }

    @Operation(
        summary = "Cancel an invitation that was never accepted",
        description = "Withdraws a pending or expired invitation and invalidates its link. "
                    + "An invitation that has already been accepted cannot be cancelled — "
                    + "deactivate the member instead."
    )
    @PatchMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('BUSINESS_OWNER', 'CLINIC_HEAD', 'OFFICE_ADMIN')")
    public ResponseEntity<ApiResponse<InviteResponse>> cancel(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal) {

        InviteResponse response = invitationService.cancel(id, principal.getOrgId());
        return ResponseEntity.ok(ApiResponse.success("Invitation cancelled", response));
    }

    /**
     * Returns the email and role for a token — public, used to prefill the accept-invite form.
     */
    @Operation(summary = "Preview an invitation", security = {})
    @GetMapping("/preview")
    public ResponseEntity<ApiResponse<InvitePreviewResponse>> preview(
            @RequestParam String token) {
        return ResponseEntity.ok(ApiResponse.success(invitationService.preview(token)));
    }

    /**
     * Accept an invitation using the token sent in the invite link.
     * Public endpoint — the invited user has not yet registered.
     */
    @Operation(summary = "Accept an invitation and create the account", security = {})
    @PostMapping("/accept")
    public ResponseEntity<ApiResponse<Void>> accept(
            @Valid @RequestBody AcceptInviteRequest request) {

        invitationService.accept(request);
        return ResponseEntity.ok(ApiResponse.success("Account created successfully. You can now log in.", null));
    }
}

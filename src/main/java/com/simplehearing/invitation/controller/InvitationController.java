package com.simplehearing.invitation.controller;

import com.simplehearing.auth.security.UserPrincipal;
import com.simplehearing.common.dto.ApiResponse;
import com.simplehearing.invitation.dto.AcceptInviteRequest;
import com.simplehearing.invitation.dto.InviteRequest;
import com.simplehearing.invitation.dto.InviteResponse;
import com.simplehearing.invitation.service.InvitationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/invitations")
public class InvitationController {

    private final InvitationService invitationService;

    public InvitationController(InvitationService invitationService) {
        this.invitationService = invitationService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('BUSINESS_OWNER', 'ADMIN')")
    public ResponseEntity<ApiResponse<List<InviteResponse>>> list(
            @AuthenticationPrincipal UserPrincipal principal) {

        return ResponseEntity.ok(ApiResponse.success(null, invitationService.listForOrg(principal)));
    }

    /**
     * Send an invitation to a new PARENT or THERAPIST user.
     * Restricted to BUSINESS_OWNER role only.
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('BUSINESS_OWNER', 'ADMIN')")
    public ResponseEntity<ApiResponse<InviteResponse>> invite(
            @Valid @RequestBody InviteRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {

        InviteResponse response = invitationService.invite(request, principal);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Invitation sent", response));
    }

    /**
     * Accept an invitation using the token sent in the invite link.
     * Public endpoint — the invited user has not yet registered.
     */
    @PostMapping("/accept")
    public ResponseEntity<ApiResponse<Void>> accept(
            @Valid @RequestBody AcceptInviteRequest request) {

        invitationService.accept(request);
        return ResponseEntity.ok(ApiResponse.success("Account created successfully. You can now log in.", null));
    }
}

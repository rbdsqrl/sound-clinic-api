package com.simplehearing.dashboard.controller;

import com.simplehearing.auth.security.UserPrincipal;
import com.simplehearing.common.dto.ApiResponse;
import com.simplehearing.common.exception.ApiException;
import com.simplehearing.concern.enums.ConcernStatus;
import com.simplehearing.concern.repository.ConcernRepository;
import com.simplehearing.dashboard.dto.AttentionCountsResponse;
import com.simplehearing.dashboard.dto.OrgOverviewResponse;
import com.simplehearing.invitation.entity.Invitation;
import com.simplehearing.invitation.repository.InvitationRepository;
import com.simplehearing.patient.enums.PatientStage;
import com.simplehearing.patient.repository.PatientRepository;
import com.simplehearing.session.enums.TherapySessionStatus;
import com.simplehearing.session.repository.TherapySessionRepository;
import com.simplehearing.user.enums.Role;
import com.simplehearing.user.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@Tag(name = "Dashboard", description = "Lightweight rollups for the dashboard")
@RestController
@RequestMapping("/api/v1/dashboard")
public class DashboardController {

    /** Everyone who works at the clinic — as opposed to parents and patients. Mirrors UserController. */
    private static final List<Role> STAFF_ROLES = List.of(
            Role.CLINIC_HEAD, Role.BUSINESS_OWNER, Role.THERAPIST, Role.OFFICE_ADMIN);

    private final PatientRepository patientRepository;
    private final UserRepository userRepository;
    private final InvitationRepository invitationRepository;
    private final TherapySessionRepository sessionRepository;
    private final ConcernRepository concernRepository;

    public DashboardController(PatientRepository patientRepository,
                               UserRepository userRepository,
                               InvitationRepository invitationRepository,
                               TherapySessionRepository sessionRepository,
                               ConcernRepository concernRepository) {
        this.sessionRepository = sessionRepository;
        this.concernRepository = concernRepository;
        this.patientRepository = patientRepository;
        this.userRepository = userRepository;
        this.invitationRepository = invitationRepository;
    }

    @Operation(summary = "Active/inactive case counts and active/invited member counts — the Organisation Overview rings")
    @PreAuthorize("hasAnyRole('BUSINESS_OWNER', 'CLINIC_HEAD', 'OFFICE_ADMIN')")
    @GetMapping("/org-overview")
    public ResponseEntity<ApiResponse<OrgOverviewResponse>> orgOverview(@AuthenticationPrincipal UserPrincipal principal) {
        UUID orgId = principal.getOrgId();
        if (orgId == null) {
            throw new ApiException(HttpStatus.FORBIDDEN, "No organisation on the current account");
        }

        long totalCases = patientRepository.countByOrgId(orgId);
        long activeCases = patientRepository.countByOrgIdAndStageNotAndIsActive(orgId, PatientStage.DISCHARGED, true);
        long activeMembers = userRepository.countByOrgIdAndRoleInAndIsActive(orgId, STAFF_ROLES, true);
        long invitedMembers = invitationRepository.countByOrgIdAndStatus(orgId, Invitation.Status.PENDING);

        return ResponseEntity.ok(ApiResponse.success(new OrgOverviewResponse(
                (int) activeCases, (int) (totalCases - activeCases), (int) activeMembers, (int) invitedMembers)));
    }

    @Operation(summary = "Counts behind the dashboard's needs-attention cards — sessions to reschedule, cancellation requests, open concerns")
    @PreAuthorize("hasAnyRole('BUSINESS_OWNER', 'CLINIC_HEAD', 'OFFICE_ADMIN')")
    @GetMapping("/attention-counts")
    public ResponseEntity<ApiResponse<AttentionCountsResponse>> attentionCounts(@AuthenticationPrincipal UserPrincipal principal) {
        UUID orgId = principal.getOrgId();
        if (orgId == null) {
            throw new ApiException(HttpStatus.FORBIDDEN, "No organisation on the current account");
        }
        return ResponseEntity.ok(ApiResponse.success(new AttentionCountsResponse(
                (int) sessionRepository.countByOrgIdAndStatus(orgId, TherapySessionStatus.PENDING_RESCHEDULE),
                (int) sessionRepository.countByOrgIdAndStatus(orgId, TherapySessionStatus.CANCELLATION_REQUESTED),
                concernRepository.countByOrgIdAndStatus(orgId, ConcernStatus.OPEN))));
    }
}

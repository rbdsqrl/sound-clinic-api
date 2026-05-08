package com.simplehearing.leave.controller;

import com.simplehearing.auth.security.UserPrincipal;
import com.simplehearing.common.dto.ApiResponse;
import com.simplehearing.common.exception.ApiException;
import com.simplehearing.common.exception.ResourceNotFoundException;
import com.simplehearing.leave.dto.CreateLeaveRequest;
import com.simplehearing.leave.dto.LeaveResponse;
import com.simplehearing.leave.dto.ReviewLeaveRequest;
import com.simplehearing.leave.entity.Leave;
import com.simplehearing.leave.enums.LeaveStatus;
import com.simplehearing.leave.repository.LeaveRepository;
import com.simplehearing.notification.EmailService;
import com.simplehearing.user.entity.User;
import com.simplehearing.user.enums.Role;
import com.simplehearing.user.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Tag(name = "Leave", description = "Leave requests and approvals")
@RestController
@RequestMapping("/api/v1/leaves")
public class LeaveController {

    private final LeaveRepository leaveRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;

    public LeaveController(LeaveRepository leaveRepository, UserRepository userRepository,
                           EmailService emailService) {
        this.leaveRepository = leaveRepository;
        this.userRepository = userRepository;
        this.emailService = emailService;
    }

    // ── Apply for leave (therapist / doctor) ─────────────────────────────────

    @Operation(summary = "Apply for a leave day")
    @PostMapping
    @PreAuthorize("hasAnyRole('THERAPIST', 'DOCTOR')")
    public ResponseEntity<ApiResponse<LeaveResponse>> apply(
            @Valid @RequestBody CreateLeaveRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {

        Leave leave = new Leave();
        leave.setOrgId(principal.getOrgId());
        leave.setTherapistId(principal.getId());
        leave.setLeaveDate(request.leaveDate());
        leave.setLeaveType(request.leaveType());
        leave.setReason(request.reason());
        leave.setStatus(LeaveStatus.PENDING);

        Leave saved = leaveRepository.save(leave);
        User therapist = principal.getUser();
        LeaveResponse response = LeaveResponse.from(saved,
                therapist.getFirstName(), therapist.getLastName(),
                null, null);

        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    // ── List leaves ───────────────────────────────────────────────────────────

    @Operation(summary = "List leaves — business owner sees all; therapist sees own")
    @GetMapping
    @PreAuthorize("hasAnyRole('BUSINESS_OWNER', 'ADMIN', 'THERAPIST', 'DOCTOR')")
    public ResponseEntity<ApiResponse<List<LeaveResponse>>> list(
            @RequestParam(required = false) LeaveStatus status,
            @AuthenticationPrincipal UserPrincipal principal) {

        User caller = principal.getUser();
        List<Leave> leaves;

        boolean isManager = caller.getRole() == Role.BUSINESS_OWNER || caller.getRole() == Role.ADMIN;

        if (isManager) {
            leaves = (status != null)
                    ? leaveRepository.findByOrgIdAndStatusOrderByLeaveDateDesc(principal.getOrgId(), status)
                    : leaveRepository.findByOrgIdOrderByLeaveDateDesc(principal.getOrgId());
        } else {
            leaves = leaveRepository.findByOrgIdAndTherapistIdOrderByLeaveDateDesc(
                    principal.getOrgId(), principal.getId());
        }

        // Collect all user IDs needed for name enrichment
        Set<UUID> userIds = leaves.stream()
                .flatMap(l -> {
                    Set<UUID> ids = new java.util.HashSet<>();
                    ids.add(l.getTherapistId());
                    if (l.getReviewedBy() != null) ids.add(l.getReviewedBy());
                    return ids.stream();
                })
                .collect(Collectors.toSet());

        Map<UUID, User> userMap = userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u));

        List<LeaveResponse> result = leaves.stream().map(l -> {
            User therapist = userMap.get(l.getTherapistId());
            User reviewer  = l.getReviewedBy() != null ? userMap.get(l.getReviewedBy()) : null;
            return LeaveResponse.from(l,
                    therapist != null ? therapist.getFirstName() : "",
                    therapist != null ? therapist.getLastName()  : "",
                    reviewer  != null ? reviewer.getFirstName()  : null,
                    reviewer  != null ? reviewer.getLastName()   : null);
        }).toList();

        return ResponseEntity.ok(ApiResponse.success(result));
    }

    // ── Review a leave request (approve / reject) ─────────────────────────────

    @Operation(summary = "Approve or reject a leave request")
    @PatchMapping("/{id}/review")
    @PreAuthorize("hasAnyRole('BUSINESS_OWNER', 'ADMIN')")
    public ResponseEntity<ApiResponse<LeaveResponse>> review(
            @PathVariable UUID id,
            @Valid @RequestBody ReviewLeaveRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {

        Leave leave = leaveRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Leave request not found"));

        if (!leave.getOrgId().equals(principal.getOrgId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Access denied");
        }

        if (leave.getStatus() != LeaveStatus.PENDING) {
            throw new ApiException(HttpStatus.CONFLICT, "Leave request has already been reviewed");
        }

        LeaveStatus newStatus = request.status();
        if (newStatus == LeaveStatus.PENDING) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Review status must be APPROVED or REJECTED");
        }

        leave.setStatus(newStatus);
        leave.setReviewedBy(principal.getId());
        leave.setReviewedAt(Instant.now());

        Leave saved = leaveRepository.save(leave);

        Set<UUID> userIds = new java.util.HashSet<>();
        userIds.add(saved.getTherapistId());
        userIds.add(saved.getReviewedBy());
        Map<UUID, User> userMap = userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u));

        User therapist = userMap.get(saved.getTherapistId());
        User reviewer  = userMap.get(saved.getReviewedBy());

        if (therapist != null) {
            String reviewerName = (reviewer != null)
                    ? reviewer.getFirstName() + " " + reviewer.getLastName()
                    : "Administrator";
            emailService.sendLeaveStatusEmail(
                    therapist.getEmail(),
                    therapist.getFirstName() + " " + therapist.getLastName(),
                    saved.getLeaveDate().toString(),
                    saved.getLeaveType().name(),
                    newStatus.name(),
                    reviewerName
            );
        }

        return ResponseEntity.ok(ApiResponse.success(LeaveResponse.from(saved,
                therapist != null ? therapist.getFirstName() : "",
                therapist != null ? therapist.getLastName()  : "",
                reviewer  != null ? reviewer.getFirstName()  : null,
                reviewer  != null ? reviewer.getLastName()   : null)));
    }

    // ── Cancel own pending leave ───────────────────────────────────────────────

    @Operation(summary = "Cancel a pending leave request (own leaves only)")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('THERAPIST', 'DOCTOR')")
    public ResponseEntity<Void> cancel(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal) {

        Leave leave = leaveRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Leave request not found"));

        if (!leave.getTherapistId().equals(principal.getId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "You can only cancel your own leave requests");
        }

        if (leave.getStatus() != LeaveStatus.PENDING) {
            throw new ApiException(HttpStatus.CONFLICT, "Only pending leave requests can be cancelled");
        }

        leaveRepository.delete(leave);
        return ResponseEntity.noContent().build();
    }
}

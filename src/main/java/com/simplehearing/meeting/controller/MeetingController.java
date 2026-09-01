package com.simplehearing.meeting.controller;

import com.simplehearing.auth.security.UserPrincipal;
import com.simplehearing.common.dto.ApiResponse;
import com.simplehearing.meeting.dto.*;
import com.simplehearing.meeting.service.MeetingService;
import com.simplehearing.user.enums.Role;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Tag(name = "Meetings", description = "General meetings with participants, scheduled from the calendar")
@RestController
@RequestMapping("/api/v1/meetings")
public class MeetingController {

    private final MeetingService meetingService;

    public MeetingController(MeetingService meetingService) {
        this.meetingService = meetingService;
    }

    @Operation(summary = "Schedule a meeting",
               description = "Staff only — parents and patients attend meetings but cannot create them. "
                           + "The organiser is added to the participant list automatically.")
    @PostMapping
    @PreAuthorize("hasAnyRole('BUSINESS_OWNER', 'CLINIC_HEAD', 'THERAPIST')")
    public ResponseEntity<ApiResponse<MeetingResponse>> create(
            @Valid @RequestBody CreateMeetingRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {

        MeetingResponse created = meetingService.create(request, principal.getOrgId(), principal.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(created));
    }

    @Operation(summary = "List meetings in a date range",
               description = "Admins see every meeting in the organisation; everyone else sees only the "
                           + "meetings they organised or were invited to.")
    @GetMapping
    public ResponseEntity<ApiResponse<List<MeetingResponse>>> list(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @AuthenticationPrincipal UserPrincipal principal) {

        boolean seesEverything = principal.getUser().getRole() == Role.BUSINESS_OWNER
                              || principal.getUser().getRole() == Role.CLINIC_HEAD;

        return ResponseEntity.ok(ApiResponse.success(
                meetingService.list(principal.getOrgId(), principal.getId(), seesEverything, from, to)));
    }

    @Operation(summary = "Get one meeting")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<MeetingResponse>> get(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal) {

        return ResponseEntity.ok(ApiResponse.success(meetingService.get(id, principal.getOrgId())));
    }

    @Operation(summary = "Cancel a meeting",
               description = "Sends a CANCEL calendar invite so the entry drops out of participants' calendars.")
    @PatchMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('BUSINESS_OWNER', 'CLINIC_HEAD', 'THERAPIST')")
    public ResponseEntity<ApiResponse<MeetingResponse>> cancel(
            @PathVariable UUID id,
            @RequestBody(required = false) CancelMeetingRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {

        String reason = request != null ? request.reason() : null;
        return ResponseEntity.ok(ApiResponse.success(
                meetingService.cancel(id, principal.getOrgId(), principal.getId(), reason)));
    }
}

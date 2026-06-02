package com.simplehearing.attendance.controller;

import com.simplehearing.attendance.dto.AttendanceResponse;
import com.simplehearing.attendance.dto.CheckInRequest;
import com.simplehearing.attendance.dto.CheckOutRequest;
import com.simplehearing.attendance.dto.EnrollFaceRequest;
import com.simplehearing.attendance.dto.VerifyAttendanceRequest;
import com.simplehearing.attendance.service.AttendanceService;
import com.simplehearing.auth.security.UserPrincipal;
import com.simplehearing.common.dto.ApiResponse;
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

@Tag(name = "Attendance", description = "Attendance check-in, check-out and management")
@RestController
@RequestMapping("/api/v1/attendance")
public class AttendanceController {

    private final AttendanceService attendanceService;

    public AttendanceController(AttendanceService attendanceService) {
        this.attendanceService = attendanceService;
    }

    @Operation(summary = "Check in with optional geo and face verification")
    @PostMapping("/check-in")
    @PreAuthorize("hasAnyRole('BUSINESS_OWNER','ADMIN','THERAPIST','DOCTOR','OFFICE_ADMIN','PATIENT')")
    public ResponseEntity<ApiResponse<AttendanceResponse>> checkIn(
            @Valid @RequestBody CheckInRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(attendanceService.checkIn(request, principal)));
    }

    @Operation(summary = "Check out")
    @PostMapping("/check-out")
    @PreAuthorize("hasAnyRole('BUSINESS_OWNER','ADMIN','THERAPIST','DOCTOR','OFFICE_ADMIN','PATIENT')")
    public ResponseEntity<ApiResponse<AttendanceResponse>> checkOut(
            @Valid @RequestBody CheckOutRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success(attendanceService.checkOut(request, principal)));
    }

    @Operation(summary = "Retry geo and face verification for today's record")
    @PatchMapping("/today/verify")
    @PreAuthorize("hasAnyRole('BUSINESS_OWNER','ADMIN','THERAPIST','DOCTOR','OFFICE_ADMIN','PATIENT')")
    public ResponseEntity<ApiResponse<AttendanceResponse>> verifyToday(
            @RequestBody VerifyAttendanceRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success(attendanceService.verifyToday(request, principal)));
    }

    @Operation(summary = "Get today's attendance record for the caller")
    @GetMapping("/today")
    @PreAuthorize("hasAnyRole('BUSINESS_OWNER','ADMIN','THERAPIST','DOCTOR','OFFICE_ADMIN','PATIENT')")
    public ResponseEntity<ApiResponse<AttendanceResponse>> today(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success(attendanceService.getToday(principal)));
    }

    @Operation(summary = "List caller's own attendance history")
    @GetMapping("/my")
    @PreAuthorize("hasAnyRole('BUSINESS_OWNER','ADMIN','THERAPIST','DOCTOR','OFFICE_ADMIN','PATIENT')")
    public ResponseEntity<ApiResponse<List<AttendanceResponse>>> listMine(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success(attendanceService.listMine(principal)));
    }

    @Operation(summary = "List all attendance records in the org (admin view)")
    @GetMapping
    @PreAuthorize("hasAnyRole('BUSINESS_OWNER','ADMIN')")
    public ResponseEntity<ApiResponse<List<AttendanceResponse>>> listAll(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success(
                attendanceService.listForOrg(principal.getOrgId(), from, to)));
    }

    @Operation(summary = "Enroll face descriptor for the caller (one-time setup)")
    @PostMapping("/enroll-face")
    @PreAuthorize("hasAnyRole('BUSINESS_OWNER','ADMIN','THERAPIST','DOCTOR','OFFICE_ADMIN','PATIENT')")
    public ResponseEntity<ApiResponse<Void>> enrollFace(
            @Valid @RequestBody EnrollFaceRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        attendanceService.enrollFace(request.faceDescriptor(), principal);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}

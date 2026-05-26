package com.simplehearing.attendance.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.simplehearing.attendance.dto.AttendanceResponse;
import com.simplehearing.attendance.dto.CheckInRequest;
import com.simplehearing.attendance.dto.CheckOutRequest;
import com.simplehearing.attendance.entity.Attendance;
import com.simplehearing.attendance.enums.AttendanceStatus;
import com.simplehearing.attendance.repository.AttendanceRepository;
import com.simplehearing.auth.security.UserPrincipal;
import com.simplehearing.clinic.entity.Clinic;
import com.simplehearing.clinic.repository.ClinicRepository;
import com.simplehearing.common.exception.ApiException;
import com.simplehearing.common.exception.ResourceNotFoundException;
import com.simplehearing.user.entity.User;
import com.simplehearing.user.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class AttendanceService {

    private static final double FACE_MATCH_THRESHOLD = 0.6;
    private static final double EARTH_RADIUS_METERS  = 6_371_000.0;

    private final AttendanceRepository attendanceRepository;
    private final ClinicRepository     clinicRepository;
    private final UserRepository       userRepository;
    private final ObjectMapper         objectMapper;

    public AttendanceService(AttendanceRepository attendanceRepository,
                             ClinicRepository clinicRepository,
                             UserRepository userRepository,
                             ObjectMapper objectMapper) {
        this.attendanceRepository = attendanceRepository;
        this.clinicRepository     = clinicRepository;
        this.userRepository       = userRepository;
        this.objectMapper         = objectMapper;
    }

    // ── Check-in ──────────────────────────────────────────────────────────────

    public AttendanceResponse checkIn(CheckInRequest request, UserPrincipal principal) {
        LocalDate today = LocalDate.now();

        if (attendanceRepository.findByUserIdAndAttendanceDate(principal.getId(), today).isPresent()) {
            throw new ApiException(HttpStatus.CONFLICT, "Already checked in today");
        }

        Clinic clinic = clinicRepository.findByIdAndOrgId(request.clinicId(), principal.getOrgId())
                .orElseThrow(() -> new ResourceNotFoundException("Clinic not found"));

        Attendance attendance = new Attendance();
        attendance.setOrgId(principal.getOrgId());
        attendance.setUserId(principal.getId());
        attendance.setClinicId(clinic.getId());
        attendance.setAttendanceDate(today);
        attendance.setCheckInTime(Instant.now());
        attendance.setCheckInLat(request.latitude());
        attendance.setCheckInLon(request.longitude());
        attendance.setGeoVerified(verifyGeoFence(request.latitude(), request.longitude(), clinic));
        attendance.setFaceVerified(verifyFace(request.faceDescriptor(), principal.getUser()));
        attendance.setStatus(AttendanceStatus.CHECKED_IN);

        Attendance saved = attendanceRepository.save(attendance);
        User user = principal.getUser();
        return AttendanceResponse.from(saved, user.getFirstName(), user.getLastName(), clinic.getName());
    }

    // ── Check-out ─────────────────────────────────────────────────────────────

    public AttendanceResponse checkOut(CheckOutRequest request, UserPrincipal principal) {
        LocalDate today = LocalDate.now();

        Attendance attendance = attendanceRepository
                .findByUserIdAndAttendanceDateAndStatus(principal.getId(), today, AttendanceStatus.CHECKED_IN)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "No active check-in found for today"));

        Clinic clinic = clinicRepository.findById(attendance.getClinicId())
                .orElseThrow(() -> new ResourceNotFoundException("Clinic not found"));

        attendance.setCheckOutTime(Instant.now());
        attendance.setCheckOutLat(request.latitude());
        attendance.setCheckOutLon(request.longitude());
        attendance.setStatus(AttendanceStatus.CHECKED_OUT);

        if (!attendance.isFaceVerified() && request.faceDescriptor() != null) {
            attendance.setFaceVerified(verifyFace(request.faceDescriptor(), principal.getUser()));
        }

        Attendance saved = attendanceRepository.save(attendance);
        User user = principal.getUser();
        return AttendanceResponse.from(saved, user.getFirstName(), user.getLastName(), clinic.getName());
    }

    // ── My attendance ─────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<AttendanceResponse> listMine(UserPrincipal principal) {
        List<Attendance> records = attendanceRepository.findByUserIdOrderByAttendanceDateDesc(principal.getId());
        return enrich(records);
    }

    // ── Today's record for the caller ─────────────────────────────────────────

    @Transactional(readOnly = true)
    public AttendanceResponse getToday(UserPrincipal principal) {
        return attendanceRepository
                .findByUserIdAndAttendanceDate(principal.getId(), LocalDate.now())
                .map(a -> enrich(List.of(a)).get(0))
                .orElse(null);
    }

    // ── All org attendance (admin view) ───────────────────────────────────────

    @Transactional(readOnly = true)
    public List<AttendanceResponse> listForOrg(UUID orgId, LocalDate from, LocalDate to) {
        LocalDate resolvedFrom = from != null ? from : LocalDate.now();
        LocalDate resolvedTo   = to   != null ? to   : LocalDate.now();
        List<Attendance> records = attendanceRepository
                .findByOrgIdAndAttendanceDateBetweenOrderByAttendanceDateDescCheckInTimeDesc(
                        orgId, resolvedFrom, resolvedTo);
        return enrich(records);
    }

    // ── Face enrollment ───────────────────────────────────────────────────────

    public void enrollFace(List<Double> descriptor, UserPrincipal principal) {
        try {
            User user = userRepository.findById(principal.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("User not found"));
            user.setFaceDescriptor(objectMapper.writeValueAsString(descriptor));
            userRepository.save(user);
        } catch (Exception e) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to save face descriptor");
        }
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private boolean verifyGeoFence(Double lat, Double lon, Clinic clinic) {
        if (lat == null || lon == null) return false;
        if (clinic.getLatitude() == null || clinic.getLongitude() == null) return true;
        double distance = haversineDistance(lat, lon, clinic.getLatitude(), clinic.getLongitude());
        int radius = clinic.getGeoFenceRadiusMeters() != null ? clinic.getGeoFenceRadiusMeters() : 200;
        return distance <= radius;
    }

    private double haversineDistance(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return EARTH_RADIUS_METERS * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    private boolean verifyFace(List<Double> submitted, User user) {
        if (submitted == null || submitted.isEmpty()) return false;
        if (user.getFaceDescriptor() == null) return false;
        try {
            List<Double> stored = objectMapper.readValue(
                    user.getFaceDescriptor(), new TypeReference<List<Double>>() {});
            if (stored.size() != submitted.size()) return false;
            double sum = 0;
            for (int i = 0; i < stored.size(); i++) {
                double diff = stored.get(i) - submitted.get(i);
                sum += diff * diff;
            }
            return Math.sqrt(sum) < FACE_MATCH_THRESHOLD;
        } catch (Exception e) {
            return false;
        }
    }

    private List<AttendanceResponse> enrich(List<Attendance> records) {
        Set<UUID> userIds   = records.stream().map(Attendance::getUserId).collect(Collectors.toSet());
        Set<UUID> clinicIds = records.stream().map(Attendance::getClinicId).collect(Collectors.toSet());

        Map<UUID, User>   userMap   = userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u));
        Map<UUID, Clinic> clinicMap = clinicRepository.findAllById(clinicIds).stream()
                .collect(Collectors.toMap(Clinic::getId, c -> c));

        return records.stream().map(a -> {
            User   u = userMap.get(a.getUserId());
            Clinic c = clinicMap.get(a.getClinicId());
            return AttendanceResponse.from(
                    a,
                    u != null ? u.getFirstName() : "",
                    u != null ? u.getLastName()  : "",
                    c != null ? c.getName()      : "");
        }).toList();
    }
}

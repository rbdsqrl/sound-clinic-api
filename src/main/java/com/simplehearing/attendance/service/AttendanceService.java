package com.simplehearing.attendance.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.simplehearing.attendance.dto.AttendanceResponse;
import com.simplehearing.attendance.dto.CheckInRequest;
import com.simplehearing.attendance.dto.CheckOutRequest;
import com.simplehearing.attendance.dto.GeoCheckResponse;
import com.simplehearing.attendance.dto.GeoCheckResponse.GeoCheckReferenceType;
import com.simplehearing.attendance.dto.VerifyAttendanceRequest;
import com.simplehearing.attendance.entity.Attendance;
import com.simplehearing.attendance.enums.AttendanceStatus;
import com.simplehearing.attendance.repository.AttendanceRepository;
import com.simplehearing.auth.security.UserPrincipal;
import com.simplehearing.clinic.entity.Clinic;
import com.simplehearing.clinic.repository.ClinicRepository;
import com.simplehearing.common.exception.ApiException;
import com.simplehearing.common.exception.ResourceNotFoundException;
import com.simplehearing.notification.EmailService;
import com.simplehearing.organisation.entity.Organisation;
import com.simplehearing.organisation.repository.OrganisationRepository;
import com.simplehearing.user.entity.User;
import com.simplehearing.user.enums.Role;
import com.simplehearing.user.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
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

    private final AttendanceRepository   attendanceRepository;
    private final ClinicRepository       clinicRepository;
    private final UserRepository         userRepository;
    private final OrganisationRepository organisationRepository;
    private final EmailService           emailService;
    private final ObjectMapper           objectMapper;

    public AttendanceService(AttendanceRepository attendanceRepository,
                             ClinicRepository clinicRepository,
                             UserRepository userRepository,
                             OrganisationRepository organisationRepository,
                             EmailService emailService,
                             ObjectMapper objectMapper) {
        this.attendanceRepository   = attendanceRepository;
        this.clinicRepository       = clinicRepository;
        this.userRepository         = userRepository;
        this.organisationRepository = organisationRepository;
        this.emailService           = emailService;
        this.objectMapper           = objectMapper;
    }

    // ── Check-in ──────────────────────────────────────────────────────────────

    public AttendanceResponse checkIn(CheckInRequest request, UserPrincipal principal) {
        LocalDate today = LocalDate.now();

        if (request.latitude() == null || request.longitude() == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Location is required for check-in");
        }

        User currentUser = principal.getUser();

        boolean faceVerified = false;
        boolean faceOverride = false;

        if (currentUser.getFaceDescriptor() != null) {
            if (request.faceDescriptor() == null || request.faceDescriptor().isEmpty()) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Face verification is required for check-in");
            }
            faceVerified = verifyFace(request.faceDescriptor(), currentUser);
            if (!faceVerified) {
                if (!request.forceCheckIn()) {
                    throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "FACE_MISMATCH");
                }
                faceOverride = true;
            }
        }

        // Where this check-in is — a clinic, or (Business Owner only) the organisation itself.
        Clinic clinic = requireClinicUnlessOrganisation(request.clinicId(), request.atOrganisation(), principal);
        Organisation org = organisationRepository.findById(principal.getOrgId()).orElse(null);
        String placeName = clinic != null ? clinic.getName() : (org != null ? org.getName() : "");

        Attendance attendance = attendanceRepository
                .findByUserIdAndAttendanceDate(principal.getId(), today)
                .orElseGet(Attendance::new);

        if (attendance.getId() != null && attendance.getStatus() == AttendanceStatus.CHECKED_IN) {
            throw new ApiException(HttpStatus.CONFLICT, "Already checked in today");
        }

        attendance.setOrgId(principal.getOrgId());
        attendance.setUserId(principal.getId());
        attendance.setClinicId(clinic != null ? clinic.getId() : null);
        attendance.setAtOrganisation(request.atOrganisation());
        attendance.setAttendanceDate(today);
        attendance.setCheckInTime(Instant.now());
        attendance.setCheckInLat(request.latitude());
        attendance.setCheckInLon(request.longitude());
        attendance.setCheckOutTime(null);
        attendance.setCheckOutLat(null);
        attendance.setCheckOutLon(null);
        GeoFenceTarget geoFenceTarget = resolveGeoFenceTarget(principal, clinic, request.atOrganisation());
        attendance.setGeoVerified(verifyGeoFence(request.latitude(), request.longitude(), geoFenceTarget));
        attendance.setFaceVerified(faceVerified);
        attendance.setFaceOverride(faceOverride);
        attendance.setStatus(AttendanceStatus.CHECKED_IN);

        Attendance saved = attendanceRepository.save(attendance);

        if (faceOverride) {
            List<User> businessOwners = userRepository.findByOrgIdAndRoleIn(
                    principal.getOrgId(), List.of(Role.BUSINESS_OWNER));
            List<String> recipients = businessOwners.stream()
                    .map(User::getEmail)
                    .collect(Collectors.toList());
            if (!recipients.isEmpty()) {
                String orgName = org != null ? org.getName() : "";
                String employeeName = currentUser.getFirstName() + " " + currentUser.getLastName();
                String checkInTime = DateTimeFormatter.ofPattern("HH:mm 'UTC'")
                        .withZone(ZoneOffset.UTC)
                        .format(saved.getCheckInTime());
                String attendanceDate = saved.getAttendanceDate().toString();
                emailService.sendUnverifiedCheckInEmail(
                        recipients,
                        employeeName,
                        currentUser.getEmail(),
                        checkInTime,
                        placeName,
                        attendanceDate,
                        orgName);
            }
        }

        return AttendanceResponse.from(saved, currentUser.getFirstName(), currentUser.getLastName(), placeName, null);
    }

    // ── Check-out ─────────────────────────────────────────────────────────────

    public AttendanceResponse checkOut(CheckOutRequest request, UserPrincipal principal) {
        LocalDate today = LocalDate.now();

        Attendance attendance = attendanceRepository
                .findByUserIdAndAttendanceDateAndStatus(principal.getId(), today, AttendanceStatus.CHECKED_IN)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "No active check-in found for today"));

        String placeName = placeNameOf(attendance);

        attendance.setCheckOutTime(Instant.now());
        attendance.setCheckOutLat(request.latitude());
        attendance.setCheckOutLon(request.longitude());
        attendance.setStatus(AttendanceStatus.CHECKED_OUT);

        if (!attendance.isFaceVerified() && request.faceDescriptor() != null) {
            attendance.setFaceVerified(verifyFace(request.faceDescriptor(), principal.getUser()));
        }

        Attendance saved = attendanceRepository.save(attendance);
        User user = principal.getUser();
        return AttendanceResponse.from(saved, user.getFirstName(), user.getLastName(), placeName, null);
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

    // ── Retry verification on today's record ─────────────────────────────────

    public AttendanceResponse verifyToday(VerifyAttendanceRequest request, UserPrincipal principal) {
        LocalDate today = LocalDate.now();
        Attendance attendance = attendanceRepository
                .findByUserIdAndAttendanceDate(principal.getId(), today)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "No attendance record found for today"));

        Clinic clinic = attendance.isAtOrganisation() ? null
                : clinicRepository.findById(attendance.getClinicId())
                        .orElseThrow(() -> new ResourceNotFoundException("Clinic not found"));

        if (request.latitude() != null && request.longitude() != null) {
            attendance.setCheckInLat(request.latitude());
            attendance.setCheckInLon(request.longitude());
            GeoFenceTarget geoFenceTarget = resolveGeoFenceTarget(principal, clinic, attendance.isAtOrganisation());
            attendance.setGeoVerified(verifyGeoFence(request.latitude(), request.longitude(), geoFenceTarget));
        }
        if (request.faceDescriptor() != null && !request.faceDescriptor().isEmpty()) {
            attendance.setFaceVerified(verifyFace(request.faceDescriptor(), principal.getUser()));
        }

        Attendance saved = attendanceRepository.save(attendance);
        User user = principal.getUser();
        return AttendanceResponse.from(saved, user.getFirstName(), user.getLastName(), placeNameOf(saved), null);
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

    // ── Review face-override check-in ─────────────────────────────────────────

    public AttendanceResponse reviewOverride(UUID id, boolean approved, UserPrincipal principal) {
        Attendance attendance = attendanceRepository.findById(id)
                .filter(a -> a.getOrgId().equals(principal.getOrgId()))
                .orElseThrow(() -> new ResourceNotFoundException("Attendance record not found"));

        if (attendance.getOverrideApproved() != null) {
            throw new ApiException(HttpStatus.CONFLICT, "Override has already been reviewed");
        }

        attendance.setOverrideApproved(approved);
        attendance.setOverrideReviewedBy(principal.getId());
        attendance.setOverrideReviewedAt(Instant.now());

        Attendance saved = attendanceRepository.save(attendance);
        return enrich(List.of(saved)).get(0);
    }

    // ── Live geo-fence preview (before an actual check-in/verify is submitted) ───

    /**
     * Distance from the caller's current position to the place they are checking in at — the
     * chosen clinic, or the organisation's own address when {@code atOrganisation} (Business
     * Owner only). Read-only: doesn't touch the attendance record.
     */
    @Transactional(readOnly = true)
    public GeoCheckResponse previewGeoCheck(UUID clinicId, boolean atOrganisation, double latitude, double longitude, UserPrincipal principal) {
        Clinic clinic = requireClinicUnlessOrganisation(clinicId, atOrganisation, principal);
        GeoFenceTarget target = resolveGeoFenceTarget(principal, clinic, atOrganisation);

        if (target.latitude() == null || target.longitude() == null) {
            return new GeoCheckResponse(true, null, target.radiusMeters(), target.label(), target.type());
        }
        double distance = haversineDistance(latitude, longitude, target.latitude(), target.longitude());
        boolean verified = distance <= target.radiusMeters();
        return new GeoCheckResponse(verified, distance, target.radiusMeters(), target.label(), target.type());
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    /** Where a geo-fence check is measured against — a clinic, or the organisation itself. */
    private record GeoFenceTarget(Double latitude, Double longitude, int radiusMeters, String label, GeoCheckReferenceType type) {}

    /**
     * The clinic being checked into, or null for the organisation's own location. Only a Business
     * Owner (judged by primary role, as elsewhere here) may choose the organisation.
     */
    private Clinic requireClinicUnlessOrganisation(UUID clinicId, boolean atOrganisation, UserPrincipal principal) {
        if (atOrganisation) {
            if (principal.getUser().getRole() != Role.BUSINESS_OWNER) {
                throw new ApiException(HttpStatus.FORBIDDEN, "Only a Business Owner can check in at the organisation");
            }
            return null;
        }
        if (clinicId == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "clinicId is required");
        }
        return clinicRepository.findByIdAndOrgId(clinicId, principal.getOrgId())
                .orElseThrow(() -> new ResourceNotFoundException("Clinic not found"));
    }

    private GeoFenceTarget resolveGeoFenceTarget(UserPrincipal principal, Clinic clinic, boolean atOrganisation) {
        if (atOrganisation) {
            Organisation org = organisationRepository.findById(principal.getOrgId())
                    .orElseThrow(() -> new ResourceNotFoundException("Organisation not found"));
            int radius = org.getGeoFenceRadiusMeters() != null ? org.getGeoFenceRadiusMeters() : 200;
            return new GeoFenceTarget(org.getLatitude(), org.getLongitude(), radius, org.getName(), GeoCheckReferenceType.ORGANISATION);
        }
        int radius = clinic.getGeoFenceRadiusMeters() != null ? clinic.getGeoFenceRadiusMeters() : 200;
        return new GeoFenceTarget(clinic.getLatitude(), clinic.getLongitude(), radius, clinic.getName(), GeoCheckReferenceType.CLINIC);
    }

    /** The display name of where a record was made — its clinic, or the organisation. */
    private String placeNameOf(Attendance a) {
        if (a.isAtOrganisation() || a.getClinicId() == null) {
            return organisationRepository.findById(a.getOrgId()).map(Organisation::getName).orElse("");
        }
        return clinicRepository.findById(a.getClinicId()).map(Clinic::getName).orElse("");
    }

    private boolean verifyGeoFence(Double lat, Double lon, GeoFenceTarget target) {
        if (lat == null || lon == null) return false;
        if (target.latitude() == null || target.longitude() == null) return true;
        double distance = haversineDistance(lat, lon, target.latitude(), target.longitude());
        return distance <= target.radiusMeters();
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
        Set<UUID> clinicIds = records.stream().map(Attendance::getClinicId)
                .filter(id -> id != null).collect(Collectors.toSet());

        // also collect reviewer UUIDs for override enrichment
        Set<UUID> reviewerIds = records.stream()
                .map(Attendance::getOverrideReviewedBy)
                .filter(id -> id != null)
                .collect(Collectors.toSet());

        Set<UUID> allUserIds = new java.util.HashSet<>(userIds);
        allUserIds.addAll(reviewerIds);

        Map<UUID, User>   userMap   = userRepository.findAllById(allUserIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u));
        Map<UUID, Clinic> clinicMap = clinicRepository.findAllById(clinicIds).stream()
                .collect(Collectors.toMap(Clinic::getId, c -> c));

        // Records made at the organisation have no clinic — they show the organisation's name.
        String orgName = records.stream().anyMatch(a -> a.getClinicId() == null)
                ? organisationRepository.findById(records.get(0).getOrgId()).map(Organisation::getName).orElse("")
                : "";

        return records.stream().map(a -> {
            User   u = userMap.get(a.getUserId());
            Clinic c = a.getClinicId() != null ? clinicMap.get(a.getClinicId()) : null;
            String reviewerName = null;
            if (a.getOverrideReviewedBy() != null) {
                User reviewer = userMap.get(a.getOverrideReviewedBy());
                if (reviewer != null) {
                    reviewerName = reviewer.getFirstName() + " " + reviewer.getLastName();
                }
            }
            return AttendanceResponse.from(
                    a,
                    u != null ? u.getFirstName() : "",
                    u != null ? u.getLastName()  : "",
                    a.getClinicId() == null ? orgName : (c != null ? c.getName() : ""),
                    reviewerName);
        }).toList();
    }
}

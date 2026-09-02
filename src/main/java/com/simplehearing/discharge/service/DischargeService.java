package com.simplehearing.discharge.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.simplehearing.common.exception.ApiException;
import com.simplehearing.common.exception.ResourceNotFoundException;
import com.simplehearing.discharge.dto.CreateDischargeRequest;
import com.simplehearing.discharge.dto.DischargePreviewResponse;
import com.simplehearing.discharge.dto.DischargeRecordResponse;
import com.simplehearing.discharge.entity.DischargeRecord;
import com.simplehearing.discharge.repository.DischargeRecordRepository;
import com.simplehearing.enrollment.entity.Enrollment;
import com.simplehearing.enrollment.enums.EnrollmentStatus;
import com.simplehearing.enrollment.repository.EnrollmentRepository;
import com.simplehearing.iep.entity.IEPPlan;
import com.simplehearing.iep.repository.IEPGoalRepository;
import com.simplehearing.iep.repository.IEPPlanRepository;
import com.simplehearing.organisation.entity.Organisation;
import com.simplehearing.organisation.repository.OrganisationRepository;
import com.simplehearing.patient.entity.Patient;
import com.simplehearing.patient.enums.PatientStage;
import com.simplehearing.patient.repository.PatientRepository;
import com.simplehearing.program.repository.ProgramRepository;
import com.simplehearing.review.entity.ReviewMeeting;
import com.simplehearing.review.repository.ReviewMeetingRepository;
import com.simplehearing.storage.StorageService;
import com.simplehearing.subscription.repository.SubscriptionRepository;
import com.simplehearing.successcriteria.dto.SuccessCriteriaResponse;
import com.simplehearing.successcriteria.service.SuccessCriteriaService;
import com.simplehearing.user.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class DischargeService {

    private final DischargeRecordRepository dischargeRecordRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final PatientRepository patientRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final ProgramRepository programRepository;
    private final UserRepository userRepository;
    private final IEPPlanRepository iepPlanRepository;
    private final IEPGoalRepository iepGoalRepository;
    private final ReviewMeetingRepository reviewMeetingRepository;
    private final SuccessCriteriaService successCriteriaService;
    private final OrganisationRepository organisationRepository;
    private final ObjectMapper objectMapper;
    private final DischargePdfService dischargePdfService;
    private final StorageService storageService;

    public DischargeService(
            DischargeRecordRepository dischargeRecordRepository,
            EnrollmentRepository enrollmentRepository,
            PatientRepository patientRepository,
            SubscriptionRepository subscriptionRepository,
            ProgramRepository programRepository,
            UserRepository userRepository,
            IEPPlanRepository iepPlanRepository,
            IEPGoalRepository iepGoalRepository,
            ReviewMeetingRepository reviewMeetingRepository,
            SuccessCriteriaService successCriteriaService,
            OrganisationRepository organisationRepository,
            ObjectMapper objectMapper,
            DischargePdfService dischargePdfService,
            StorageService storageService) {
        this.dischargeRecordRepository = dischargeRecordRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.patientRepository = patientRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.programRepository = programRepository;
        this.userRepository = userRepository;
        this.iepPlanRepository = iepPlanRepository;
        this.iepGoalRepository = iepGoalRepository;
        this.reviewMeetingRepository = reviewMeetingRepository;
        this.successCriteriaService = successCriteriaService;
        this.organisationRepository = organisationRepository;
        this.objectMapper = objectMapper;
        this.dischargePdfService = dischargePdfService;
        this.storageService = storageService;
    }

    /**
     * The entire "episode of care" boundary: every enrollment not yet claimed by a past
     * discharge. Deliberately a null-FK filter, not a date-range computation — immune to
     * overlapping/re-enrollment edge cases a date range would get wrong.
     */
    public List<Enrollment> resolveEpisodeEnrollments(UUID orgId, UUID patientId) {
        return enrollmentRepository.findByOrgIdAndPatientIdAndDischargedInRecordIdIsNull(orgId, patientId);
    }

    private String programName(Enrollment e) {
        return subscriptionRepository.findById(e.getSubscriptionId())
                .flatMap(sub -> programRepository.findById(sub.getProgramId()))
                .map(p -> p.getName())
                .orElse("Unknown Program");
    }

    private String therapistName(UUID therapistId) {
        return userRepository.findById(therapistId)
                .map(u -> u.getFirstName() + " " + u.getLastName())
                .orElse("");
    }

    public DischargePreviewResponse preview(UUID orgId, UUID patientId) {
        List<Enrollment> episodeEnrollments = resolveEpisodeEnrollments(orgId, patientId);

        List<DischargePreviewResponse.EnrollmentPreview> previews = episodeEnrollments.stream()
                .map(e -> new DischargePreviewResponse.EnrollmentPreview(
                        e.getId(), programName(e), therapistName(e.getTherapistId()),
                        successCriteriaService.compute(orgId, e.getId())))
                .toList();

        boolean allMet = !previews.isEmpty() && previews.stream().allMatch(p -> p.criteria().overallSuccessful());
        return new DischargePreviewResponse(previews, allMet);
    }

    @Transactional
    public DischargeRecordResponse createDischarge(UUID orgId, UUID patientId, CreateDischargeRequest request, UUID dischargedBy) {
        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new ResourceNotFoundException("Patient not found"));
        if (!patient.getOrgId().equals(orgId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Access denied");
        }
        if (patient.getStage() == PatientStage.DISCHARGED) {
            throw new ApiException(HttpStatus.CONFLICT, "This patient is already discharged");
        }

        List<Enrollment> episodeEnrollments = resolveEpisodeEnrollments(orgId, patientId);
        if (episodeEnrollments.isEmpty()) {
            throw new ApiException(HttpStatus.CONFLICT, "This patient has no enrollments to discharge");
        }

        List<SuccessCriteriaResponse> criteriaList = episodeEnrollments.stream()
                .map(e -> successCriteriaService.compute(orgId, e.getId()))
                .toList();

        Organisation org = organisationRepository.findById(orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Organisation not found"));
        boolean allSuccessful = org.isRequireAllEnrollmentsForDischarge()
                ? criteriaList.stream().allMatch(SuccessCriteriaResponse::overallSuccessful)
                : criteriaList.stream().anyMatch(SuccessCriteriaResponse::overallSuccessful);

        BigDecimal avgCommunication = averageOf(episodeEnrollments.stream()
                .flatMap(e -> reviewMeetingRepository.findByEnrollmentIdOrderByMeetingNumberAsc(e.getId()).stream())
                .map(ReviewMeeting::getCommunicationRating)
                .filter(r -> r != null)
                .map(BigDecimal::valueOf)
                .toList());

        BigDecimal avgProgress = averageOf(criteriaList.stream()
                .map(SuccessCriteriaResponse::parentSatisfactionPct)
                .filter(v -> v != null)
                .map(BigDecimal::valueOf)
                .toList());

        BigDecimal goalMastery = averageOf(criteriaList.stream()
                .map(SuccessCriteriaResponse::goalMasteryPct)
                .filter(v -> v != null)
                .map(BigDecimal::valueOf)
                .toList());

        Boolean goalMasteryMet = criteriaList.stream().anyMatch(c -> c.goalMasteryMet() != null)
                ? criteriaList.stream().allMatch(c -> Boolean.TRUE.equals(c.goalMasteryMet()) || c.goalMasteryMet() == null)
                : null;
        Boolean parentSatisfactionMet = criteriaList.stream().anyMatch(c -> c.parentSatisfactionMet() != null)
                ? criteriaList.stream().allMatch(c -> Boolean.TRUE.equals(c.parentSatisfactionMet()) || c.parentSatisfactionMet() == null)
                : null;
        boolean therapistSignoffMet = episodeEnrollments.stream().allMatch(Enrollment::isTherapistSignedOff);

        LocalDate episodeStart = episodeEnrollments.stream()
                .map(Enrollment::getStartDate)
                .min(Comparator.naturalOrder())
                .orElse(null);

        DischargeRecord record = new DischargeRecord();
        record.setOrgId(orgId);
        record.setPatientId(patientId);
        record.setDischargeDate(LocalDate.now());
        record.setDischargedBy(dischargedBy);
        record.setEpisodeStartDate(episodeStart);
        record.setGoalsAtDischargeSnapshot(buildGoalsSnapshot(episodeEnrollments));
        record.setAvgCommunicationRating(avgCommunication);
        record.setAvgProgressRatingPct(avgProgress);
        record.setGoalMasteryPct(goalMastery);
        record.setGoalMasteryMet(goalMasteryMet);
        record.setTherapistSignoffMet(therapistSignoffMet);
        record.setParentSatisfactionMet(parentSatisfactionMet);
        record.setOverallSuccessful(allSuccessful);
        record.setNotes(request.notes());

        DischargeRecord saved = dischargeRecordRepository.save(record);

        for (Enrollment e : episodeEnrollments) {
            e.setDischargedInRecordId(saved.getId());
            if (e.getStatus() == EnrollmentStatus.ACTIVE) {
                e.setStatus(EnrollmentStatus.COMPLETED);
            }
            enrollmentRepository.save(e);
        }

        patient.setStage(PatientStage.DISCHARGED);
        patientRepository.save(patient);

        return toResponse(saved, episodeEnrollments);
    }

    public List<DischargeRecordResponse> list(UUID orgId, UUID patientId) {
        return dischargeRecordRepository.findByOrgIdAndPatientIdOrderByDischargeDateDesc(orgId, patientId).stream()
                .map(d -> toResponse(d, enrollmentRepository.findByDischargedInRecordId(d.getId())))
                .toList();
    }

    public DischargeRecord getInOrg(UUID orgId, UUID dischargeId) {
        return dischargeRecordRepository.findByIdAndOrgId(dischargeId, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Discharge record not found"));
    }

    public DischargeRecordResponse getResponseInOrg(UUID orgId, UUID dischargeId) {
        DischargeRecord record = getInOrg(orgId, dischargeId);
        return toResponse(record, enrollmentRepository.findByDischargedInRecordId(record.getId()));
    }

    /** Lazily generates the PDF on first call, then always returns a fresh short-lived download URL. */
    @Transactional
    public String getOrGeneratePdfUrl(UUID orgId, UUID dischargeId) {
        DischargeRecord record = getInOrg(orgId, dischargeId);

        if (record.getPdfUrl() == null) {
            Patient patient = patientRepository.findById(record.getPatientId())
                    .orElseThrow(() -> new ResourceNotFoundException("Patient not found"));
            String patientName = patient.getFirstName() + " " + patient.getLastName();
            DischargeRecordResponse response = toResponse(record, enrollmentRepository.findByDischargedInRecordId(record.getId()));
            byte[] pdfBytes = dischargePdfService.generate(response, patientName);

            try {
                String url = storageService.store(
                        pdfBytes, "discharge-summary.pdf", "application/pdf",
                        "discharge/" + record.getPatientId());
                record.setPdfUrl(url);
                record.setPdfGeneratedAt(Instant.now());
                dischargeRecordRepository.save(record);
            } catch (java.io.IOException e) {
                throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to store discharge PDF");
            }
        }

        return storageService.presign(record.getPdfUrl(), java.time.Duration.ofMinutes(15));
    }

    public DischargeRecordResponse toResponse(DischargeRecord record, List<Enrollment> enrollments) {
        String dischargedByName = therapistName(record.getDischargedBy());
        List<DischargeRecordResponse.EnrollmentSummary> summaries = enrollments.stream()
                .map(e -> new DischargeRecordResponse.EnrollmentSummary(
                        e.getId(), programName(e), therapistName(e.getTherapistId()), e.getStartDate(), e.getEndDate()))
                .toList();
        return DischargeRecordResponse.from(record, dischargedByName, summaries);
    }

    private String buildGoalsSnapshot(List<Enrollment> episodeEnrollments) {
        List<Map<String, Object>> snapshot = episodeEnrollments.stream()
                .flatMap(e -> iepPlanRepository.findByEnrollmentId(e.getId()).stream())
                .map(IEPPlan::getId)
                .distinct()
                .flatMap(planId -> iepGoalRepository.findByPlanId(planId).stream())
                .map(g -> Map.<String, Object>of(
                        "title", g.getTitle() == null ? "" : g.getTitle(),
                        "domain", g.getDomain() == null ? "" : g.getDomain().name(),
                        "status", g.getStatus() == null ? "" : g.getStatus().name()))
                .collect(Collectors.toList());
        try {
            return objectMapper.writeValueAsString(snapshot);
        } catch (Exception e) {
            return "[]";
        }
    }

    private static BigDecimal averageOf(List<BigDecimal> values) {
        if (values.isEmpty()) return null;
        BigDecimal sum = values.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        return sum.divide(BigDecimal.valueOf(values.size()), 2, RoundingMode.HALF_UP);
    }
}

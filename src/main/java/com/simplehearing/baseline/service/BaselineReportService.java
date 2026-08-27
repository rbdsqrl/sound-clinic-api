package com.simplehearing.baseline.service;

import com.simplehearing.auth.security.UserPrincipal;
import com.simplehearing.baseline.dto.AddBaselineProgressRequest;
import com.simplehearing.baseline.dto.BaselineDomainResponse;
import com.simplehearing.baseline.dto.BaselineProgressEntryResponse;
import com.simplehearing.baseline.dto.BaselineReportResponse;
import com.simplehearing.baseline.dto.CreateBaselineReportRequest;
import com.simplehearing.baseline.dto.UpdateBaselineReportRequest;
import com.simplehearing.baseline.entity.BaselineDomainValue;
import com.simplehearing.baseline.entity.BaselineProgressEntry;
import com.simplehearing.baseline.entity.BaselineReport;
import com.simplehearing.baseline.enums.BaselineDomain;
import com.simplehearing.baseline.repository.BaselineDomainValueRepository;
import com.simplehearing.baseline.repository.BaselineProgressEntryRepository;
import com.simplehearing.baseline.repository.BaselineReportRepository;
import com.simplehearing.common.exception.ApiException;
import com.simplehearing.common.exception.ResourceNotFoundException;
import com.simplehearing.patient.repository.PatientRepository;
import com.simplehearing.user.entity.User;
import com.simplehearing.user.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class BaselineReportService {

    private final BaselineReportRepository reportRepository;
    private final BaselineDomainValueRepository domainValueRepository;
    private final BaselineProgressEntryRepository progressRepository;
    private final PatientRepository patientRepository;
    private final UserRepository userRepository;

    public BaselineReportService(BaselineReportRepository reportRepository,
                                  BaselineDomainValueRepository domainValueRepository,
                                  BaselineProgressEntryRepository progressRepository,
                                  PatientRepository patientRepository,
                                  UserRepository userRepository) {
        this.reportRepository = reportRepository;
        this.domainValueRepository = domainValueRepository;
        this.progressRepository = progressRepository;
        this.patientRepository = patientRepository;
        this.userRepository = userRepository;
    }

    // ── Get report ────────────────────────────────────────────────────────────

    /** Returns {@code null} when the patient has no baseline report yet — that's a normal,
     *  expected state (staff haven't created one), not an error. */
    public BaselineReportResponse getReport(UUID patientId, UserPrincipal principal) {
        requirePatientInOrg(patientId, principal.getOrgId());
        BaselineReport report = reportRepository.findByPatientId(patientId).orElse(null);
        return report == null ? null : buildResponse(report);
    }

    // ── Create report ─────────────────────────────────────────────────────────

    @Transactional
    public BaselineReportResponse createReport(UUID patientId, CreateBaselineReportRequest req, UserPrincipal principal) {
        requirePatientInOrg(patientId, principal.getOrgId());

        if (reportRepository.findByPatientId(patientId).isPresent()) {
            throw new ApiException(HttpStatus.CONFLICT, "A baseline report already exists for this patient");
        }

        BaselineReport report = new BaselineReport();
        report.setOrgId(principal.getOrgId());
        report.setPatientId(patientId);
        report.setAgeAtAdmission(req.ageAtAdmission());
        report.setAgeOnDate(req.ageOnDate());
        report.setCdct(req.cdct());
        report.setCreatedBy(principal.getId());
        BaselineReport saved = reportRepository.save(report);

        Map<BaselineDomain, String> values = req.domainValues() != null ? req.domainValues() : Map.of();
        for (BaselineDomain domain : BaselineDomain.values()) {
            BaselineDomainValue dv = new BaselineDomainValue();
            dv.setReportId(saved.getId());
            dv.setDomain(domain);
            String value = values.get(domain);
            dv.setValue(value);
            if (value != null && !value.isBlank()) {
                dv.setUpdatedBy(principal.getId());
            }
            domainValueRepository.save(dv);
        }

        return buildResponse(saved);
    }

    // ── Update report (header fields and/or per-domain baseline text) ──────────

    @Transactional
    public BaselineReportResponse updateReport(UUID patientId, UpdateBaselineReportRequest req, UserPrincipal principal) {
        requirePatientInOrg(patientId, principal.getOrgId());
        BaselineReport report = requireReport(patientId);

        if (req.ageAtAdmission() != null) report.setAgeAtAdmission(req.ageAtAdmission());
        if (req.ageOnDate() != null) report.setAgeOnDate(req.ageOnDate());
        if (req.cdct() != null) report.setCdct(req.cdct());
        BaselineReport saved = reportRepository.save(report);

        if (req.domainValues() != null) {
            for (Map.Entry<BaselineDomain, String> e : req.domainValues().entrySet()) {
                BaselineDomainValue dv = domainValueRepository.findByReportIdAndDomain(saved.getId(), e.getKey())
                        .orElseGet(() -> {
                            BaselineDomainValue fresh = new BaselineDomainValue();
                            fresh.setReportId(saved.getId());
                            fresh.setDomain(e.getKey());
                            return fresh;
                        });
                dv.setValue(e.getValue());
                dv.setUpdatedBy(principal.getId());
                domainValueRepository.save(dv);
            }
        }

        return buildResponse(saved);
    }

    // ── Log a dated "current" entry for one domain ──────────────────────────────

    @Transactional
    public BaselineProgressEntryResponse addProgress(UUID patientId, BaselineDomain domain,
                                                       AddBaselineProgressRequest req, UserPrincipal principal) {
        requirePatientInOrg(patientId, principal.getOrgId());
        BaselineReport report = requireReport(patientId);

        BaselineProgressEntry entry = new BaselineProgressEntry();
        entry.setReportId(report.getId());
        entry.setDomain(domain);
        entry.setEntryDate(req.entryDate());
        entry.setValue(req.value());
        entry.setLoggedBy(principal.getId());
        BaselineProgressEntry saved = progressRepository.save(entry);

        User loggedBy = userRepository.findById(principal.getId()).orElse(null);
        return BaselineProgressEntryResponse.from(saved, loggedBy != null ? fullName(loggedBy) : null);
    }

    // ── List a domain's dated entries, newest first ─────────────────────────────

    public List<BaselineProgressEntryResponse> listProgress(UUID patientId, BaselineDomain domain, UserPrincipal principal) {
        requirePatientInOrg(patientId, principal.getOrgId());
        BaselineReport report = requireReport(patientId);

        List<BaselineProgressEntry> entries =
                progressRepository.findByReportIdAndDomainOrderByEntryDateDesc(report.getId(), domain);
        Map<UUID, User> userMap = userMapFor(entries);

        return entries.stream()
                .map(e -> BaselineProgressEntryResponse.from(e, nameFor(e.getLoggedBy(), userMap)))
                .toList();
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private BaselineReportResponse buildResponse(BaselineReport report) {
        Map<BaselineDomain, BaselineDomainValue> valuesByDomain = domainValueRepository.findByReportId(report.getId())
                .stream()
                .collect(Collectors.toMap(BaselineDomainValue::getDomain, v -> v));

        List<BaselineProgressEntry> allEntries = progressRepository.findByReportIdOrderByEntryDateDesc(report.getId());
        Map<UUID, User> userMap = userMapFor(allEntries);

        Map<BaselineDomain, List<BaselineProgressEntryResponse>> entriesByDomain = new HashMap<>();
        for (BaselineProgressEntry entry : allEntries) {
            entriesByDomain
                    .computeIfAbsent(entry.getDomain(), k -> new java.util.ArrayList<>())
                    .add(BaselineProgressEntryResponse.from(entry, nameFor(entry.getLoggedBy(), userMap)));
        }

        List<BaselineDomainResponse> domains = new java.util.ArrayList<>();
        for (BaselineDomain domain : BaselineDomain.values()) {
            BaselineDomainValue dv = valuesByDomain.get(domain);
            domains.add(new BaselineDomainResponse(
                    domain,
                    dv != null ? dv.getValue() : null,
                    dv != null ? dv.getUpdatedAt() : null,
                    entriesByDomain.getOrDefault(domain, List.of())
            ));
        }

        return new BaselineReportResponse(
                report.getId(),
                report.getPatientId(),
                report.getAgeAtAdmission(),
                report.getAgeOnDate(),
                report.getCdct(),
                report.getCreatedAt(),
                report.getUpdatedAt(),
                domains
        );
    }

    private Map<UUID, User> userMapFor(List<BaselineProgressEntry> entries) {
        Set<UUID> ids = entries.stream().map(BaselineProgressEntry::getLoggedBy).collect(Collectors.toSet());
        if (ids.isEmpty()) return Map.of();
        return userRepository.findAllById(ids).stream().collect(Collectors.toMap(User::getId, u -> u));
    }

    private String nameFor(UUID userId, Map<UUID, User> userMap) {
        User u = userMap.get(userId);
        return u != null ? fullName(u) : null;
    }

    private String fullName(User user) {
        return user.getFirstName() + " " + user.getLastName();
    }

    private void requirePatientInOrg(UUID patientId, UUID orgId) {
        patientRepository.findByIdAndOrgId(patientId, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Patient not found"));
    }

    private BaselineReport requireReport(UUID patientId) {
        return reportRepository.findByPatientId(patientId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND,
                        "No baseline report exists for this patient yet — create one first"));
    }
}

package com.simplehearing.assessment.service;

import com.simplehearing.assessment.dto.AssessmentDefinitionResponse;
import com.simplehearing.assessment.dto.CreateAssessmentRequest;
import com.simplehearing.assessment.dto.PatientAssessmentResponse;
import com.simplehearing.assessment.entity.*;
import com.simplehearing.assessment.enums.ItemType;
import com.simplehearing.assessment.enums.ScoringType;
import com.simplehearing.assessment.repository.*;
import com.simplehearing.common.exception.ApiException;
import com.simplehearing.common.exception.ResourceNotFoundException;
import com.simplehearing.patient.entity.Patient;
import com.simplehearing.patient.repository.PatientRepository;
import com.simplehearing.storage.StorageService;
import com.simplehearing.user.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDate;
import java.time.Period;
import java.util.*;

@Service
public class AssessmentService {

    private final AssessmentDefinitionRepository definitionRepository;
    private final AssessmentCategoryRepository categoryRepository;
    private final AssessmentItemRepository itemRepository;
    private final AssessmentItemOptionRepository optionRepository;
    private final AssessmentClassificationBandRepository bandRepository;
    private final AssessmentResponseRepository responseRepository;
    private final PatientAssessmentRepository assessmentRepository;
    private final PatientRepository patientRepository;
    private final UserRepository userRepository;
    private final AssessmentPdfService assessmentPdfService;
    private final StorageService storageService;

    public AssessmentService(AssessmentDefinitionRepository definitionRepository,
                              AssessmentCategoryRepository categoryRepository,
                              AssessmentItemRepository itemRepository,
                              AssessmentItemOptionRepository optionRepository,
                              AssessmentClassificationBandRepository bandRepository,
                              AssessmentResponseRepository responseRepository,
                              PatientAssessmentRepository assessmentRepository,
                              PatientRepository patientRepository,
                              UserRepository userRepository,
                              AssessmentPdfService assessmentPdfService,
                              StorageService storageService) {
        this.definitionRepository = definitionRepository;
        this.categoryRepository = categoryRepository;
        this.itemRepository = itemRepository;
        this.optionRepository = optionRepository;
        this.bandRepository = bandRepository;
        this.responseRepository = responseRepository;
        this.assessmentRepository = assessmentRepository;
        this.patientRepository = patientRepository;
        this.userRepository = userRepository;
        this.assessmentPdfService = assessmentPdfService;
        this.storageService = storageService;
    }

    // ── Definition ──────────────────────────────────────────────────────────

    public AssessmentDefinitionResponse getDefinition(String code) {
        AssessmentDefinition def = requireDefinition(code);
        List<AssessmentCategory> categories = categoryRepository.findByDefinitionIdOrderByDisplayOrder(def.getId());
        List<UUID> categoryIds = categories.stream().map(AssessmentCategory::getId).toList();
        List<AssessmentItem> items = itemRepository.findByCategoryIdIn(categoryIds);
        items.sort(Comparator.comparingInt(AssessmentItem::getDisplayOrder));
        Map<UUID, List<AssessmentItem>> itemsByCategory = new LinkedHashMap<>();
        for (AssessmentCategory c : categories) itemsByCategory.put(c.getId(), new ArrayList<>());
        for (AssessmentItem i : items) itemsByCategory.get(i.getCategoryId()).add(i);

        List<UUID> itemIds = items.stream().map(AssessmentItem::getId).toList();
        List<AssessmentItemOption> options = optionRepository.findByItemIdIn(itemIds);
        options.sort(Comparator.comparingInt(AssessmentItemOption::getDisplayOrder));
        Map<UUID, List<AssessmentItemOption>> optionsByItem = new HashMap<>();
        for (AssessmentItem i : items) optionsByItem.put(i.getId(), new ArrayList<>());
        for (AssessmentItemOption o : options) optionsByItem.get(o.getItemId()).add(o);

        List<AssessmentDefinitionResponse.CategoryDto> categoryDtos = categories.stream()
                .map(c -> new AssessmentDefinitionResponse.CategoryDto(c.getName(),
                        itemsByCategory.get(c.getId()).stream()
                                .map(i -> new AssessmentDefinitionResponse.ItemDto(
                                        i.getItemNumber(), i.getText(), i.getItemType().name(),
                                        optionsByItem.get(i.getId()).stream()
                                                .map(o -> new AssessmentDefinitionResponse.OptionDto(o.getId(), o.getLabel(), o.getScore()))
                                                .toList()))
                                .toList()))
                .toList();

        Integer maxScore = def.getScoringType() == ScoringType.SUM_SCORE ? maxScoreFor(items, optionsByItem) : null;
        return new AssessmentDefinitionResponse(def.getCode(), def.getName(), def.getDescription(),
                def.getScoringType().name(), maxScore, categoryDtos);
    }

    private int maxScoreFor(List<AssessmentItem> items, Map<UUID, List<AssessmentItemOption>> optionsByItem) {
        int max = 0;
        for (AssessmentItem i : items) {
            if (i.getItemType() != ItemType.SINGLE_SELECT) continue;
            max += optionsByItem.get(i.getId()).stream()
                    .map(AssessmentItemOption::getScore).filter(Objects::nonNull)
                    .mapToInt(Integer::intValue).max().orElse(0);
        }
        return max;
    }

    // ── List / create ───────────────────────────────────────────────────────

    public List<PatientAssessmentResponse> list(UUID orgId, UUID patientId, String code) {
        AssessmentDefinition def = requireDefinition(code);
        Integer maxScore = def.getScoringType() == ScoringType.SUM_SCORE ? getDefinition(code).maxScore() : null;
        return assessmentRepository.findByOrgIdAndPatientIdAndDefinitionIdOrderByAssessmentDateAsc(orgId, patientId, def.getId())
                .stream()
                .map(a -> PatientAssessmentResponse.from(a, def.getCode(), userName(a.getFilledBy()), maxScore))
                .toList();
    }

    public PatientAssessmentResponse create(UUID orgId, UUID patientId, UUID filledBy, String code, CreateAssessmentRequest request) {
        AssessmentDefinition def = requireDefinition(code);
        Patient patient = patientRepository.findByIdAndOrgId(patientId, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Patient not found"));

        List<AssessmentCategory> categories = categoryRepository.findByDefinitionIdOrderByDisplayOrder(def.getId());
        List<UUID> categoryIds = categories.stream().map(AssessmentCategory::getId).toList();
        List<AssessmentItem> items = itemRepository.findByCategoryIdIn(categoryIds);
        Map<Integer, AssessmentItem> itemsByNumber = new HashMap<>();
        for (AssessmentItem i : items) itemsByNumber.put(i.getItemNumber(), i);

        if (!request.responses().keySet().equals(itemsByNumber.keySet())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Every item must be answered");
        }

        List<UUID> itemIds = items.stream().map(AssessmentItem::getId).toList();
        List<AssessmentItemOption> options = optionRepository.findByItemIdIn(itemIds);
        Map<UUID, AssessmentItemOption> optionsById = new HashMap<>();
        for (AssessmentItemOption o : options) optionsById.put(o.getId(), o);

        PatientAssessment assessment = new PatientAssessment();
        assessment.setOrgId(orgId);
        assessment.setPatientId(patientId);
        assessment.setDefinitionId(def.getId());
        assessment.setAssessmentDate(request.assessmentDate());
        assessment.setFilledBy(filledBy);

        List<AssessmentResponse> toSave = new ArrayList<>();
        int total = 0;
        for (Map.Entry<Integer, CreateAssessmentRequest.ItemResponse> entry : request.responses().entrySet()) {
            AssessmentItem item = itemsByNumber.get(entry.getKey());
            CreateAssessmentRequest.ItemResponse answer = entry.getValue();
            AssessmentResponse response = new AssessmentResponse();
            response.setItemId(item.getId());

            switch (item.getItemType()) {
                case SINGLE_SELECT -> {
                    if (answer.optionId() == null) {
                        throw new ApiException(HttpStatus.BAD_REQUEST, "Item " + entry.getKey() + " needs a selected option");
                    }
                    AssessmentItemOption opt = optionsById.get(answer.optionId());
                    if (opt == null || !opt.getItemId().equals(item.getId())) {
                        throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid option for item " + entry.getKey());
                    }
                    response.setSelectedOptionId(opt.getId());
                    if (opt.getScore() != null) total += opt.getScore();
                }
                case MULTI_SELECT -> {
                    if (answer.optionIds() == null || answer.optionIds().isEmpty()) {
                        throw new ApiException(HttpStatus.BAD_REQUEST, "Item " + entry.getKey() + " needs at least one selected option");
                    }
                    for (UUID optId : answer.optionIds()) {
                        AssessmentItemOption opt = optionsById.get(optId);
                        if (opt == null || !opt.getItemId().equals(item.getId())) {
                            throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid option for item " + entry.getKey());
                        }
                    }
                    response.setTextValue(String.join(",", answer.optionIds().stream().map(UUID::toString).toList()));
                }
                case TEXT, FILE -> {
                    if (answer.text() == null || answer.text().isBlank()) {
                        throw new ApiException(HttpStatus.BAD_REQUEST, "Item " + entry.getKey() + " needs an answer");
                    }
                    response.setTextValue(answer.text());
                }
            }
            toSave.add(response);
        }

        boolean scored = def.getScoringType() == ScoringType.SUM_SCORE;
        assessment.setTotalScore(scored ? total : null);
        assessment.setClassification(scored ? classify(def.getId(), total, ageYears(patient.getDateOfBirth(), request.assessmentDate())) : null);

        PatientAssessment saved = assessmentRepository.save(assessment);
        for (AssessmentResponse r : toSave) r.setPatientAssessmentId(saved.getId());
        responseRepository.saveAll(toSave);

        Integer maxScore = scored ? getDefinition(code).maxScore() : null;
        return PatientAssessmentResponse.from(saved, def.getCode(), userName(filledBy), maxScore);
    }

    public String uploadFile(UUID patientId, MultipartFile file) {
        try {
            return storageService.store(file, "assessments/" + patientId);
        } catch (IOException e) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to store file");
        }
    }

    /**
     * Renders and stores the PDF fresh on every call — regenerating one of these is cheap
     * and infrequent, so there's no need for a cached pdf_url column like discharge reports.
     */
    public String generatePdfUrl(UUID orgId, UUID patientId, UUID assessmentId) {
        PatientAssessment assessment = assessmentRepository.findById(assessmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Assessment not found"));
        if (!assessment.getOrgId().equals(orgId) || !assessment.getPatientId().equals(patientId)) {
            throw new ResourceNotFoundException("Assessment not found");
        }
        Patient patient = patientRepository.findByIdAndOrgId(patientId, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Patient not found"));
        AssessmentDefinition def = definitionRepository.findById(assessment.getDefinitionId())
                .orElseThrow(() -> new ResourceNotFoundException("Assessment definition not found"));

        byte[] pdf = assessmentPdfService.generate(assessment, def, patient, userName(assessment.getFilledBy()));
        String filename = def.getCode().toLowerCase() + "-" + assessment.getAssessmentDate() + ".pdf";
        try {
            String storedUrl = storageService.store(pdf, filename, "application/pdf", "assessments/" + patientId);
            return storageService.presign(storedUrl, Duration.ofMinutes(15));
        } catch (IOException e) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to store assessment PDF");
        }
    }

    // ── Classification ──────────────────────────────────────────────────────

    /**
     * First matching band wins. Age bounds are half-open [min, max); score bounds are
     * inclusive [min, max] on both sides. A band with age bounds only matches when the
     * patient's age at assessment time is known.
     */
    private String classify(UUID definitionId, int totalScore, Double ageYears) {
        for (AssessmentClassificationBand b : bandRepository.findByDefinitionIdOrderByDisplayOrder(definitionId)) {
            boolean hasAgeGate = b.getMinAgeYears() != null || b.getMaxAgeYears() != null;
            if (hasAgeGate) {
                if (ageYears == null) continue;
                if (b.getMinAgeYears() != null && ageYears < b.getMinAgeYears().doubleValue()) continue;
                if (b.getMaxAgeYears() != null && ageYears >= b.getMaxAgeYears().doubleValue()) continue;
            }
            if (b.getMinScore() != null && totalScore < b.getMinScore()) continue;
            if (b.getMaxScore() != null && totalScore > b.getMaxScore()) continue;
            return b.getLabel();
        }
        return null;
    }

    private Double ageYears(LocalDate dob, LocalDate assessmentDate) {
        if (dob == null) return null;
        return Period.between(dob, assessmentDate).toTotalMonths() / 12.0;
    }

    // ── Helpers ─────────────────────────────────────────────────────────────

    private AssessmentDefinition requireDefinition(String code) {
        return definitionRepository.findByCode(code)
                .orElseThrow(() -> new ResourceNotFoundException("Assessment type not found: " + code));
    }

    private String userName(UUID userId) {
        return userRepository.findById(userId)
                .map(u -> (nullToEmpty(u.getFirstName()) + " " + nullToEmpty(u.getLastName())).trim())
                .orElse(null);
    }

    private String nullToEmpty(String s) { return s == null ? "" : s; }
}

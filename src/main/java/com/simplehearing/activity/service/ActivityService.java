package com.simplehearing.activity.service;

import com.simplehearing.activity.dto.*;
import com.simplehearing.activity.entity.*;
import com.simplehearing.activity.enums.AssignmentStatus;
import com.simplehearing.activity.enums.ChecklistQuestionType;
import com.simplehearing.activity.repository.*;
import com.simplehearing.common.exception.ApiException;
import com.simplehearing.common.exception.ResourceNotFoundException;
import com.simplehearing.organisation.repository.OrganisationRepository;
import com.simplehearing.patient.entity.Patient;
import com.simplehearing.patient.repository.PatientRepository;
import com.simplehearing.program.repository.ProgramRepository;
import com.simplehearing.user.entity.User;
import com.simplehearing.user.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ActivityService {

    private final ActivityRepository activityRepository;
    private final SkillRepository skillRepository;
    private final LanguageRepository languageRepository;
    private final PropRepository propRepository;
    private final ActivitySkillRepository activitySkillRepository;
    private final ActivityLanguageRepository activityLanguageRepository;
    private final ActivityPropRepository activityPropRepository;
    private final ActivityInstructionRepository instructionRepository;
    private final ActivityChecklistQuestionRepository questionRepository;
    private final ActivityChecklistOptionRepository optionRepository;
    private final ActivityResourceRepository resourceRepository;
    private final ActivityLinkRepository linkRepository;
    private final ActivityAssignmentRepository assignmentRepository;
    private final ActivityAttemptLogRepository attemptLogRepository;
    private final ActivityAttemptAnswerRepository answerRepository;
    private final ActivityAttemptAnswerOptionRepository answerOptionRepository;
    private final ProgramRepository programRepository;
    private final PatientRepository patientRepository;
    private final UserRepository userRepository;
    private final OrganisationRepository organisationRepository;

    public ActivityService(ActivityRepository activityRepository, SkillRepository skillRepository,
                            LanguageRepository languageRepository, PropRepository propRepository,
                            ActivitySkillRepository activitySkillRepository,
                            ActivityLanguageRepository activityLanguageRepository,
                            ActivityPropRepository activityPropRepository,
                            ActivityInstructionRepository instructionRepository,
                            ActivityChecklistQuestionRepository questionRepository,
                            ActivityChecklistOptionRepository optionRepository,
                            ActivityResourceRepository resourceRepository,
                            ActivityLinkRepository linkRepository,
                            ActivityAssignmentRepository assignmentRepository,
                            ActivityAttemptLogRepository attemptLogRepository,
                            ActivityAttemptAnswerRepository answerRepository,
                            ActivityAttemptAnswerOptionRepository answerOptionRepository,
                            ProgramRepository programRepository, PatientRepository patientRepository,
                            UserRepository userRepository, OrganisationRepository organisationRepository) {
        this.activityRepository = activityRepository;
        this.skillRepository = skillRepository;
        this.languageRepository = languageRepository;
        this.propRepository = propRepository;
        this.activitySkillRepository = activitySkillRepository;
        this.activityLanguageRepository = activityLanguageRepository;
        this.activityPropRepository = activityPropRepository;
        this.instructionRepository = instructionRepository;
        this.questionRepository = questionRepository;
        this.optionRepository = optionRepository;
        this.resourceRepository = resourceRepository;
        this.linkRepository = linkRepository;
        this.assignmentRepository = assignmentRepository;
        this.attemptLogRepository = attemptLogRepository;
        this.answerRepository = answerRepository;
        this.answerOptionRepository = answerOptionRepository;
        this.programRepository = programRepository;
        this.patientRepository = patientRepository;
        this.userRepository = userRepository;
        this.organisationRepository = organisationRepository;
    }

    // ── List / Get ──────────────────────────────────────────────────────────

    public List<ActivityResponse> list(UUID orgId, boolean activeOnly) {
        List<Activity> activities = activeOnly
                ? activityRepository.findByOrgIdAndIsActiveTrueOrderByCreatedAtDesc(orgId)
                : activityRepository.findByOrgIdOrderByCreatedAtDesc(orgId);
        return activities.stream().map(a -> toResponse(a, orgId)).toList();
    }

    public ActivityResponse get(UUID id, UUID orgId) {
        Activity activity = activityRepository.findByIdAndOrgId(id, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Activity not found"));
        return toResponse(activity, orgId);
    }

    public Activity requireOwned(UUID id, UUID orgId) {
        Activity activity = activityRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Activity not found"));
        if (!activity.getOrgId().equals(orgId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Access denied");
        }
        return activity;
    }

    // ── Create / Update / Delete ───────────────────────────────────────────

    @Transactional
    public ActivityResponse create(UUID orgId, UUID userId, CreateActivityRequest req) {
        Activity activity = new Activity();
        activity.setOrgId(orgId);
        activity.setCreatedBy(userId);
        applyCore(activity, req.title(), req.aboutActivity(), req.programId(), req.durationWeeks(),
                req.ageMinValue(), req.ageMinUnit(), req.ageMaxValue(), req.ageMaxUnit(),
                req.difficulty(), req.tipsAndSuggestions(), req.isShared());
        Activity saved = activityRepository.save(activity);

        replaceSkills(saved.getId(), req.skillIds());
        replaceLanguages(saved.getId(), req.languageIds());
        replaceProps(saved.getId(), req.propIds());
        replaceInstructions(saved.getId(), req.instructions());
        replaceChecklist(saved.getId(), req.checklist());
        replaceLinks(saved.getId(), req.links());

        return toResponse(saved, orgId);
    }

    @Transactional
    public ActivityResponse update(UUID id, UUID orgId, UpdateActivityRequest req) {
        Activity activity = requireOwned(id, orgId);

        if (req.title() != null) activity.setTitle(req.title().trim());
        if (req.aboutActivity() != null) activity.setAboutActivity(req.aboutActivity());
        if (req.programId() != null) activity.setProgramId(req.programId());
        if (req.durationWeeks() != null) activity.setDurationWeeks(req.durationWeeks());
        if (req.ageMinValue() != null) activity.setAgeMinValue(req.ageMinValue());
        if (req.ageMinUnit() != null) activity.setAgeMinUnit(req.ageMinUnit());
        if (req.ageMaxValue() != null) activity.setAgeMaxValue(req.ageMaxValue());
        if (req.ageMaxUnit() != null) activity.setAgeMaxUnit(req.ageMaxUnit());
        if (req.difficulty() != null) activity.setDifficulty(parseDifficulty(req.difficulty()));
        if (req.tipsAndSuggestions() != null) activity.setTipsAndSuggestions(req.tipsAndSuggestions());
        if (req.isShared() != null) activity.setShared(req.isShared());
        if (req.isActive() != null) activity.setActive(req.isActive());

        Activity saved = activityRepository.save(activity);

        if (req.skillIds() != null) replaceSkills(saved.getId(), req.skillIds());
        if (req.languageIds() != null) replaceLanguages(saved.getId(), req.languageIds());
        if (req.propIds() != null) replaceProps(saved.getId(), req.propIds());
        if (req.instructions() != null) replaceInstructions(saved.getId(), req.instructions());
        if (req.checklist() != null) replaceChecklist(saved.getId(), req.checklist());
        if (req.links() != null) replaceLinks(saved.getId(), req.links());

        return toResponse(saved, orgId);
    }

    @Transactional
    public void deactivate(UUID id, UUID orgId) {
        Activity activity = requireOwned(id, orgId);
        activity.setActive(false);
        activityRepository.save(activity);
    }

    private void applyCore(Activity a, String title, String about, UUID programId, Integer durationWeeks,
                            Integer ageMinValue, com.simplehearing.activity.enums.AgeUnit ageMinUnit,
                            Integer ageMaxValue, com.simplehearing.activity.enums.AgeUnit ageMaxUnit,
                            String difficulty, String tips, Boolean isShared) {
        a.setTitle(title.trim());
        a.setAboutActivity(about);
        a.setProgramId(programId);
        a.setDurationWeeks(durationWeeks);
        a.setAgeMinValue(ageMinValue);
        a.setAgeMinUnit(ageMinUnit);
        a.setAgeMaxValue(ageMaxValue);
        a.setAgeMaxUnit(ageMaxUnit);
        a.setDifficulty(parseDifficulty(difficulty));
        a.setTipsAndSuggestions(tips);
        a.setShared(Boolean.TRUE.equals(isShared));
    }

    private com.simplehearing.activity.enums.ActivityDifficulty parseDifficulty(String difficulty) {
        if (difficulty == null || difficulty.isBlank()) return com.simplehearing.activity.enums.ActivityDifficulty.EASY;
        try {
            return com.simplehearing.activity.enums.ActivityDifficulty.valueOf(difficulty.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid difficulty: " + difficulty);
        }
    }

    // ── Child-collection replace helpers ───────────────────────────────────

    void replaceSkills(UUID activityId, List<UUID> skillIds) {
        activitySkillRepository.deleteById_ActivityId(activityId);
        if (skillIds == null) return;
        skillIds.stream().distinct().forEach(sid -> activitySkillRepository.save(new ActivitySkill(activityId, sid)));
    }

    void replaceLanguages(UUID activityId, List<UUID> languageIds) {
        activityLanguageRepository.deleteById_ActivityId(activityId);
        if (languageIds == null) return;
        languageIds.stream().distinct().forEach(lid -> activityLanguageRepository.save(new ActivityLanguage(activityId, lid)));
    }

    void replaceProps(UUID activityId, List<UUID> propIds) {
        activityPropRepository.deleteById_ActivityId(activityId);
        if (propIds == null) return;
        propIds.stream().distinct().forEach(pid -> activityPropRepository.save(new ActivityProp(activityId, pid)));
    }

    void replaceInstructions(UUID activityId, List<String> instructions) {
        instructionRepository.deleteByActivityId(activityId);
        if (instructions == null) return;
        int i = 0;
        for (String text : instructions) {
            if (text == null || text.isBlank()) continue;
            ActivityInstruction instr = new ActivityInstruction();
            instr.setActivityId(activityId);
            instr.setOrderIndex(i++);
            instr.setText(text);
            instructionRepository.save(instr);
        }
    }

    void replaceChecklist(UUID activityId, List<ChecklistQuestionInput> checklist) {
        List<ActivityChecklistQuestion> existing = questionRepository.findByActivityIdOrderByOrderIndexAsc(activityId);
        questionRepository.deleteAll(existing);
        if (checklist == null) return;
        int qi = 0;
        for (ChecklistQuestionInput q : checklist) {
            if (q.questionText() == null || q.questionText().isBlank()) continue;
            ActivityChecklistQuestion question = new ActivityChecklistQuestion();
            question.setActivityId(activityId);
            question.setOrderIndex(qi++);
            question.setQuestionText(q.questionText());
            question.setQuestionType(q.questionType() != null ? q.questionType() : ChecklistQuestionType.SINGLE_CHOICE);
            ActivityChecklistQuestion savedQ = questionRepository.save(question);

            if (q.options() != null) {
                int oi = 0;
                for (String optText : q.options()) {
                    if (optText == null || optText.isBlank()) continue;
                    ActivityChecklistOption opt = new ActivityChecklistOption();
                    opt.setQuestionId(savedQ.getId());
                    opt.setOrderIndex(oi++);
                    opt.setOptionText(optText);
                    optionRepository.save(opt);
                }
            }
        }
    }

    void replaceLinks(UUID activityId, List<String> links) {
        linkRepository.deleteByActivityId(activityId);
        if (links == null) return;
        int i = 0;
        for (String url : links) {
            if (url == null || url.isBlank()) continue;
            ActivityLink link = new ActivityLink();
            link.setActivityId(activityId);
            link.setOrderIndex(i++);
            link.setUrl(url);
            linkRepository.save(link);
        }
    }

    // ── Response assembly ──────────────────────────────────────────────────

    ActivityResponse toResponse(Activity a, UUID viewerOrgId) {
        String programName = a.getProgramId() == null ? null :
                programRepository.findById(a.getProgramId()).map(com.simplehearing.program.entity.Program::getName).orElse(null);
        String orgName = organisationRepository.findById(a.getOrgId())
                .map(com.simplehearing.organisation.entity.Organisation::getName).orElse(null);

        List<SkillResponse> skills = activitySkillRepository.findById_ActivityId(a.getId()).stream()
                .map(ActivitySkill::getSkillId).map(skillRepository::findById)
                .flatMap(Optional::stream).map(SkillResponse::from).toList();

        List<LanguageResponse> languages = activityLanguageRepository.findById_ActivityId(a.getId()).stream()
                .map(ActivityLanguage::getLanguageId).map(languageRepository::findById)
                .flatMap(Optional::stream).map(LanguageResponse::from).toList();

        List<PropResponse> props = activityPropRepository.findById_ActivityId(a.getId()).stream()
                .map(ActivityProp::getPropId).map(propRepository::findById)
                .flatMap(Optional::stream).map(PropResponse::from).toList();

        List<String> instructions = instructionRepository.findByActivityIdOrderByOrderIndexAsc(a.getId())
                .stream().map(ActivityInstruction::getText).toList();

        List<ChecklistQuestionResponse> checklist = questionRepository.findByActivityIdOrderByOrderIndexAsc(a.getId())
                .stream().map(q -> ChecklistQuestionResponse.from(q,
                        optionRepository.findByQuestionIdOrderByOrderIndexAsc(q.getId()).stream()
                                .map(ChecklistOptionResponse::from).toList()))
                .toList();

        List<ActivityResourceResponse> resources = resourceRepository.findByActivityIdOrderByCreatedAtAsc(a.getId())
                .stream().map(ActivityResourceResponse::from).toList();

        List<String> links = linkRepository.findByActivityIdOrderByOrderIndexAsc(a.getId())
                .stream().map(ActivityLink::getUrl).toList();

        boolean mine = a.getOrgId().equals(viewerOrgId);

        return ActivityResponse.from(a, orgName, mine, programName, skills, languages,
                instructions, checklist, props, resources, links);
    }

    // ── Assignments ─────────────────────────────────────────────────────────

    @Transactional
    public ActivityAssignmentResponse assign(UUID activityId, UUID orgId, UUID userId, AssignActivityRequest req) {
        Activity activity = requireOwned(activityId, orgId);
        Patient patient = patientRepository.findByIdAndOrgId(req.patientId(), orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Patient not found"));

        ActivityAssignment assignment = new ActivityAssignment();
        assignment.setOrgId(orgId);
        assignment.setActivityId(activity.getId());
        assignment.setPatientId(patient.getId());
        assignment.setAssignedBy(userId);
        assignment.setAssignedTherapistId(req.assignedTherapistId());
        LocalDate start = req.startDate() != null ? req.startDate() : LocalDate.now();
        assignment.setStartDate(start);
        if (activity.getDurationWeeks() != null) {
            assignment.setDueDate(start.plusWeeks(activity.getDurationWeeks()));
        }
        ActivityAssignment saved = assignmentRepository.save(assignment);
        return toAssignmentResponse(saved);
    }

    public List<ActivityAssignmentResponse> listAssignmentsForPatient(UUID orgId, UUID patientId) {
        return assignmentRepository.findByOrgIdAndPatientIdOrderByCreatedAtDesc(orgId, patientId)
                .stream().map(this::toAssignmentResponse).toList();
    }

    ActivityAssignment requireOwnedAssignment(UUID assignmentId, UUID orgId) {
        return assignmentRepository.findByIdAndOrgId(assignmentId, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Assignment not found"));
    }

    @Transactional
    public ActivityAssignmentResponse updateAssignmentStatus(UUID assignmentId, UUID orgId, AssignmentStatus status) {
        ActivityAssignment assignment = requireOwnedAssignment(assignmentId, orgId);
        assignment.setStatus(status);
        ActivityAssignment saved = assignmentRepository.save(assignment);
        return toAssignmentResponse(saved);
    }

    private ActivityAssignmentResponse toAssignmentResponse(ActivityAssignment a) {
        String activityTitle = activityRepository.findById(a.getActivityId()).map(Activity::getTitle).orElse(null);
        String patientName = patientRepository.findById(a.getPatientId())
                .map(p -> fullName(p.getFirstName(), p.getLastName())).orElse(null);
        String assignedByName = userName(a.getAssignedBy());
        String assignedTherapistName = a.getAssignedTherapistId() != null ? userName(a.getAssignedTherapistId()) : null;
        int attemptCount = attemptLogRepository.findByAssignmentIdOrderByAttemptDateDesc(a.getId()).size();
        return ActivityAssignmentResponse.from(a, activityTitle, patientName, assignedByName, assignedTherapistName, attemptCount);
    }

    private String userName(UUID userId) {
        return userRepository.findById(userId).map(u -> fullName(u.getFirstName(), u.getLastName())).orElse(null);
    }

    private String fullName(String first, String last) {
        return ((first == null ? "" : first) + " " + (last == null ? "" : last)).trim();
    }

    // ── Attempts ────────────────────────────────────────────────────────────

    @Transactional
    public ActivityAttemptResponse logAttempt(UUID assignmentId, UUID orgId, UUID userId, LogAttemptRequest req) {
        ActivityAssignment assignment = requireOwnedAssignment(assignmentId, orgId);

        ActivityAttemptLog log = new ActivityAttemptLog();
        log.setOrgId(orgId);
        log.setAssignmentId(assignment.getId());
        log.setLoggedBy(userId);
        log.setAttemptDate(req.attemptDate());
        log.setNote(req.note());
        ActivityAttemptLog savedLog = attemptLogRepository.save(log);

        if (req.answers() != null) {
            for (AttemptAnswerInput ans : req.answers()) {
                ActivityAttemptAnswer answer = new ActivityAttemptAnswer();
                answer.setAttemptLogId(savedLog.getId());
                answer.setQuestionId(ans.questionId());
                answer.setTextAnswer(ans.textAnswer());
                ActivityAttemptAnswer savedAnswer = answerRepository.save(answer);

                if (ans.selectedOptionIds() != null) {
                    for (UUID optionId : ans.selectedOptionIds()) {
                        answerOptionRepository.save(new ActivityAttemptAnswerOption(savedAnswer.getId(), optionId));
                    }
                }
            }
        }

        if (assignment.getStatus() == AssignmentStatus.ASSIGNED) {
            assignment.setStatus(AssignmentStatus.IN_PROGRESS);
            assignmentRepository.save(assignment);
        }

        return toAttemptResponse(savedLog);
    }

    public List<ActivityAttemptResponse> listAttempts(UUID assignmentId, UUID orgId) {
        requireOwnedAssignment(assignmentId, orgId);
        return attemptLogRepository.findByAssignmentIdOrderByAttemptDateDesc(assignmentId)
                .stream().map(this::toAttemptResponse).toList();
    }

    private ActivityAttemptResponse toAttemptResponse(ActivityAttemptLog log) {
        String loggedByName = userName(log.getLoggedBy());
        List<ActivityAttemptAnswer> answers = answerRepository.findByAttemptLogId(log.getId());
        List<AttemptAnswerResponse> answerResponses = answers.stream().map(ans -> {
            ActivityChecklistQuestion question = questionRepository.findById(ans.getQuestionId()).orElse(null);
            List<ActivityChecklistOption> selected = answerOptionRepository.findById_AnswerId(ans.getId()).stream()
                    .map(ActivityAttemptAnswerOption::getOptionId)
                    .map(optionRepository::findById).flatMap(Optional::stream).toList();
            return new AttemptAnswerResponse(
                    ans.getQuestionId(),
                    question != null ? question.getQuestionText() : null,
                    selected.stream().map(ActivityChecklistOption::getId).toList(),
                    selected.stream().map(ActivityChecklistOption::getOptionText).toList(),
                    ans.getTextAnswer());
        }).toList();
        return ActivityAttemptResponse.from(log, loggedByName, answerResponses);
    }
}

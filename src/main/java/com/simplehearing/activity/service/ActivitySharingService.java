package com.simplehearing.activity.service;

import com.simplehearing.activity.dto.ActivityResponse;
import com.simplehearing.activity.entity.*;
import com.simplehearing.activity.repository.*;
import com.simplehearing.common.exception.ApiException;
import com.simplehearing.common.exception.ResourceNotFoundException;
import com.simplehearing.program.entity.Program;
import com.simplehearing.program.repository.ProgramRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/** Cross-org "browse and clone" sharing. Shared activities stay owned by their creating org;
 *  importing deep-copies the content into the caller's org rather than granting cross-org access. */
@Service
public class ActivitySharingService {

    private final ActivityRepository activityRepository;
    private final ActivityService activityService;
    private final ActivityInstructionRepository instructionRepository;
    private final ActivityChecklistQuestionRepository questionRepository;
    private final ActivityChecklistOptionRepository optionRepository;
    private final ActivityResourceRepository resourceRepository;
    private final ActivityLinkRepository linkRepository;
    private final SkillRepository skillRepository;
    private final LanguageRepository languageRepository;
    private final PropRepository propRepository;
    private final ActivitySkillRepository activitySkillRepository;
    private final ActivityLanguageRepository activityLanguageRepository;
    private final ActivityPropRepository activityPropRepository;
    private final ProgramRepository programRepository;

    public ActivitySharingService(ActivityRepository activityRepository, ActivityService activityService,
                                   ActivityInstructionRepository instructionRepository,
                                   ActivityChecklistQuestionRepository questionRepository,
                                   ActivityChecklistOptionRepository optionRepository,
                                   ActivityResourceRepository resourceRepository,
                                   ActivityLinkRepository linkRepository,
                                   SkillRepository skillRepository, LanguageRepository languageRepository,
                                   PropRepository propRepository, ActivitySkillRepository activitySkillRepository,
                                   ActivityLanguageRepository activityLanguageRepository,
                                   ActivityPropRepository activityPropRepository, ProgramRepository programRepository) {
        this.activityRepository = activityRepository;
        this.activityService = activityService;
        this.instructionRepository = instructionRepository;
        this.questionRepository = questionRepository;
        this.optionRepository = optionRepository;
        this.resourceRepository = resourceRepository;
        this.linkRepository = linkRepository;
        this.skillRepository = skillRepository;
        this.languageRepository = languageRepository;
        this.propRepository = propRepository;
        this.activitySkillRepository = activitySkillRepository;
        this.activityLanguageRepository = activityLanguageRepository;
        this.activityPropRepository = activityPropRepository;
        this.programRepository = programRepository;
    }

    public List<ActivityResponse> sharedLibrary(UUID viewerOrgId) {
        return activityRepository.findByIsSharedTrueAndIsActiveTrueOrderByCreatedAtDesc().stream()
                .map(a -> activityService.toResponse(a, viewerOrgId))
                .toList();
    }

    @Transactional
    public ActivityResponse importActivity(UUID sourceActivityId, UUID destOrgId, UUID userId) {
        Activity source = activityRepository.findById(sourceActivityId)
                .orElseThrow(() -> new ResourceNotFoundException("Activity not found"));
        if (!source.isShared() || !source.isActive()) {
            throw new ApiException(HttpStatus.FORBIDDEN, "This activity is not shared");
        }

        Activity clone = new Activity();
        clone.setOrgId(destOrgId);
        clone.setCreatedBy(userId);
        clone.setTitle(source.getTitle());
        clone.setAboutActivity(source.getAboutActivity());
        clone.setProgramId(matchProgram(source.getProgramId(), destOrgId));
        clone.setDurationWeeks(source.getDurationWeeks());
        clone.setAgeMinValue(source.getAgeMinValue());
        clone.setAgeMinUnit(source.getAgeMinUnit());
        clone.setAgeMaxValue(source.getAgeMaxValue());
        clone.setAgeMaxUnit(source.getAgeMaxUnit());
        clone.setDifficulty(source.getDifficulty());
        clone.setTipsAndSuggestions(source.getTipsAndSuggestions());
        clone.setShared(false);
        clone.setSourceActivityId(source.getId());
        Activity saved = activityRepository.save(clone);

        activitySkillRepository.findById_ActivityId(source.getId()).stream()
                .map(ActivitySkill::getSkillId).map(skillRepository::findById).flatMap(java.util.Optional::stream)
                .forEach(skill -> activitySkillRepository.save(new ActivitySkill(saved.getId(), matchOrCreateSkill(skill.getName(), destOrgId))));

        activityLanguageRepository.findById_ActivityId(source.getId()).stream()
                .map(ActivityLanguage::getLanguageId).map(languageRepository::findById).flatMap(java.util.Optional::stream)
                .forEach(lang -> activityLanguageRepository.save(new ActivityLanguage(saved.getId(), matchOrCreateLanguage(lang.getName(), destOrgId))));

        activityPropRepository.findById_ActivityId(source.getId()).stream()
                .map(ActivityProp::getPropId).map(propRepository::findById).flatMap(java.util.Optional::stream)
                .forEach(prop -> activityPropRepository.save(new ActivityProp(saved.getId(), matchOrCreateProp(prop.getName(), destOrgId))));

        instructionRepository.findByActivityIdOrderByOrderIndexAsc(source.getId()).forEach(instr -> {
            ActivityInstruction copy = new ActivityInstruction();
            copy.setActivityId(saved.getId());
            copy.setOrderIndex(instr.getOrderIndex());
            copy.setText(instr.getText());
            instructionRepository.save(copy);
        });

        questionRepository.findByActivityIdOrderByOrderIndexAsc(source.getId()).forEach(q -> {
            ActivityChecklistQuestion qCopy = new ActivityChecklistQuestion();
            qCopy.setActivityId(saved.getId());
            qCopy.setOrderIndex(q.getOrderIndex());
            qCopy.setQuestionText(q.getQuestionText());
            qCopy.setQuestionType(q.getQuestionType());
            ActivityChecklistQuestion savedQ = questionRepository.save(qCopy);

            optionRepository.findByQuestionIdOrderByOrderIndexAsc(q.getId()).forEach(o -> {
                ActivityChecklistOption oCopy = new ActivityChecklistOption();
                oCopy.setQuestionId(savedQ.getId());
                oCopy.setOrderIndex(o.getOrderIndex());
                oCopy.setOptionText(o.getOptionText());
                optionRepository.save(oCopy);
            });
        });

        resourceRepository.findByActivityIdOrderByCreatedAtAsc(source.getId()).forEach(r -> {
            ActivityResource copy = new ActivityResource();
            copy.setOrgId(destOrgId);
            copy.setActivityId(saved.getId());
            copy.setUploadedBy(userId);
            copy.setFileName(r.getFileName());
            copy.setFileUrl(r.getFileUrl());
            copy.setContentType(r.getContentType());
            copy.setFileSizeBytes(r.getFileSizeBytes());
            resourceRepository.save(copy);
        });

        linkRepository.findByActivityIdOrderByOrderIndexAsc(source.getId()).forEach(l -> {
            ActivityLink copy = new ActivityLink();
            copy.setActivityId(saved.getId());
            copy.setOrderIndex(l.getOrderIndex());
            copy.setUrl(l.getUrl());
            linkRepository.save(copy);
        });

        return activityService.toResponse(saved, destOrgId);
    }

    /** Programs carry pricing set by each clinic, so importing never invents one — if the
     *  destination org doesn't already run a program with this name, the clone is left unset
     *  and the importer picks one of their own. */
    private UUID matchProgram(UUID sourceProgramId, UUID destOrgId) {
        if (sourceProgramId == null) return null;
        Program source = programRepository.findById(sourceProgramId).orElse(null);
        if (source == null) return null;
        return programRepository.findByOrgIdAndIsActiveTrueOrderByNameAsc(destOrgId).stream()
                .filter(p -> p.getName().equalsIgnoreCase(source.getName())).findFirst()
                .map(Program::getId)
                .orElse(null);
    }

    private UUID matchOrCreateSkill(String name, UUID destOrgId) {
        return skillRepository.findByOrgIdAndNameIgnoreCase(destOrgId, name).map(Skill::getId)
                .orElseGet(() -> {
                    Skill s = new Skill();
                    s.setOrgId(destOrgId);
                    s.setName(name);
                    return skillRepository.save(s).getId();
                });
    }

    private UUID matchOrCreateLanguage(String name, UUID destOrgId) {
        return languageRepository.findByOrgIdAndNameIgnoreCase(destOrgId, name).map(Language::getId)
                .orElseGet(() -> {
                    Language l = new Language();
                    l.setOrgId(destOrgId);
                    l.setName(name);
                    return languageRepository.save(l).getId();
                });
    }

    private UUID matchOrCreateProp(String name, UUID destOrgId) {
        return propRepository.findByOrgIdAndNameIgnoreCase(destOrgId, name).map(Prop::getId)
                .orElseGet(() -> {
                    Prop p = new Prop();
                    p.setOrgId(destOrgId);
                    p.setName(name);
                    return propRepository.save(p).getId();
                });
    }
}

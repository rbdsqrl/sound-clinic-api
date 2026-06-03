package com.simplehearing.iep.service;

import com.simplehearing.auth.security.UserPrincipal;
import com.simplehearing.common.exception.ResourceNotFoundException;
import com.simplehearing.iep.dto.*;
import com.simplehearing.iep.entity.IEPTemplate;
import com.simplehearing.iep.entity.IEPTemplateGoal;
import com.simplehearing.iep.repository.IEPTemplateGoalRepository;
import com.simplehearing.iep.repository.IEPTemplateRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class IEPTemplateService {

    private final IEPTemplateRepository templateRepository;
    private final IEPTemplateGoalRepository goalRepository;

    public IEPTemplateService(IEPTemplateRepository templateRepository,
                               IEPTemplateGoalRepository goalRepository) {
        this.templateRepository = templateRepository;
        this.goalRepository = goalRepository;
    }

    public List<IEPTemplateResponse> listTemplates(UserPrincipal principal) {
        List<IEPTemplate> templates = templateRepository.findByOrgIdOrderByCreatedAtDesc(principal.getOrgId());
        return templates.stream()
                .map(t -> IEPTemplateResponse.from(t, goalRepository.findByTemplateIdOrderByCreatedAtAsc(t.getId())))
                .collect(Collectors.toList());
    }

    @Transactional
    public IEPTemplateResponse createTemplate(CreateIEPTemplateRequest req, UserPrincipal principal) {
        IEPTemplate template = new IEPTemplate();
        template.setOrgId(principal.getOrgId());
        template.setName(req.name());
        template.setDescription(req.description());
        template.setTags(joinTags(req.tags()));

        IEPTemplate saved = templateRepository.save(template);
        return IEPTemplateResponse.from(saved, List.of());
    }

    @Transactional
    public IEPTemplateResponse updateTemplate(UUID id, UpdateIEPTemplateRequest req, UserPrincipal principal) {
        IEPTemplate template = templateRepository.findByIdAndOrgId(id, principal.getOrgId())
                .orElseThrow(() -> new ResourceNotFoundException("IEP template not found"));

        if (req.name() != null) template.setName(req.name());
        if (req.description() != null) template.setDescription(req.description());
        if (req.tags() != null) template.setTags(joinTags(req.tags()));

        IEPTemplate saved = templateRepository.save(template);
        List<IEPTemplateGoal> goals = goalRepository.findByTemplateIdOrderByCreatedAtAsc(saved.getId());
        return IEPTemplateResponse.from(saved, goals);
    }

    @Transactional
    public void deleteTemplate(UUID id, UserPrincipal principal) {
        IEPTemplate template = templateRepository.findByIdAndOrgId(id, principal.getOrgId())
                .orElseThrow(() -> new ResourceNotFoundException("IEP template not found"));
        templateRepository.delete(template);
    }

    @Transactional
    public IEPTemplateResponse addGoal(UUID templateId, CreateIEPTemplateGoalRequest req, UserPrincipal principal) {
        IEPTemplate template = templateRepository.findByIdAndOrgId(templateId, principal.getOrgId())
                .orElseThrow(() -> new ResourceNotFoundException("IEP template not found"));

        IEPTemplateGoal goal = new IEPTemplateGoal();
        goal.setTemplateId(template.getId());
        goal.setOrgId(principal.getOrgId());
        goal.setTitle(req.title());
        goal.setGoalStatement(req.goalStatement());
        goal.setDomain(req.domain());
        goal.setBaseline(req.baseline());
        goal.setTargetCriteria(req.targetCriteria());

        goalRepository.save(goal);

        List<IEPTemplateGoal> goals = goalRepository.findByTemplateIdOrderByCreatedAtAsc(template.getId());
        return IEPTemplateResponse.from(template, goals);
    }

    @Transactional
    public void deleteGoal(UUID goalId, UserPrincipal principal) {
        IEPTemplateGoal goal = goalRepository.findByIdAndOrgId(goalId, principal.getOrgId())
                .orElseThrow(() -> new ResourceNotFoundException("IEP template goal not found"));
        goalRepository.delete(goal);
    }

    private String joinTags(List<String> tags) {
        if (tags == null || tags.isEmpty()) return null;
        return tags.stream()
                .map(String::trim)
                .filter(t -> !t.isBlank())
                .collect(Collectors.joining(","));
    }
}

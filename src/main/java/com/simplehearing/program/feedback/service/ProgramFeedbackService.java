package com.simplehearing.program.feedback.service;

import com.simplehearing.program.feedback.dto.*;
import com.simplehearing.program.feedback.entity.*;
import com.simplehearing.program.feedback.enums.FeedbackQuestionType;
import com.simplehearing.program.feedback.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class ProgramFeedbackService {

    private final ProgramFeedbackQuestionRepository questionRepository;
    private final ProgramFeedbackOptionRepository optionRepository;
    private final SessionFeedbackAnswerRepository answerRepository;
    private final SessionFeedbackAnswerOptionRepository answerOptionRepository;

    public ProgramFeedbackService(ProgramFeedbackQuestionRepository questionRepository,
                                   ProgramFeedbackOptionRepository optionRepository,
                                   SessionFeedbackAnswerRepository answerRepository,
                                   SessionFeedbackAnswerOptionRepository answerOptionRepository) {
        this.questionRepository = questionRepository;
        this.optionRepository = optionRepository;
        this.answerRepository = answerRepository;
        this.answerOptionRepository = answerOptionRepository;
    }

    // ── Template (per program) ─────────────────────────────────────────────────

    public List<ProgramFeedbackQuestionResponse> getTemplate(UUID programId) {
        return questionRepository.findByProgramIdOrderByOrderIndexAsc(programId).stream()
                .map(q -> ProgramFeedbackQuestionResponse.from(q,
                        optionRepository.findByQuestionIdOrderByOrderIndexAsc(q.getId()).stream()
                                .map(ProgramFeedbackOptionResponse::from).toList()))
                .toList();
    }

    @Transactional
    public List<ProgramFeedbackQuestionResponse> replaceTemplate(UUID programId, UUID orgId, UpdateProgramFeedbackTemplateRequest request) {
        List<ProgramFeedbackQuestion> existing = questionRepository.findByProgramIdOrderByOrderIndexAsc(programId);
        questionRepository.deleteAll(existing);

        List<ProgramFeedbackQuestionInput> questions = request.questions();
        if (questions != null) {
            int qi = 0;
            for (ProgramFeedbackQuestionInput q : questions) {
                if (q.questionText() == null || q.questionText().isBlank()) continue;
                ProgramFeedbackQuestion question = new ProgramFeedbackQuestion();
                question.setOrgId(orgId);
                question.setProgramId(programId);
                question.setOrderIndex(qi++);
                question.setQuestionText(q.questionText().trim());
                question.setQuestionType(q.questionType() != null ? q.questionType() : FeedbackQuestionType.MULTI_CHOICE);
                ProgramFeedbackQuestion savedQ = questionRepository.save(question);

                if (q.options() != null) {
                    int oi = 0;
                    for (String optText : q.options()) {
                        if (optText == null || optText.isBlank()) continue;
                        ProgramFeedbackOption opt = new ProgramFeedbackOption();
                        opt.setQuestionId(savedQ.getId());
                        opt.setOrderIndex(oi++);
                        opt.setOptionText(optText.trim());
                        optionRepository.save(opt);
                    }
                }
            }
        }

        return getTemplate(programId);
    }

    // ── Session fill ────────────────────────────────────────────────────────────

    public List<SessionFeedbackAnswerResponse> getSessionAnswers(UUID sessionId) {
        return answerRepository.findBySessionId(sessionId).stream()
                .map(ans -> new SessionFeedbackAnswerResponse(
                        ans.getQuestionId(),
                        answerOptionRepository.findById_AnswerId(ans.getId()).stream()
                                .map(SessionFeedbackAnswerOption::getOptionId).toList(),
                        ans.getTextAnswer()))
                .toList();
    }

    @Transactional
    public void replaceSessionAnswers(UUID sessionId, List<SessionFeedbackAnswerInput> answers) {
        answerRepository.deleteBySessionId(sessionId);
        if (answers == null) return;
        for (SessionFeedbackAnswerInput ans : answers) {
            SessionFeedbackAnswer answer = new SessionFeedbackAnswer();
            answer.setSessionId(sessionId);
            answer.setQuestionId(ans.questionId());
            answer.setTextAnswer(ans.textAnswer());
            SessionFeedbackAnswer savedAnswer = answerRepository.save(answer);

            if (ans.selectedOptionIds() != null) {
                for (UUID optionId : ans.selectedOptionIds()) {
                    answerOptionRepository.save(new SessionFeedbackAnswerOption(savedAnswer.getId(), optionId));
                }
            }
        }
    }
}

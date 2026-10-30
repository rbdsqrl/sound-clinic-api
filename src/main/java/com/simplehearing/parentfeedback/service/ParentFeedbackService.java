package com.simplehearing.parentfeedback.service;

import com.simplehearing.enrollment.entity.Enrollment;
import com.simplehearing.enrollment.repository.EnrollmentRepository;
import com.simplehearing.parentfeedback.dto.ParentFeedbackResponse;
import com.simplehearing.parentfeedback.entity.ParentSessionFeedback;
import com.simplehearing.patient.entity.Patient;
import com.simplehearing.patient.repository.PatientRepository;
import com.simplehearing.program.entity.Program;
import com.simplehearing.program.repository.ProgramRepository;
import com.simplehearing.session.entity.TherapySession;
import com.simplehearing.session.repository.TherapySessionRepository;
import com.simplehearing.subscription.entity.Subscription;
import com.simplehearing.subscription.repository.SubscriptionRepository;
import com.simplehearing.user.entity.User;
import com.simplehearing.user.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Turns stored feedback into response rows, with the session, child, program, therapist and parent names batched in. */
@Service
public class ParentFeedbackService {

    private final TherapySessionRepository sessionRepository;
    private final PatientRepository patientRepository;
    private final UserRepository userRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final ProgramRepository programRepository;

    public ParentFeedbackService(TherapySessionRepository sessionRepository,
                                 PatientRepository patientRepository,
                                 UserRepository userRepository,
                                 EnrollmentRepository enrollmentRepository,
                                 SubscriptionRepository subscriptionRepository,
                                 ProgramRepository programRepository) {
        this.sessionRepository = sessionRepository;
        this.patientRepository = patientRepository;
        this.userRepository = userRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.programRepository = programRepository;
    }

    public List<ParentFeedbackResponse> toResponses(List<ParentSessionFeedback> feedback) {
        if (feedback.isEmpty()) return List.of();

        Map<UUID, TherapySession> sessions = sessionRepository
                .findAllById(feedback.stream().map(ParentSessionFeedback::getSessionId).collect(Collectors.toSet())).stream()
                .collect(Collectors.toMap(TherapySession::getId, Function.identity()));
        Map<UUID, Patient> patients = patientRepository
                .findAllById(feedback.stream().map(ParentSessionFeedback::getPatientId).collect(Collectors.toSet())).stream()
                .collect(Collectors.toMap(Patient::getId, Function.identity()));

        Set<UUID> userIds = new HashSet<>();
        feedback.forEach(f -> userIds.add(f.getParentId()));
        sessions.values().forEach(s -> userIds.add(s.getTherapistId()));
        Map<UUID, User> users = userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));

        // enrollment -> subscription -> program name, batched
        Map<UUID, Enrollment> enrollments = enrollmentRepository
                .findAllById(sessions.values().stream().map(TherapySession::getEnrollmentId).collect(Collectors.toSet())).stream()
                .collect(Collectors.toMap(Enrollment::getId, Function.identity()));
        Map<UUID, Subscription> subs = subscriptionRepository
                .findAllById(enrollments.values().stream().map(Enrollment::getSubscriptionId).collect(Collectors.toSet())).stream()
                .collect(Collectors.toMap(Subscription::getId, Function.identity()));
        Map<UUID, Program> programs = programRepository
                .findAllById(subs.values().stream().map(Subscription::getProgramId).collect(Collectors.toSet())).stream()
                .collect(Collectors.toMap(Program::getId, Function.identity()));
        Map<UUID, String> programNameBySession = new HashMap<>();
        sessions.values().forEach(s -> {
            Enrollment e = enrollments.get(s.getEnrollmentId());
            Subscription sub = e == null ? null : subs.get(e.getSubscriptionId());
            Program p = sub == null ? null : programs.get(sub.getProgramId());
            programNameBySession.put(s.getId(), p != null ? p.getName() : "Program");
        });

        return feedback.stream().map(f -> {
            TherapySession s = sessions.get(f.getSessionId());
            Patient patient = patients.get(f.getPatientId());
            User therapist = s == null ? null : users.get(s.getTherapistId());
            User parent = users.get(f.getParentId());
            return new ParentFeedbackResponse(
                    f.getId(), f.getSessionId(),
                    s != null ? s.getSessionNumber() : 0,
                    s != null ? s.getSessionDate() : null,
                    s != null ? programNameBySession.get(s.getId()) : "Program",
                    f.getPatientId(),
                    patient != null ? patient.getFirstName() : "", patient != null ? patient.getLastName() : "",
                    therapist != null ? therapist.getFirstName() : "", therapist != null ? therapist.getLastName() : "",
                    parent != null ? parent.getFirstName() : "", parent != null ? parent.getLastName() : "",
                    f.getMessage(), f.getCreatedAt(), f.getUpdatedAt(),
                    f.isSharedWithTherapist(), f.getSharedAt(), f.getAdminReviewedAt() != null);
        }).toList();
    }
}

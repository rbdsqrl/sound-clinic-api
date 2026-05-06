package com.simplehearing.session.service;

import com.simplehearing.enrollment.entity.Enrollment;
import com.simplehearing.session.entity.TherapySession;
import com.simplehearing.session.repository.TherapySessionRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class SessionGenerationService {

    private final TherapySessionRepository sessionRepository;

    public SessionGenerationService(TherapySessionRepository sessionRepository) {
        this.sessionRepository = sessionRepository;
    }

    /**
     * Generates {@code numSessions} daily {@link TherapySession} records starting from
     * {@code enrollment.getStartDate()}, one per consecutive day.
     */
    public void generateSessions(Enrollment enrollment, int numSessions) {
        LocalDate startDate = enrollment.getStartDate();
        LocalTime startTime = enrollment.getStartTime();
        LocalTime endTime   = startTime.plusMinutes(enrollment.getSessionDurationMinutes());

        List<TherapySession> sessions = new ArrayList<>(numSessions);
        for (int i = 0; i < numSessions; i++) {
            TherapySession s = new TherapySession();
            s.setOrgId(enrollment.getOrgId());
            s.setEnrollmentId(enrollment.getId());
            s.setPatientId(enrollment.getPatientId());
            s.setTherapistId(enrollment.getTherapistId());
            s.setSessionNumber(i + 1);
            s.setSessionDate(startDate.plusDays(i));
            s.setStartTime(startTime);
            s.setEndTime(endTime);
            sessions.add(s);
        }

        sessionRepository.saveAll(sessions);
    }
}

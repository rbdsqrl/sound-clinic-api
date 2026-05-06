package com.simplehearing.session.service;

import com.simplehearing.enrollment.entity.Enrollment;
import com.simplehearing.session.entity.TherapySession;
import com.simplehearing.session.repository.TherapySessionRepository;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
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
     * Generates {@code numSessions} weekly {@link TherapySession} records for the given enrollment.
     * <p>
     * The first session falls on the first occurrence of {@code enrollment.getDayOfWeek()}
     * that is on or after {@code enrollment.getStartDate()}.  Subsequent sessions are each
     * one week later.
     */
    public void generateSessions(Enrollment enrollment, int numSessions) {
        DayOfWeek targetDay = enrollment.getDayOfWeek();
        LocalDate firstDate = nextOrSameDayOfWeek(enrollment.getStartDate(), targetDay);

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
            s.setSessionDate(firstDate.plusWeeks(i));
            s.setStartTime(startTime);
            s.setEndTime(endTime);
            sessions.add(s);
        }

        sessionRepository.saveAll(sessions);
    }

    /** Returns {@code date} itself if it falls on {@code day}, otherwise advances to the next occurrence. */
    private LocalDate nextOrSameDayOfWeek(LocalDate date, DayOfWeek day) {
        int daysUntil = (day.getValue() - date.getDayOfWeek().getValue() + 7) % 7;
        return date.plusDays(daysUntil);
    }
}

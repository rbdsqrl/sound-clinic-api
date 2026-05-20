package com.simplehearing.session.service;

import com.simplehearing.enrollment.entity.Enrollment;
import com.simplehearing.holiday.repository.PublicHolidayRepository;
import com.simplehearing.session.entity.TherapySession;
import com.simplehearing.session.repository.TherapySessionRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class SessionGenerationService {

    private final TherapySessionRepository sessionRepository;
    private final PublicHolidayRepository holidayRepository;

    public SessionGenerationService(TherapySessionRepository sessionRepository,
                                    PublicHolidayRepository holidayRepository) {
        this.sessionRepository = sessionRepository;
        this.holidayRepository = holidayRepository;
    }

    /**
     * Generates {@code numSessions} {@link TherapySession} records starting from
     * {@code enrollment.getStartDate()}, advancing one day at a time and skipping
     * any dates that are public holidays for the organisation.
     */
    public void generateSessions(Enrollment enrollment, int numSessions) {
        LocalTime startTime = enrollment.getStartTime();
        LocalTime endTime   = startTime.plusMinutes(enrollment.getSessionDurationMinutes());

        // Fetch all holiday dates for this org to use as a skip set
        Set<LocalDate> holidays = holidayRepository
                .findByOrgIdOrderByHolidayDateAsc(enrollment.getOrgId())
                .stream()
                .map(h -> h.getHolidayDate())
                .collect(Collectors.toSet());

        List<TherapySession> sessions = new ArrayList<>(numSessions);
        LocalDate date = enrollment.getStartDate();
        for (int i = 0; i < numSessions; i++) {
            // Skip past any public holidays
            while (holidays.contains(date)) {
                date = date.plusDays(1);
            }
            TherapySession s = new TherapySession();
            s.setOrgId(enrollment.getOrgId());
            s.setEnrollmentId(enrollment.getId());
            s.setPatientId(enrollment.getPatientId());
            s.setTherapistId(enrollment.getTherapistId());
            s.setSessionNumber(i + 1);
            s.setSessionDate(date);
            s.setStartTime(startTime);
            s.setEndTime(endTime);
            sessions.add(s);
            date = date.plusDays(1);
        }

        sessionRepository.saveAll(sessions);
    }
}

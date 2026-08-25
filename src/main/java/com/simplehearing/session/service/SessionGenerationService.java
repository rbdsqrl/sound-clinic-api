package com.simplehearing.session.service;

import com.simplehearing.enrollment.entity.Enrollment;
import com.simplehearing.holiday.repository.PublicHolidayRepository;
import com.simplehearing.organisation.entity.Organisation;
import com.simplehearing.organisation.repository.OrganisationRepository;
import com.simplehearing.session.entity.TherapySession;
import com.simplehearing.session.repository.TherapySessionRepository;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class SessionGenerationService {

    private final TherapySessionRepository sessionRepository;
    private final PublicHolidayRepository holidayRepository;
    private final OrganisationRepository organisationRepository;

    public SessionGenerationService(TherapySessionRepository sessionRepository,
                                    PublicHolidayRepository holidayRepository,
                                    OrganisationRepository organisationRepository) {
        this.sessionRepository = sessionRepository;
        this.holidayRepository = holidayRepository;
        this.organisationRepository = organisationRepository;
    }

    /**
     * Generates {@code numSessions} {@link TherapySession} records starting from
     * {@code enrollment.getStartDate()}, advancing one day at a time and skipping
     * any dates that are public holidays or the org's weekly off days.
     *
     * @return the saved sessions in date order — the last one's date is the plan's real end
     */
    public List<TherapySession> generateSessions(Enrollment enrollment, int numSessions) {
        LocalTime startTime = enrollment.getStartTime();
        LocalTime endTime   = startTime.plusMinutes(enrollment.getSessionDurationMinutes());

        // Fetch all holiday dates for this org to use as a skip set
        Set<LocalDate> holidays = holidayRepository
                .findByOrgIdOrderByHolidayDateAsc(enrollment.getOrgId())
                .stream()
                .map(h -> h.getHolidayDate())
                .collect(Collectors.toSet());

        Set<DayOfWeek> weeklyOffDays = organisationRepository.findById(enrollment.getOrgId())
                .map(Organisation::getWeeklyOffDays)
                .orElse(EnumSet.noneOf(DayOfWeek.class));

        List<TherapySession> sessions = new ArrayList<>(numSessions);
        LocalDate date = enrollment.getStartDate();
        for (int i = 0; i < numSessions; i++) {
            // Skip past any public holidays or the org's weekly off days
            while (holidays.contains(date) || weeklyOffDays.contains(date.getDayOfWeek())) {
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

        return sessionRepository.saveAll(sessions);
    }
}

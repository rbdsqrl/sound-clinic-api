package com.simplehearing.session.service;

import com.simplehearing.common.exception.ApiException;
import com.simplehearing.enrollment.entity.Enrollment;
import com.simplehearing.holiday.repository.PublicHolidayRepository;
import com.simplehearing.organisation.entity.Organisation;
import com.simplehearing.organisation.repository.OrganisationRepository;
import com.simplehearing.session.entity.TherapySession;
import com.simplehearing.session.repository.TherapySessionRepository;
import org.springframework.http.HttpStatus;
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
     * any dates that are public holidays, the org's weekly off days, or — when the
     * enrollment restricts itself to specific weekdays — not one of those days.
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

        // Empty means no restriction — every day is a candidate, same as before this field existed.
        Set<DayOfWeek> sessionDays = enrollment.getSessionDays();
        boolean restrictToSessionDays = sessionDays != null && !sessionDays.isEmpty();

        // A day the enrollment is restricted to that also happens to be an org-wide weekly
        // off day would never be reachable — the skip loop below would spin forever advancing
        // one day at a time, so fail fast instead of hanging the request.
        if (restrictToSessionDays && weeklyOffDays.containsAll(sessionDays)) {
            throw new ApiException(HttpStatus.CONFLICT,
                    "Every selected day is a weekly off day for this organisation — choose a different day");
        }

        List<TherapySession> sessions = new ArrayList<>(numSessions);
        LocalDate date = enrollment.getStartDate();
        for (int i = 0; i < numSessions; i++) {
            // Bounded to just over two years out — long enough that a real schedule never hits
            // it, but finite so a bad combination can't hang the request.
            date = nextValidDate(date, holidays, weeklyOffDays, sessionDays, restrictToSessionDays, date.plusDays(800));
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

    /**
     * Re-dates and re-times an already-fetched subset of a plan's still-SCHEDULED sessions
     * (sorted by session number — the caller's chosen "still ahead of the new effective date"
     * set), starting from {@code effectiveDate}, using the same placement rules
     * {@link #generateSessions} uses at creation. Session numbers and total plan length never
     * change — only which calendar dates the remaining sessions land on. The caller is expected
     * to have already applied any new start time / session days onto {@code enrollment} (not
     * yet saved) before calling this, since those are what get read here.
     */
    public List<TherapySession> rescheduleFutureSessions(
            Enrollment enrollment, LocalDate effectiveDate, List<TherapySession> sessionsToReschedule) {
        LocalTime startTime = enrollment.getStartTime();
        LocalTime endTime   = startTime.plusMinutes(enrollment.getSessionDurationMinutes());

        Set<LocalDate> holidays = holidayRepository
                .findByOrgIdOrderByHolidayDateAsc(enrollment.getOrgId())
                .stream()
                .map(h -> h.getHolidayDate())
                .collect(Collectors.toSet());

        Set<DayOfWeek> weeklyOffDays = organisationRepository.findById(enrollment.getOrgId())
                .map(Organisation::getWeeklyOffDays)
                .orElse(EnumSet.noneOf(DayOfWeek.class));

        Set<DayOfWeek> sessionDays = enrollment.getSessionDays();
        boolean restrictToSessionDays = sessionDays != null && !sessionDays.isEmpty();

        if (restrictToSessionDays && weeklyOffDays.containsAll(sessionDays)) {
            throw new ApiException(HttpStatus.CONFLICT,
                    "Every selected day is a weekly off day for this organisation — choose a different day");
        }

        LocalDate date = effectiveDate;
        for (TherapySession session : sessionsToReschedule) {
            date = nextValidDate(date, holidays, weeklyOffDays, sessionDays, restrictToSessionDays, date.plusDays(800));
            session.setSessionDate(date);
            session.setStartTime(startTime);
            session.setEndTime(endTime);
            date = date.plusDays(1);
        }

        return sessionRepository.saveAll(sessionsToReschedule);
    }

    /** Advances {@code date} past any public holiday, the org's weekly off days, or a weekday
     *  the plan isn't scheduled on, and returns the first date that clears all three. */
    private LocalDate nextValidDate(
            LocalDate date, Set<LocalDate> holidays, Set<DayOfWeek> weeklyOffDays,
            Set<DayOfWeek> sessionDays, boolean restrictToSessionDays, LocalDate searchLimit) {
        while (holidays.contains(date) || weeklyOffDays.contains(date.getDayOfWeek())
                || (restrictToSessionDays && !sessionDays.contains(date.getDayOfWeek()))) {
            date = date.plusDays(1);
            if (date.isAfter(searchLimit)) {
                throw new ApiException(HttpStatus.CONFLICT,
                        "Couldn't find enough valid session dates — check the selected days against holidays and weekly off days");
            }
        }
        return date;
    }
}

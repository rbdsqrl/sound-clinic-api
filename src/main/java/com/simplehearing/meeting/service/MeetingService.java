package com.simplehearing.meeting.service;

import com.simplehearing.common.exception.ApiException;
import com.simplehearing.common.dto.ParticipantResponse;
import com.simplehearing.holiday.repository.PublicHolidayRepository;
import com.simplehearing.meeting.dto.*;
import com.simplehearing.meeting.entity.Meeting;
import com.simplehearing.meeting.enums.MeetingStatus;
import com.simplehearing.meeting.repository.MeetingRepository;
import com.simplehearing.notification.CalendarInviteService;
import com.simplehearing.notification.EmailProperties;
import com.simplehearing.notification.EmailService;
import com.simplehearing.organisation.entity.Organisation;
import com.simplehearing.organisation.repository.OrganisationRepository;
import com.simplehearing.user.entity.User;
import com.simplehearing.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class MeetingService {

    private static final Logger log = LoggerFactory.getLogger(MeetingService.class);
    private static final DateTimeFormatter DATE_LABEL = DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy");
    private static final DateTimeFormatter TIME_LABEL = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter UNTIL_LABEL = DateTimeFormatter.ofPattern("d MMMM yyyy");
    /** Bounds how far a recurring series can run — long enough for any real weekly/biweekly
     *  cadence, short enough that a mistaken date range can't generate thousands of rows. */
    private static final int MAX_OCCURRENCES = 120;

    private final MeetingRepository meetingRepository;
    private final UserRepository userRepository;
    private final OrganisationRepository organisationRepository;
    private final PublicHolidayRepository holidayRepository;
    private final CalendarInviteService calendarInviteService;
    private final EmailService emailService;
    private final EmailProperties emailProperties;

    public MeetingService(MeetingRepository meetingRepository,
                          UserRepository userRepository,
                          OrganisationRepository organisationRepository,
                          PublicHolidayRepository holidayRepository,
                          CalendarInviteService calendarInviteService,
                          EmailService emailService,
                          EmailProperties emailProperties) {
        this.meetingRepository = meetingRepository;
        this.userRepository = userRepository;
        this.organisationRepository = organisationRepository;
        this.holidayRepository = holidayRepository;
        this.calendarInviteService = calendarInviteService;
        this.emailService = emailService;
        this.emailProperties = emailProperties;
    }

    @Transactional
    public MeetingResponse create(CreateMeetingRequest request, UUID orgId, UUID createdBy) {
        if (!request.endTime().isAfter(request.startTime())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "End time must be after the start time");
        }
        boolean recurring = Boolean.TRUE.equals(request.recurring());
        if (recurring) {
            if (request.recurrenceDays() == null || request.recurrenceDays().isEmpty()) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Select at least one day for the recurrence");
            }
            if (request.recurrenceEndDate() == null || !request.recurrenceEndDate().isAfter(request.meetingDate())) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Recurrence end date must be after the start date");
            }
        }

        // The organiser always attends; everyone else must belong to the same organisation.
        Set<UUID> ids = new LinkedHashSet<>(request.participantIds());
        ids.add(createdBy);

        List<User> participants = userRepository.findAllById(ids);
        if (participants.size() != ids.size()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "One or more participants could not be found");
        }
        boolean foreign = participants.stream().anyMatch(u -> !orgId.equals(u.getOrgId()));
        if (foreign) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Participants must belong to your organisation");
        }

        List<Meeting> occurrences = recurring
                ? buildRecurringOccurrences(request, orgId, createdBy, ids)
                : List.of(buildOccurrence(request, orgId, createdBy, ids, null, null, null));

        List<Meeting> saved = meetingRepository.saveAll(occurrences);
        Meeting first = saved.get(0);

        // One email per participant for the whole series, not one per occurrence — the ICS
        // carries an RRULE so the recipient's own calendar app expands the remaining dates.
        String recurrenceLabel = recurring ? recurrenceLabel(request) : null;
        sendInvites(first, participants, false, recurring ? buildRrule(request) : null, recurrenceLabel);

        return toResponse(first, participants);
    }

    private Meeting buildOccurrence(CreateMeetingRequest request, UUID orgId, UUID createdBy, Set<UUID> participantIds,
                                    LocalDate date, Integer occurrenceNumber, UUID seriesId) {
        Meeting meeting = new Meeting();
        meeting.setOrgId(orgId);
        meeting.setTitle(request.title().trim());
        meeting.setDescription(request.description());
        meeting.setMeetingDate(date != null ? date : request.meetingDate());
        meeting.setStartTime(request.startTime());
        meeting.setEndTime(request.endTime());
        meeting.setLocation(request.location());
        meeting.setCreatedBy(createdBy);
        meeting.setParticipantIds(participantIds);
        meeting.setSeriesId(seriesId);
        meeting.setOccurrenceNumber(occurrenceNumber);
        // ics_uid is set per occurrence (not shared) so each one is its own calendar entry —
        // only the single announcement email's ICS carries the RRULE.
        meeting.setIcsUid(UUID.randomUUID() + "@simplehearing");
        return meeting;
    }

    /** Walks every date in [meetingDate, recurrenceEndDate] whose weekday is in
     *  {@code recurrenceDays}, skipping public holidays, generating one {@link Meeting} row
     *  per match — mirrors {@code SessionGenerationService}'s day-selection logic. */
    private List<Meeting> buildRecurringOccurrences(CreateMeetingRequest request, UUID orgId, UUID createdBy,
                                                     Set<UUID> participantIds) {
        Set<LocalDate> holidays = holidayRepository.findByOrgIdOrderByHolidayDateAsc(orgId).stream()
                .map(h -> h.getHolidayDate())
                .collect(Collectors.toSet());
        // Defense in depth — the frontend's day-picker already disables weekly-off days, but a
        // stale client or a direct API call shouldn't be able to schedule on one anyway.
        Set<DayOfWeek> weeklyOffDays = organisationRepository.findById(orgId)
                .map(Organisation::getWeeklyOffDays)
                .orElse(EnumSet.noneOf(DayOfWeek.class));

        UUID seriesId = UUID.randomUUID();
        List<Meeting> occurrences = new ArrayList<>();
        LocalDate date = request.meetingDate();
        while (!date.isAfter(request.recurrenceEndDate())) {
            if (request.recurrenceDays().contains(date.getDayOfWeek())
                    && !holidays.contains(date) && !weeklyOffDays.contains(date.getDayOfWeek())) {
                occurrences.add(buildOccurrence(request, orgId, createdBy, participantIds,
                        date, occurrences.size() + 1, seriesId));
                if (occurrences.size() >= MAX_OCCURRENCES) break;
            }
            date = date.plusDays(1);
        }

        if (occurrences.isEmpty()) {
            throw new ApiException(HttpStatus.CONFLICT,
                    "No dates in that range match the selected days — check against public holidays");
        }
        int total = occurrences.size();
        occurrences.forEach(m -> m.setTotalOccurrences(total));
        return occurrences;
    }

    /** RFC 5545 BYDAY codes, in a stable order regardless of the caller's Set iteration order. */
    private String buildRrule(CreateMeetingRequest request) {
        String byDay = request.recurrenceDays().stream()
                .sorted()
                .map(this::icalDay)
                .collect(Collectors.joining(","));
        String until = request.recurrenceEndDate().atTime(23, 59, 59)
                .atZone(java.time.ZoneId.of("UTC"))
                .format(DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'"));
        return "FREQ=WEEKLY;BYDAY=" + byDay + ";UNTIL=" + until;
    }

    private String icalDay(DayOfWeek d) {
        return switch (d) {
            case MONDAY -> "MO"; case TUESDAY -> "TU"; case WEDNESDAY -> "WE";
            case THURSDAY -> "TH"; case FRIDAY -> "FR"; case SATURDAY -> "SA"; case SUNDAY -> "SU";
        };
    }

    private String recurrenceLabel(CreateMeetingRequest request) {
        String days = request.recurrenceDays().stream()
                .sorted()
                .map(d -> d.getDisplayName(java.time.format.TextStyle.SHORT, Locale.ENGLISH))
                .collect(Collectors.joining(", "));
        return "Repeats every " + days + " through " + request.recurrenceEndDate().format(UNTIL_LABEL);
    }

    @Transactional(readOnly = true)
    public List<MeetingResponse> list(UUID orgId, UUID userId, boolean seesEverything,
                                      LocalDate from, LocalDate to) {
        List<Meeting> meetings = seesEverything
                ? meetingRepository.findByOrgIdAndMeetingDateBetweenOrderByMeetingDateAscStartTimeAsc(orgId, from, to)
                : meetingRepository.findVisibleTo(orgId, userId, from, to);

        // One lookup for every participant across the window rather than per meeting.
        Set<UUID> allIds = meetings.stream()
                .flatMap(m -> m.getParticipantIds().stream())
                .collect(Collectors.toSet());
        Map<UUID, User> byId = userRepository.findAllById(allIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u));

        return meetings.stream()
                .map(m -> toResponse(m, m.getParticipantIds().stream()
                        .map(byId::get).filter(Objects::nonNull).toList()))
                .toList();
    }

    @Transactional(readOnly = true)
    public MeetingResponse get(UUID id, UUID orgId) {
        Meeting m = require(id, orgId);
        return toResponse(m, userRepository.findAllById(m.getParticipantIds()));
    }

    @Transactional
    public MeetingResponse cancel(UUID id, UUID orgId, UUID actorId, String reason) {
        Meeting m = require(id, orgId);
        if (m.getStatus() == MeetingStatus.CANCELLED) {
            throw new ApiException(HttpStatus.CONFLICT, "This meeting is already cancelled");
        }
        m.setStatus(MeetingStatus.CANCELLED);
        m.setCancelledReason(reason);
        m.setIcsSequence(m.getIcsSequence() + 1);
        Meeting saved = meetingRepository.save(m);

        List<User> participants = userRepository.findAllById(saved.getParticipantIds());
        sendCancellations(saved, participants, reason);
        return toResponse(saved, participants);
    }

    /** Post-meeting write-up for this one occurrence — a recurring series' other occurrences
     *  keep their own notes untouched. Restricted to the organiser, a participant, or an
     *  admin-tier role, so notes stay to people who were actually in the room. */
    @Transactional
    public MeetingResponse updateNotes(UUID id, UUID orgId, UUID actorId, boolean isAdminTier, String notes) {
        Meeting m = require(id, orgId);
        boolean allowed = isAdminTier || actorId.equals(m.getCreatedBy()) || m.getParticipantIds().contains(actorId);
        if (!allowed) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Only a participant can add notes to this meeting");
        }
        m.setNotes(notes);
        Meeting saved = meetingRepository.save(m);
        return toResponse(saved, userRepository.findAllById(saved.getParticipantIds()));
    }

    private Meeting require(UUID id, UUID orgId) {
        Meeting m = meetingRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Meeting not found"));
        if (!orgId.equals(m.getOrgId())) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Meeting not found");
        }
        return m;
    }

    private MeetingResponse toResponse(Meeting m, List<User> participants) {
        String organiserName = participants.stream()
                .filter(u -> u.getId().equals(m.getCreatedBy()))
                .findFirst()
                .map(u -> u.getFirstName() + " " + u.getLastName())
                .orElse(null);

        List<ParticipantResponse> people = participants.stream()
                .sorted(Comparator.comparing(User::getFirstName).thenComparing(User::getLastName))
                .map(u -> ParticipantResponse.from(u, u.getId().equals(m.getCreatedBy())))
                .toList();

        return MeetingResponse.from(m, organiserName, people);
    }

    // ── Notifications ───────────────────────────────────────────────────────────

    private void sendInvites(Meeting m, List<User> participants, boolean rescheduled) {
        sendInvites(m, participants, rescheduled, null, null);
    }

    /** @param rrule           set only for the single announcement email of a recurring series —
     *                         carried in the ICS so the recipient's calendar app expands it.
     *  @param recurrenceLabel human-readable version of the same schedule, shown in the email body. */
    private void sendInvites(Meeting m, List<User> participants, boolean rescheduled,
                             String rrule, String recurrenceLabel) {
        String orgName = orgName(m.getOrgId());
        String organiser = participants.stream()
                .filter(u -> u.getId().equals(m.getCreatedBy()))
                .findFirst().map(this::fullName).orElse(orgName);
        String names = participants.stream().map(this::fullName).collect(Collectors.joining(", "));

        String ics = calendarInviteService.build(
                m.getIcsUid(), m.getIcsSequence(), CalendarInviteService.Method.REQUEST,
                m.getTitle(),
                m.getDescription() != null ? m.getDescription() : "Meeting organised by " + organiser + ".",
                m.getLocation(), m.getMeetingDate(), m.getStartTime(), m.getEndTime(),
                orgName, emailProperties.getFromAddress(),
                participants.stream()
                        .map(u -> new CalendarInviteService.Attendee(fullName(u), u.getEmail()))
                        .toList(),
                rrule);

        String dateLabel = m.getMeetingDate().format(DATE_LABEL);
        String timeLabel = m.getStartTime().format(TIME_LABEL);

        for (User u : participants) {
            emailService.sendMeetingInvite(u.getEmail(), u.getFirstName(), m.getTitle(),
                    organiser, names, m.getLocation(), dateLabel, timeLabel, orgName,
                    "/calendar?meeting=" + m.getId(), ics, rescheduled, recurrenceLabel);
        }
        log.info("Meeting {} invites queued for {} participant(s)", m.getId(), participants.size());
    }

    private void sendCancellations(Meeting m, List<User> participants, String reason) {
        String orgName = orgName(m.getOrgId());
        String ics = calendarInviteService.build(
                m.getIcsUid(), m.getIcsSequence(), CalendarInviteService.Method.CANCEL,
                m.getTitle(), "This meeting has been cancelled.",
                m.getLocation(), m.getMeetingDate(), m.getStartTime(), m.getEndTime(),
                orgName, emailProperties.getFromAddress(),
                participants.stream()
                        .map(u -> new CalendarInviteService.Attendee(fullName(u), u.getEmail()))
                        .toList());

        String dateLabel = m.getMeetingDate().format(DATE_LABEL);
        for (User u : participants) {
            emailService.sendMeetingCancelled(u.getEmail(), u.getFirstName(), m.getTitle(),
                    dateLabel, orgName, reason, ics);
        }
    }

    private String fullName(User u) { return u.getFirstName() + " " + u.getLastName(); }

    private String orgName(UUID orgId) {
        return organisationRepository.findById(orgId)
                .map(o -> o.getName())
                .orElse("Simple Hearing");
    }
}

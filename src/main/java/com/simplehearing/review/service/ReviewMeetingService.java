package com.simplehearing.review.service;

import java.util.Comparator;
import com.simplehearing.auth.security.UserPrincipal;
import com.simplehearing.clinic.repository.ClinicRepository;
import com.simplehearing.common.exception.ApiException;
import com.simplehearing.enrollment.entity.Enrollment;
import com.simplehearing.holiday.repository.PublicHolidayRepository;
import com.simplehearing.notification.CalendarInviteService;
import com.simplehearing.notification.EmailProperties;
import com.simplehearing.notification.EmailService;
import com.simplehearing.organisation.entity.Organisation;
import com.simplehearing.organisation.repository.OrganisationRepository;
import com.simplehearing.patient.entity.Patient;
import com.simplehearing.patient.repository.PatientParentRepository;
import com.simplehearing.patient.repository.PatientRepository;
import com.simplehearing.review.dto.ReviewScheduleRequest;
import com.simplehearing.review.entity.ReviewMeeting;
import com.simplehearing.review.enums.ReviewMeetingStatus;
import com.simplehearing.review.repository.ReviewMeetingRepository;
import com.simplehearing.user.entity.User;
import com.simplehearing.user.enums.Role;
import com.simplehearing.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Generates review meetings for an enrollment and keeps everyone's calendar in step.
 */
@Service
public class ReviewMeetingService {

    private static final Logger log = LoggerFactory.getLogger(ReviewMeetingService.class);

    private static final DateTimeFormatter DATE_LABEL = DateTimeFormatter.ofPattern("EEE, d MMM yyyy");
    private static final DateTimeFormatter TIME_LABEL = DateTimeFormatter.ofPattern("h:mm a");

    private final ReviewMeetingRepository meetingRepository;
    private final PublicHolidayRepository holidayRepository;
    private final PatientRepository patientRepository;
    private final PatientParentRepository patientParentRepository;
    private final UserRepository userRepository;
    private final OrganisationRepository organisationRepository;
    private final ClinicRepository clinicRepository;
    private final EmailService emailService;
    private final EmailProperties emailProperties;
    private final CalendarInviteService calendarInviteService;

    public ReviewMeetingService(ReviewMeetingRepository meetingRepository,
                                PublicHolidayRepository holidayRepository,
                                PatientRepository patientRepository,
                                PatientParentRepository patientParentRepository,
                                UserRepository userRepository,
                                OrganisationRepository organisationRepository,
                                ClinicRepository clinicRepository,
                                EmailService emailService,
                                EmailProperties emailProperties,
                                CalendarInviteService calendarInviteService) {
        this.meetingRepository = meetingRepository;
        this.holidayRepository = holidayRepository;
        this.patientRepository = patientRepository;
        this.patientParentRepository = patientParentRepository;
        this.userRepository = userRepository;
        this.organisationRepository = organisationRepository;
        this.clinicRepository = clinicRepository;
        this.emailService = emailService;
        this.emailProperties = emailProperties;
        this.calendarInviteService = calendarInviteService;
    }

    // ── Generation ───────────────────────────────────────────────────────────

    /**
     * Creates the review meetings for a freshly-set-up therapy plan, spaced by the
     * requested interval and skipping public holidays the way therapy sessions do.
     *
     * @param clinicHeadIds the Clinic Head(s) chosen at scheduling time to sit in for the
     *                      therapist, who is deliberately not a participant under this model.
     * @return the meetings created, in date order
     */
    @Transactional
    public List<ReviewMeeting> generateForEnrollment(Enrollment enrollment,
                                                     ReviewScheduleRequest schedule,
                                                     Set<UUID> clinicHeadIds,
                                                     UUID createdBy) {

        int intervalWeeks = schedule.intervalWeeksOrDefault();
        int durationMinutes = schedule.durationMinutesOrDefault();

        LocalDate windowEnd = schedule.endDate() != null ? schedule.endDate() : enrollment.getEndDate();
        if (windowEnd == null) {
            log.warn("No end date for enrollment {} — cannot generate review meetings", enrollment.getId());
            return List.of();
        }

        LocalDate first = schedule.firstMeetingDate() != null
                ? schedule.firstMeetingDate()
                : enrollment.getStartDate().plusWeeks(intervalWeeks);

        if (first.isAfter(windowEnd)) {
            log.info("First review meeting {} falls past the end of the plan {} — none generated", first, windowEnd);
            return List.of();
        }

        Set<LocalDate> holidays = holidayRepository
                .findByOrgIdOrderByHolidayDateAsc(enrollment.getOrgId())
                .stream()
                .map(h -> h.getHolidayDate())
                .collect(Collectors.toSet());

        Set<DayOfWeek> weeklyOffDays = organisationRepository.findById(enrollment.getOrgId())
                .map(Organisation::getWeeklyOffDays)
                .orElse(EnumSet.noneOf(DayOfWeek.class));

        LocalTime startTime = schedule.startTime();
        LocalTime endTime = startTime.plusMinutes(durationMinutes);

        Set<UUID> participantIds = participantsFor(enrollment.getPatientId(), clinicHeadIds);

        List<ReviewMeeting> meetings = new ArrayList<>();
        LocalDate date = first;
        int number = 1;

        while (!date.isAfter(windowEnd)) {
            LocalDate slot = date;
            // Nudge past holidays and weekly off days, but never past the end of the plan.
            while ((holidays.contains(slot) || weeklyOffDays.contains(slot.getDayOfWeek())) && !slot.isAfter(windowEnd)) {
                slot = slot.plusDays(1);
            }
            if (slot.isAfter(windowEnd)) break;

            ReviewMeeting m = new ReviewMeeting();
            m.setOrgId(enrollment.getOrgId());
            m.setEnrollmentId(enrollment.getId());
            m.setPatientId(enrollment.getPatientId());
            m.setTherapistId(enrollment.getTherapistId());
            m.setMeetingNumber(number++);
            m.setMeetingDate(slot);
            m.setStartTime(startTime);
            m.setEndTime(endTime);
            m.setIcsUid(newIcsUid());
            m.setCreatedBy(createdBy);
            m.setParticipantIds(new LinkedHashSet<>(participantIds));
            meetings.add(m);

            date = date.plusWeeks(intervalWeeks);
        }

        List<ReviewMeeting> saved = meetingRepository.saveAll(meetings);
        log.info("Generated {} review meetings for enrollment {}", saved.size(), enrollment.getId());

        saved.forEach(m -> sendInvites(m, false));
        return saved;
    }

    /** Creates one extra meeting outside the generated rhythm. */
    @Transactional
    public ReviewMeeting createSingle(Enrollment enrollment, LocalDate date, LocalTime startTime,
                                      int durationMinutes, Set<UUID> clinicHeadIds, UUID createdBy) {
        ReviewMeeting m = new ReviewMeeting();
        m.setOrgId(enrollment.getOrgId());
        m.setEnrollmentId(enrollment.getId());
        m.setPatientId(enrollment.getPatientId());
        m.setTherapistId(enrollment.getTherapistId());
        m.setMeetingDate(date);
        m.setStartTime(startTime);
        m.setEndTime(startTime.plusMinutes(durationMinutes));
        m.setIcsUid(newIcsUid());
        m.setCreatedBy(createdBy);
        m.setParticipantIds(participantsFor(enrollment.getPatientId(), clinicHeadIds));

        ReviewMeeting saved = meetingRepository.save(m);

        // A meeting booked for an earlier date than existing ones would otherwise take the
        // highest number, so "Review 3" could fall before "Review 1". Number the whole plan
        // by date instead, which also corrects any sequence already out of order.
        renumberByDate(enrollment.getId());
        ReviewMeeting renumbered = meetingRepository.findById(saved.getId()).orElse(saved);

        sendInvites(renumbered, false);
        return renumbered;
    }

    /**
     * Rewrites meetingNumber across a plan in date order. Cancelled meetings keep their
     * place in the sequence so the numbers people have already been emailed still line up.
     */
    @Transactional
    public void renumberByDate(UUID enrollmentId) {
        List<ReviewMeeting> all = meetingRepository.findByEnrollmentIdOrderByMeetingNumberAsc(enrollmentId)
                .stream()
                .sorted(Comparator.comparing(ReviewMeeting::getMeetingDate)
                        .thenComparing(ReviewMeeting::getStartTime))
                .toList();

        int n = 1;
        for (ReviewMeeting m : all) {
            if (m.getMeetingNumber() != n) {
                m.setMeetingNumber(n);
                meetingRepository.save(m);
            }
            n++;
        }
    }

    // ── Calendar notifications ───────────────────────────────────────────────

    /** Emails the therapist and every linked parent an invite carrying an .ics attachment. */
    public void sendInvites(ReviewMeeting meeting, boolean rescheduled) {
        Context ctx = contextFor(meeting);
        if (ctx == null) return;

        String ics = calendarInviteService.build(
                meeting.getIcsUid(),
                meeting.getIcsSequence(),
                CalendarInviteService.Method.REQUEST,
                "Review meeting — " + ctx.patientName(),
                "Progress review for " + ctx.patientName() + " with " + ctx.therapistName() + ".",
                ctx.location(),
                meeting.getMeetingDate(),
                meeting.getStartTime(),
                meeting.getEndTime(),
                ctx.orgName(),
                emailProperties.getFromAddress(),
                ctx.attendees());

        String dateLabel = meeting.getMeetingDate().format(DATE_LABEL);
        String timeLabel = meeting.getStartTime().format(TIME_LABEL);
        String url = "/patients/" + meeting.getPatientId() + "?review=" + meeting.getId();

        for (Recipient r : ctx.recipients()) {
            emailService.sendReviewMeetingInvite(
                    r.email(), r.name(), ctx.patientName(), ctx.therapistName(),
                    dateLabel, timeLabel, ctx.orgName(), url, ics, rescheduled);
        }
    }

    /** Emails a CANCEL-method invite so the entry drops out of everyone's calendar. */
    public void sendCancellations(ReviewMeeting meeting, String reason) {
        Context ctx = contextFor(meeting);
        if (ctx == null) return;

        String ics = calendarInviteService.build(
                meeting.getIcsUid(),
                meeting.getIcsSequence(),
                CalendarInviteService.Method.CANCEL,
                "Review meeting — " + ctx.patientName(),
                "This review meeting has been cancelled.",
                ctx.location(),
                meeting.getMeetingDate(),
                meeting.getStartTime(),
                meeting.getEndTime(),
                ctx.orgName(),
                emailProperties.getFromAddress(),
                ctx.attendees());

        String dateLabel = meeting.getMeetingDate().format(DATE_LABEL);

        for (Recipient r : ctx.recipients()) {
            emailService.sendReviewMeetingCancelled(
                    r.email(), r.name(), ctx.patientName(), dateLabel, ctx.orgName(), reason, ics);
        }
    }

    /**
     * Notifies the other side that feedback has landed. Independent of who's invited to the
     * meeting itself (participantIds) — the therapist should still hear when parent feedback
     * arrives even though they're no longer a calendar participant under this model.
     */
    public void notifyFeedbackSubmitted(ReviewMeeting meeting, boolean fromParent) {
        Optional<Patient> patient = patientRepository.findById(meeting.getPatientId());
        if (patient.isEmpty()) {
            log.warn("Review meeting {} has no patient — skipping notification", meeting.getId());
            return;
        }
        String patientName = patient.get().getFirstName() + " " + patient.get().getLastName();
        String orgName = organisationRepository.findById(meeting.getOrgId())
                .map(Organisation::getName)
                .orElse("Simple Hearing");
        String dateLabel = meeting.getMeetingDate().format(DATE_LABEL);
        String url = "/patients/" + meeting.getPatientId() + "?review=" + meeting.getId();

        List<Recipient> targets = fromParent
                ? userRepository.findById(meeting.getTherapistId())
                        .filter(User::isActive)
                        .filter(u -> u.getEmail() != null && !u.getEmail().isBlank())
                        .map(u -> new Recipient(u.getFirstName() + " " + u.getLastName(), u.getEmail(), true))
                        .map(List::of)
                        .orElse(List.of())
                : parentRecipients(meeting.getPatientId());

        for (Recipient r : targets) {
            emailService.sendReviewFeedbackNotification(
                    r.email(), r.name(), patientName, dateLabel, orgName, url);
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    /** A UID that stays stable for this meeting across reschedules. */
    private static String newIcsUid() {
        return UUID.randomUUID() + "@simplehearing.in";
    }

    /**
     * Everyone and everything needed to build an invite. Recipients come from the meeting's
     * persisted {@code participantIds} — the patient's linked parents plus whichever Clinic
     * Head(s) were chosen. The assigned therapist is looked up only for the message text
     * (name/location); they are deliberately not a recipient under this model.
     * Null when the patient has vanished.
     */
    private Context contextFor(ReviewMeeting meeting) {
        Optional<Patient> patient = patientRepository.findById(meeting.getPatientId());
        if (patient.isEmpty()) {
            log.warn("Review meeting {} has no patient — skipping notification", meeting.getId());
            return null;
        }

        String patientName = patient.get().getFirstName() + " " + patient.get().getLastName();

        User therapist = userRepository.findById(meeting.getTherapistId()).orElse(null);
        String therapistName = therapist != null
                ? therapist.getFirstName() + " " + therapist.getLastName()
                : "the therapist";

        String orgName = organisationRepository.findById(meeting.getOrgId())
                .map(Organisation::getName)
                .orElse("Simple Hearing");

        String location = therapist != null && therapist.getClinicId() != null
                ? clinicRepository.findById(therapist.getClinicId()).map(c -> c.getName()).orElse("")
                : "";

        List<Recipient> recipients = userRepository.findAllById(meeting.getParticipantIds()).stream()
                .filter(User::isActive)
                .filter(u -> u.getEmail() != null && !u.getEmail().isBlank())
                .map(u -> new Recipient(u.getFirstName() + " " + u.getLastName(), u.getEmail(), false))
                .collect(Collectors.toCollection(ArrayList::new));

        List<CalendarInviteService.Attendee> attendees = recipients.stream()
                .map(r -> new CalendarInviteService.Attendee(r.name(), r.email()))
                .toList();

        return new Context(patientName, therapistName, orgName, location, recipients, attendees);
    }

    /** The patient's linked parents, active and with an email — used outside the participant model. */
    private List<Recipient> parentRecipients(UUID patientId) {
        List<UUID> parentIds = patientParentRepository.findById_PatientId(patientId)
                .stream()
                .map(pp -> pp.getId().getParentId())
                .toList();

        return userRepository.findAllById(parentIds).stream()
                .filter(User::isActive)
                .filter(u -> u.getEmail() != null && !u.getEmail().isBlank())
                .map(u -> new Recipient(u.getFirstName() + " " + u.getLastName(), u.getEmail(), false))
                .toList();
    }

    /** Validates the Clinic-Head picker used at scheduling time — required, and every id must
     *  resolve to an active, org-matching user holding the CLINIC_HEAD role. */
    public Set<UUID> requireClinicHeads(List<UUID> participantIds, UserPrincipal principal) {
        if (participantIds == null || participantIds.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Pick at least one Clinic Head to invite");
        }
        Set<UUID> ids = new LinkedHashSet<>(participantIds);
        List<User> users = userRepository.findAllById(ids);
        if (users.size() != ids.size()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "One or more selected Clinic Heads could not be found");
        }
        for (User u : users) {
            if (!principal.getOrgId().equals(u.getOrgId())) {
                throw new ApiException(HttpStatus.FORBIDDEN, "Participants must belong to your organisation");
            }
            if (!u.isActive() || !u.hasRole(Role.CLINIC_HEAD)) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Every participant must be an active Clinic Head");
            }
        }
        return ids;
    }

    /** Union of a patient's linked parents and the given Clinic Head(s). */
    private Set<UUID> participantsFor(UUID patientId, Set<UUID> clinicHeadIds) {
        Set<UUID> participants = new LinkedHashSet<>(clinicHeadIds);
        patientParentRepository.findById_PatientId(patientId)
                .forEach(pp -> participants.add(pp.getId().getParentId()));
        return participants;
    }

    /** Replaces the meeting's attendee list and resends invites to the new full set. */
    @Transactional
    public ReviewMeeting updateParticipants(ReviewMeeting meeting, Set<UUID> newParticipantIds) {
        meeting.setParticipantIds(new LinkedHashSet<>(newParticipantIds));
        meeting.setIcsSequence(meeting.getIcsSequence() + 1);
        ReviewMeeting saved = meetingRepository.save(meeting);
        sendInvites(saved, true);
        return saved;
    }

    private record Recipient(String name, String email, boolean isTherapist) {}

    private record Context(String patientName,
                           String therapistName,
                           String orgName,
                           String location,
                           List<Recipient> recipients,
                           List<CalendarInviteService.Attendee> attendees) {}

    // ── Status transitions used by the controller ────────────────────────────

    @Transactional
    public ReviewMeeting reschedule(ReviewMeeting meeting, LocalDate date, LocalTime startTime,
                                    Integer durationMinutes) {
        int minutes = durationMinutes != null
                ? durationMinutes
                : (int) java.time.Duration.between(meeting.getStartTime(), meeting.getEndTime()).toMinutes();

        meeting.setMeetingDate(date);
        meeting.setStartTime(startTime);
        meeting.setEndTime(startTime.plusMinutes(minutes));
        meeting.setStatus(ReviewMeetingStatus.SCHEDULED);
        meeting.setIcsSequence(meeting.getIcsSequence() + 1);   // makes clients update, not duplicate

        ReviewMeeting saved = meetingRepository.save(meeting);

        // Moving a meeting can change where it falls in the sequence.
        renumberByDate(saved.getEnrollmentId());
        ReviewMeeting renumbered = meetingRepository.findById(saved.getId()).orElse(saved);

        sendInvites(renumbered, true);
        return renumbered;
    }

    @Transactional
    public ReviewMeeting cancel(ReviewMeeting meeting, String reason) {
        meeting.setStatus(ReviewMeetingStatus.CANCELLED);
        meeting.setCancelledReason(reason);
        meeting.setIcsSequence(meeting.getIcsSequence() + 1);

        ReviewMeeting saved = meetingRepository.save(meeting);
        sendCancellations(saved, reason);
        return saved;
    }
}

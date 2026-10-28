package com.simplehearing.enrollment.service;

import com.simplehearing.enrollment.entity.Enrollment;
import com.simplehearing.enrollment.enums.EnrollmentCareStatus;
import com.simplehearing.enrollment.repository.EnrollmentRepository;
import com.simplehearing.notification.EmailService;
import com.simplehearing.organisation.entity.Organisation;
import com.simplehearing.organisation.repository.OrganisationRepository;
import com.simplehearing.patient.entity.Patient;
import com.simplehearing.patient.entity.PatientParent;
import com.simplehearing.patient.repository.PatientParentRepository;
import com.simplehearing.patient.repository.PatientRepository;
import com.simplehearing.program.entity.Program;
import com.simplehearing.program.repository.ProgramRepository;
import com.simplehearing.subscription.entity.Subscription;
import com.simplehearing.subscription.repository.SubscriptionRepository;
import com.simplehearing.user.entity.User;
import com.simplehearing.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Emails a child's parents a renewal reminder twice a day — a morning one ({@value #MORNING_HOUR}:00) and an
 * evening one ({@value #EVENING_HOUR}:00), in the organisation's own timezone — starting
 * {@value #LEAD_DAYS} days before the program's last session and continuing until it is renewed, or an admin
 * marks the program Completed/Discontinued. Only programs an admin has marked Needs Renewal (status Renewal Due)
 * get reminders — nothing switches a program into that state automatically.
 */
@Service
public class RenewalReminderService {

    private static final Logger log = LoggerFactory.getLogger(RenewalReminderService.class);

    public static final int LEAD_DAYS = 3;
    public static final int MORNING_HOUR = 9;
    public static final int EVENING_HOUR = 21;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("d MMM yyyy");

    private final EnrollmentRepository enrollmentRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final ProgramRepository programRepository;
    private final PatientRepository patientRepository;
    private final PatientParentRepository patientParentRepository;
    private final UserRepository userRepository;
    private final OrganisationRepository organisationRepository;
    private final EmailService emailService;

    public RenewalReminderService(EnrollmentRepository enrollmentRepository,
                                  SubscriptionRepository subscriptionRepository,
                                  ProgramRepository programRepository,
                                  PatientRepository patientRepository,
                                  PatientParentRepository patientParentRepository,
                                  UserRepository userRepository,
                                  OrganisationRepository organisationRepository,
                                  EmailService emailService) {
        this.enrollmentRepository = enrollmentRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.programRepository = programRepository;
        this.patientRepository = patientRepository;
        this.patientParentRepository = patientParentRepository;
        this.userRepository = userRepository;
        this.organisationRepository = organisationRepository;
        this.emailService = emailService;
    }

    /** Runs hourly; sends only when an organisation's local clock is in a reminder hour and that slot is unsent. */
    @Transactional
    public void sendDueReminders() {
        // Wide enough to cover every timezone's "today + 3 days"; each plan is re-checked on its org's own date below.
        List<Enrollment> candidates = enrollmentRepository.findDueForRenewalReminder(LocalDate.now().plusDays(LEAD_DAYS + 1));
        if (candidates.isEmpty()) return;

        Map<UUID, Organisation> orgs = new HashMap<>();
        int sent = 0;
        for (Enrollment e : candidates) {
            if (e.getCareStatus() == EnrollmentCareStatus.PROGRAM_COMPLETED) continue;   // therapy finished on purpose
            Organisation org = orgs.computeIfAbsent(e.getOrgId(), id -> organisationRepository.findById(id).orElse(null));
            if (org == null) continue;

            ZonedDateTime now = ZonedDateTime.now(zoneOf(org));
            String slotPart;
            if (now.getHour() == MORNING_HOUR) slotPart = "AM";
            else if (now.getHour() == EVENING_HOUR) slotPart = "PM";
            else continue;
            String slot = now.toLocalDate() + "-" + slotPart;

            long daysLeft = ChronoUnit.DAYS.between(now.toLocalDate(), e.getEndDate());
            if (daysLeft > LEAD_DAYS || slot.equals(e.getRenewalReminderLastSlot())) continue;

            if (sendOne(e, org, daysLeft)) sent++;
            e.setRenewalReminderLastSlot(slot);
        }
        enrollmentRepository.saveAll(candidates);
        if (sent > 0) log.info("Sent {} renewal reminder(s)", sent);
    }

    private boolean sendOne(Enrollment e, Organisation org, long daysLeft) {
        Patient patient = patientRepository.findById(e.getPatientId()).orElse(null);
        if (patient == null) return false;
        List<PatientParent> links = patientParentRepository.findById_PatientId(e.getPatientId());
        List<User> parents = userRepository.findAllById(links.stream().map(l -> l.getId().getParentId()).toList());
        if (parents.isEmpty()) return false;

        Subscription sub = subscriptionRepository.findById(e.getSubscriptionId()).orElse(null);
        Program program = sub == null ? null : programRepository.findById(sub.getProgramId()).orElse(null);
        String programName = program != null ? program.getName() : "therapy";

        String duePhrase = daysLeft > 1 ? "ends in " + daysLeft + " days"
                : daysLeft == 1 ? "ends tomorrow"
                : daysLeft == 0 ? "ends today"
                : "has ended";
        for (User parent : parents) {
            emailService.sendRenewalReminderEmail(parent.getEmail(), parent.getFirstName(), patient.getFirstName(),
                    programName, e.getEndDate().format(DATE_FMT), duePhrase, org.getName(), patient.getId().toString());
        }
        return true;
    }

    private static ZoneId zoneOf(Organisation org) {
        try {
            return ZoneId.of(org.getTimezone());
        } catch (DateTimeException | NullPointerException ex) {
            return ZoneId.of("UTC");
        }
    }
}

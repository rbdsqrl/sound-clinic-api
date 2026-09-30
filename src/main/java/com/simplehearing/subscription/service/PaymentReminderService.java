package com.simplehearing.subscription.service;

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

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

/** Emails a patient's linked parent(s) a reminder while a subscription's payment is still
 *  pending, every REMINDER_INTERVAL_DAYS — see PaymentReminderJob, the only caller. */
@Service
public class PaymentReminderService {

    private static final Logger log = LoggerFactory.getLogger(PaymentReminderService.class);
    public static final int REMINDER_INTERVAL_DAYS = 2;

    private final SubscriptionRepository subscriptionRepository;
    private final PatientRepository patientRepository;
    private final PatientParentRepository patientParentRepository;
    private final UserRepository userRepository;
    private final ProgramRepository programRepository;
    private final OrganisationRepository organisationRepository;
    private final EmailService emailService;

    public PaymentReminderService(SubscriptionRepository subscriptionRepository,
                                  PatientRepository patientRepository,
                                  PatientParentRepository patientParentRepository,
                                  UserRepository userRepository,
                                  ProgramRepository programRepository,
                                  OrganisationRepository organisationRepository,
                                  EmailService emailService) {
        this.subscriptionRepository = subscriptionRepository;
        this.patientRepository = patientRepository;
        this.patientParentRepository = patientParentRepository;
        this.userRepository = userRepository;
        this.programRepository = programRepository;
        this.organisationRepository = organisationRepository;
        this.emailService = emailService;
    }

    @Transactional
    public void sendDueReminders() {
        Instant cutoff = Instant.now().minus(REMINDER_INTERVAL_DAYS, ChronoUnit.DAYS);
        List<Subscription> due = subscriptionRepository.findDueForPaymentReminder(cutoff);
        if (due.isEmpty()) return;

        log.info("Sending {} payment reminder(s)", due.size());
        for (Subscription sub : due) {
            sendOneReminder(sub);
            sub.setPaymentReminderSentAt(Instant.now());
        }
        subscriptionRepository.saveAll(due);
    }

    private void sendOneReminder(Subscription sub) {
        Patient patient = patientRepository.findById(sub.getPatientId()).orElse(null);
        if (patient == null) return;

        List<PatientParent> links = patientParentRepository.findById_PatientId(sub.getPatientId());
        List<User> parents = userRepository.findAllById(links.stream().map(l -> l.getId().getParentId()).toList());
        if (parents.isEmpty()) return;

        Program program = programRepository.findById(sub.getProgramId()).orElse(null);
        String programName = program != null ? program.getName() : "Unknown Program";
        Organisation org = organisationRepository.findById(sub.getOrgId()).orElse(null);
        String orgName = org != null ? org.getName() : "";

        BigDecimal discount = sub.getDiscountPercent()
                .divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
        BigDecimal totalDue = sub.getPerSessionCost()
                .multiply(BigDecimal.valueOf(sub.getNumSessions()))
                .multiply(BigDecimal.ONE.subtract(discount))
                .setScale(0, RoundingMode.HALF_UP);
        BigDecimal remaining = totalDue.subtract(sub.getAmountPaid().setScale(0, RoundingMode.HALF_UP));

        for (User parent : parents) {
            emailService.sendPaymentReminderEmail(
                    parent.getEmail(), parent.getFirstName(), patient.getFirstName(),
                    programName, remaining, orgName, sub.getPatientId().toString());
        }
    }
}

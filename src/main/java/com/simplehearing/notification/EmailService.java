package com.simplehearing.notification;

import com.simplehearing.user.enums.Role;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;
    private final EmailProperties props;

    public EmailService(Optional<JavaMailSender> mailSender, EmailProperties props) {
        this.mailSender = mailSender.orElse(null);
        this.props = props;
        if (this.mailSender == null) {
            log.warn("No JavaMailSender configured — email sending is disabled");
        }
    }

    @Async
    public void sendInvitationEmail(String to, String acceptPath, Role role, String orgName) {
        String html = fillStubs(loadTemplate("invitation"), Map.of(
                "ORG_NAME", orgName,
                "ROLE", formatRole(role),
                "ACCEPT_LINK", props.getBaseUrl() + acceptPath,
                "EXPIRY_HOURS", "72"
        ));
        send(to, "You're invited to join " + orgName, html);
    }

    @Async
    public void sendWelcomeEmail(String to, String firstName, String orgName) {
        String html = fillStubs(loadTemplate("welcome"), Map.of(
                "FIRST_NAME", firstName,
                "ORG_NAME", orgName,
                "LOGIN_URL", props.getBaseUrl() + "/login"
        ));
        send(to, "Welcome to " + orgName, html);
    }

    @Async
    public void sendAppointmentReminderEmail(String to, String patientName, String therapistName,
                                              String date, String time, String clinicName) {
        String html = fillStubs(loadTemplate("appointment-reminder"), Map.of(
                "PATIENT_NAME", patientName,
                "THERAPIST_NAME", therapistName,
                "DATE", date,
                "TIME", time,
                "CLINIC_NAME", clinicName
        ));
        send(to, "Appointment confirmed — " + date, html);
    }

    @Async
    public void sendLeaveStatusEmail(String to, String therapistName, String leaveDate,
                                      String leaveType, String status, String reviewerName) {
        String statusClass = "APPROVED".equalsIgnoreCase(status) ? "approved" : "rejected";
        String html = fillStubs(loadTemplate("leave-status"), Map.of(
                "THERAPIST_NAME", therapistName,
                "LEAVE_DATE", leaveDate,
                "LEAVE_TYPE", formatLeaveType(leaveType),
                "STATUS", status,
                "STATUS_CLASS", statusClass,
                "REVIEWER_NAME", reviewerName
        ));
        send(to, "Leave request " + status.toLowerCase() + " — " + leaveDate, html);
    }

    private String loadTemplate(String name) {
        try {
            ClassPathResource resource = new ClassPathResource("templates/email/" + name + ".html");
            try (InputStream is = resource.getInputStream()) {
                return new String(is.readAllBytes(), StandardCharsets.UTF_8);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Email template not found: " + name, e);
        }
    }

    private String fillStubs(String template, Map<String, String> vars) {
        for (Map.Entry<String, String> entry : vars.entrySet()) {
            template = template.replace("{{" + entry.getKey() + "}}", entry.getValue());
        }
        return template;
    }

    private void send(String to, String subject, String htmlBody) {
        if (mailSender == null) {
            log.warn("Email skipped (no SMTP configured) — to={} subject={}", to, subject);
            return;
        }
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, "utf-8");
            helper.setFrom(props.getFromAddress(), props.getFromName());
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);
            mailSender.send(message);
            log.info("Email sent to {} — {}", to, subject);
        } catch (MessagingException | UnsupportedEncodingException e) {
            log.error("Failed to send email to {}: {}", to, e.getMessage());
        }
    }

    private String formatRole(Role role) {
        return switch (role) {
            case THERAPIST -> "Therapist";
            case DOCTOR -> "Doctor";
            case PARENT -> "Parent";
            case PATIENT -> "Patient";
            case ADMIN -> "Admin";
            case OFFICE_ADMIN -> "Office Admin";
            case BUSINESS_OWNER -> "Business Owner";
        };
    }

    private String formatLeaveType(String leaveType) {
        return switch (leaveType) {
            case "FULL_DAY" -> "Full Day";
            case "HALF_DAY" -> "Half Day";
            default -> leaveType;
        };
    }
}

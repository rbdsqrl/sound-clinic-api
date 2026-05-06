package com.simplehearing.invitation.service;

import com.simplehearing.auth.security.UserPrincipal;
import com.simplehearing.clinic.repository.ClinicRepository;
import com.simplehearing.common.exception.ApiException;
import com.simplehearing.invitation.dto.AcceptInviteRequest;
import com.simplehearing.invitation.dto.InviteRequest;
import com.simplehearing.invitation.dto.InviteResponse;
import com.simplehearing.invitation.entity.Invitation;
import com.simplehearing.invitation.repository.InvitationRepository;
import com.simplehearing.patient.entity.PatientParent;
import com.simplehearing.patient.repository.PatientParentRepository;
import com.simplehearing.user.entity.User;
import com.simplehearing.user.enums.Role;
import com.simplehearing.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class InvitationService {

    private static final Logger log = LoggerFactory.getLogger(InvitationService.class);

    /** Roles that require a clinicId in the invitation. */
    private static final Set<Role> CLINIC_SCOPED_ROLES = Set.of(Role.PARENT, Role.THERAPIST, Role.DOCTOR);

    /** All roles a BUSINESS_OWNER / ADMIN may invite. */
    private static final Set<Role> INVITABLE_ROLES = Set.of(
            Role.ADMIN, Role.OFFICE_ADMIN, Role.THERAPIST, Role.DOCTOR,
            Role.PARENT, Role.PATIENT, Role.BUSINESS_OWNER
    );

    private static final long EXPIRY_HOURS = 72;

    private final InvitationRepository invitationRepository;
    private final UserRepository userRepository;
    private final ClinicRepository clinicRepository;
    private final PasswordEncoder passwordEncoder;
    private final PatientParentRepository patientParentRepository;

    public InvitationService(InvitationRepository invitationRepository,
                             UserRepository userRepository,
                             ClinicRepository clinicRepository,
                             PasswordEncoder passwordEncoder,
                             PatientParentRepository patientParentRepository) {
        this.invitationRepository = invitationRepository;
        this.userRepository = userRepository;
        this.clinicRepository = clinicRepository;
        this.passwordEncoder = passwordEncoder;
        this.patientParentRepository = patientParentRepository;
    }

    /**
     * Creates an invitation.
     * - THERAPIST / PARENT: clinicId required and must belong to the caller's org.
     * - BUSINESS_OWNER: clinicId not required (org-level invite).
     */
    public InviteResponse invite(InviteRequest request, UserPrincipal caller) {
        if (!INVITABLE_ROLES.contains(request.role())) {
            throw new ApiException(HttpStatus.FORBIDDEN,
                    "Cannot invite users with role: " + request.role());
        }

        // Validate clinic requirement for clinic-scoped roles
        if (CLINIC_SCOPED_ROLES.contains(request.role())) {
            if (request.clinicId() == null) {
                throw new ApiException(HttpStatus.BAD_REQUEST,
                        "clinicId is required when inviting a " + request.role());
            }
            clinicRepository.findByIdAndOrgId(request.clinicId(), caller.getOrgId())
                    .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND,
                            "Clinic not found in your organisation"));
        }

        if (userRepository.existsByEmail(request.email())) {
            throw new ApiException(HttpStatus.CONFLICT, "A user with this email already exists");
        }

        if (invitationRepository.existsByEmailAndOrgIdAndStatus(
                request.email(), caller.getOrgId(), Invitation.Status.PENDING)) {
            throw new ApiException(HttpStatus.CONFLICT, "A pending invitation for this email already exists");
        }

        String rawToken = UUID.randomUUID() + "-" + UUID.randomUUID();

        Invitation invitation = new Invitation();
        invitation.setOrgId(caller.getOrgId());
        invitation.setClinicId(request.clinicId()); // null for BUSINESS_OWNER
        invitation.setInvitedBy(caller.getId());
        invitation.setEmail(request.email());
        invitation.setRole(request.role());
        invitation.setTokenHash(sha256(rawToken));
        invitation.setStatus(Invitation.Status.PENDING);
        invitation.setExpiresAt(Instant.now().plus(EXPIRY_HOURS, ChronoUnit.HOURS));
        invitationRepository.save(invitation);

        log.info("Invitation created for {} ({}) by {}", request.email(), request.role(), caller.getId());

        // No email service wired yet — return the accept link so the admin can share it manually.
        // When an email service is added, send the link here and omit acceptLink from the response.
        String acceptLink = "/accept-invite?token=" + rawToken;

        String clinicName = invitation.getClinicId() != null
                ? clinicRepository.findById(invitation.getClinicId())
                        .map(c -> c.getName()).orElse(null)
                : null;

        return InviteResponse.from(invitation, acceptLink, clinicName);
    }

    public List<InviteResponse> listForOrg(UserPrincipal caller) {
        List<Invitation> invitations = invitationRepository.findByOrgIdOrderByCreatedAtDesc(caller.getOrgId());

        // Bulk-load clinic names to avoid N+1
        Map<UUID, String> clinicNames = clinicRepository.findByOrgId(caller.getOrgId())
                .stream()
                .collect(Collectors.toMap(c -> c.getId(), c -> c.getName()));

        return invitations.stream()
                .map(inv -> InviteResponse.from(inv, null, clinicNames.get(inv.getClinicId())))
                .toList();
    }

    /**
     * Accepts an invitation: validates the token, creates the user account,
     * and marks the invitation as ACCEPTED.
     */
    public void accept(AcceptInviteRequest request) {
        String tokenHash = sha256(request.token());

        Invitation invitation = invitationRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Invalid or unknown invitation token"));

        if (invitation.getStatus() != Invitation.Status.PENDING) {
            throw new ApiException(HttpStatus.CONFLICT,
                    "This invitation has already been " + invitation.getStatus().name().toLowerCase());
        }

        if (invitation.getExpiresAt().isBefore(Instant.now())) {
            invitation.setStatus(Invitation.Status.EXPIRED);
            invitationRepository.save(invitation);
            throw new ApiException(HttpStatus.GONE, "This invitation has expired");
        }

        if (userRepository.existsByEmail(invitation.getEmail())) {
            throw new ApiException(HttpStatus.CONFLICT, "A user with this email already exists");
        }

        User user = new User();
        user.setOrgId(invitation.getOrgId());
        user.setClinicId(invitation.getClinicId()); // null for BUSINESS_OWNER — that's fine
        user.setEmail(invitation.getEmail());
        user.setFirstName(request.firstName());
        user.setLastName(request.lastName());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRole(invitation.getRole());
        user.setActive(true);
        userRepository.save(user);

        // Auto-link to patient if this invitation was created during inquiry conversion
        if (invitation.getPatientId() != null) {
            PatientParent link = new PatientParent(invitation.getPatientId(), user.getId());
            patientParentRepository.save(link);
            log.info("Auto-linked user {} to patient {}", user.getId(), invitation.getPatientId());
        }

        invitation.setStatus(Invitation.Status.ACCEPTED);
        invitationRepository.save(invitation);

        log.info("Invitation accepted — {} ({}) created", invitation.getEmail(), invitation.getRole());
    }

    /**
     * Creates an invitation linked to a patient — called during inquiry conversion.
     * When the invited user accepts, they are automatically linked to the patient.
     * Returns the raw accept link (e.g. "/accept-invite?token=...").
     */
    public String createLinkedInvitation(
            String email, Role role, UUID clinicId, UUID patientId,
            UUID orgId, UUID invitedBy) {

        Role effectiveRole = (role == Role.PATIENT || role == Role.PARENT) ? role : Role.PARENT;

        if (userRepository.existsByEmail(email)) {
            throw new ApiException(HttpStatus.CONFLICT, "A user with this email already exists");
        }
        if (invitationRepository.existsByEmailAndOrgIdAndStatus(email, orgId, Invitation.Status.PENDING)) {
            throw new ApiException(HttpStatus.CONFLICT, "A pending invitation for this email already exists");
        }

        String rawToken = UUID.randomUUID() + "-" + UUID.randomUUID();

        Invitation invitation = new Invitation();
        invitation.setOrgId(orgId);
        invitation.setClinicId(clinicId);
        invitation.setInvitedBy(invitedBy);
        invitation.setEmail(email);
        invitation.setRole(effectiveRole);
        invitation.setPatientId(patientId);
        invitation.setTokenHash(sha256(rawToken));
        invitation.setStatus(Invitation.Status.PENDING);
        invitation.setExpiresAt(Instant.now().plus(EXPIRY_HOURS, ChronoUnit.HOURS));
        invitationRepository.save(invitation);

        log.info("Linked invitation created for {} ({}) → patient {}", email, effectiveRole, patientId);
        return "/accept-invite?token=" + rawToken;
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                String h = Integer.toHexString(0xff & b);
                if (h.length() == 1) hex.append('0');
                hex.append(h);
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}

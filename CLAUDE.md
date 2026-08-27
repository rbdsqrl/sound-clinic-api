# Simple Hearing Backend — CLAUDE.md

Developer context for AI assistants working on this codebase.

---

## Tech Stack

| Layer          | Technology                          |
|----------------|-------------------------------------|
| Language       | Java 21                             |
| Framework      | Spring Boot 3.3.4                   |
| Build          | Maven                               |
| ORM            | Hibernate / Spring Data JPA         |
| Security       | Spring Security + JWT (jjwt 0.12.6) |
| DB (local)     | PostgreSQL (Docker, port 5432)      |
| DB (prod)      | PostgreSQL                          |
| Migrations     | Liquibase (YAML master + SQL files) |
| API Docs       | SpringDoc / Swagger UI              |
| JSON           | Jackson (camelCase, NON_NULL)       |
| PDF generation | OpenPDF 2.0.3 (LGPL/MPL, iText-4-compatible `com.lowagie.text` API) — used for discharge reports |

---

## Running Locally

```bash
cd backend
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

Server starts on **http://localhost:8080**.  
The `local` profile connects to PostgreSQL on `localhost:5432` (see the Docker command in
`application-local.yml`) — not H2, despite the leftover `~/.simplehearing/*.mv.db` file.  
Liquibase runs on startup and applies any new migrations only.

---

## Health Checks

| Method | URL                | Description                            |
|--------|--------------------|----------------------------------------|
| GET    | `/`                | Root ping — `{ "status": "running" }` |
| GET    | `/health`          | Manual health probe                    |
| GET    | `/actuator/health` | Spring Actuator (shows DB status)      |

---

## Package Structure

```
com.simplehearing
├── SimpleHearingApplication.java        # Entry point
│
├── config/
│   ├── JacksonConfig.java               # snake_case, NON_NULL, ISO-8601 dates
│   ├── OpenApiConfig.java               # Swagger/OpenAPI setup
│   └── SecurityConfig.java             # JWT filter chain, role-based access
│
├── common/
│   ├── dto/
│   │   ├── ApiResponse.java             # Universal wrapper: {success, message, data, timestamp}
│   │   └── PagedResponse.java           # Paginated list wrapper
│   ├── exception/
│   │   ├── ApiException.java            # RuntimeException with HttpStatus + message
│   │   ├── ConflictException.java       # 409 convenience subclass
│   │   ├── ResourceNotFoundException.java  # 404 convenience subclass
│   │   └── GlobalExceptionHandler.java  # @ControllerAdvice — maps exceptions → ApiResponse
│   └── tenant/
│       └── TenantContext.java           # ThreadLocal orgId holder (multi-tenancy helper)
│
├── auth/
│   ├── controller/AuthController.java   # POST /api/v1/auth/{login,register,refresh,logout}
│   ├── dto/                             # LoginRequest/Response, RegisterRequest, RefreshRequest/Response, LogoutRequest
│   ├── entity/RefreshToken.java         # Persisted refresh token
│   ├── repository/RefreshTokenRepository.java
│   ├── security/
│   │   ├── JwtAuthFilter.java           # OncePerRequestFilter — validates Bearer token
│   │   ├── JwtProperties.java           # jwt.secret, jwt.expiration-ms from application.yml
│   │   ├── TokenService.java            # JWT sign / verify / extract claims
│   │   └── UserPrincipal.java           # UserDetails wrapper around User entity
│   └── service/
│       ├── AuthService.java             # login, refresh, logout logic
│       └── RegistrationService.java     # register new org + owner
│
├── user/
│   ├── controller/UserController.java   # GET /api/v1/users/me, /users/therapists, /users/search
│   ├── dto/UserResponse.java
│   ├── entity/User.java                 # id, orgId, clinicId, email, passwordHash, role, additionalRoles...
│   ├── enums/
│   │   ├── Role.java                    # BUSINESS_OWNER, CLINIC_HEAD, THERAPIST, DOCTOR, PARENT, PATIENT
│   │   └── Gender.java                  # MALE, FEMALE, OTHER
│   └── repository/UserRepository.java
│
├── organisation/
│   ├── controller/OrganisationController.java  # GET/PATCH /api/v1/organisation
│   ├── dto/                             # OrganisationResponse, UpdateOrganisationRequest
│   ├── entity/Organisation.java
│   ├── repository/OrganisationRepository.java
│   └── service/OrganisationService.java
│
├── clinic/
│   ├── controller/ClinicController.java # CRUD /api/v1/clinics
│   ├── dto/                             # ClinicResponse, CreateClinicRequest
│   ├── entity/Clinic.java
│   ├── repository/ClinicRepository.java
│   └── service/ClinicService.java
│
├── patient/
│   ├── controller/PatientController.java  # CRUD /api/v1/patients + child-management routes
│   ├── dto/                               # PatientResponse, CreatePatientRequest, AddConditionRequest,
│   │                                      #   LinkParentRequest, AssignTherapistRequest
│   ├── entity/
│   │   ├── Patient.java
│   │   ├── PatientCondition.java          # Join: patient ↔ condition
│   │   ├── PatientParent.java             # Join: patient ↔ parent user
│   │   └── TherapistPatient.java          # Join: therapist ↔ patient
│   ├── repository/                        # PatientRepository, PatientConditionRepository,
│   │                                      #   PatientParentRepository, TherapistPatientRepository
│   └── service/PatientService.java
│
├── condition/
│   ├── controller/ConditionController.java  # GET /api/v1/conditions (shared lookup table)
│   ├── dto/ConditionResponse.java
│   ├── entity/Condition.java
│   └── repository/ConditionRepository.java
│
├── invitation/
│   ├── controller/InvitationController.java  # POST /api/v1/invitations, GET, POST /accept
│   ├── dto/                                  # InviteRequest, InviteResponse, AcceptInviteRequest
│   ├── entity/Invitation.java
│   ├── repository/InvitationRepository.java
│   └── service/InvitationService.java
│
├── appointment/
│   ├── controller/AppointmentController.java  # /api/v1/availability-slots, /api/v1/appointments
│   ├── dto/                                   # SlotResponse, CreateSlotRequest, AppointmentResponse,
│   │                                          #   BookAppointmentRequest, UpdateAppointmentStatusRequest
│   ├── entity/
│   │   ├── TherapistSlot.java                 # Recurring weekly availability slot
│   │   ├── Appointment.java
│   │   └── DayOfWeekConverter.java            # JPA AttributeConverter for DayOfWeek enum
│   ├── enums/AppointmentStatus.java           # PENDING, CONFIRMED, CANCELLED, COMPLETED
│   ├── repository/
│   │   ├── TherapistSlotRepository.java
│   │   └── AppointmentRepository.java
│   └── service/AppointmentService.java
│
├── leave/
│   ├── controller/LeaveController.java  # POST/GET /api/v1/leaves, PATCH /{id}/review, DELETE /{id}
│   ├── dto/
│   │   ├── LeaveResponse.java           # Record with therapist name + reviewer name enrichment
│   │   ├── CreateLeaveRequest.java      # leaveDate, leaveType, reason
│   │   └── ReviewLeaveRequest.java      # status: APPROVED | REJECTED
│   ├── entity/Leave.java                # id, orgId, therapistId, leaveDate, leaveType, status, reason,
│   │                                    #   reviewedBy, reviewedAt, createdAt, updatedAt
│   ├── enums/
│   │   ├── LeaveType.java               # FULL_DAY, HALF_DAY
│   │   └── LeaveStatus.java             # PENDING, APPROVED, REJECTED
│   └── repository/LeaveRepository.java  # findByOrgId*, findByOrgIdAndTherapistId*
│
├── analytics/
│   ├── controller/AnalyticsController.java  # /api/v1/analytics/* — admin roles only
│   ├── dto/
│   │   ├── TimeSeriesResponse.java      # Shared envelope: buckets + domains + totals
│   │   └── CaseloadResponse.java        # Therapist series + PatientRow list
│   ├── enums/Granularity.java           # DAILY | WEEKLY | MONTHLY + ISO bucketing rules
│   └── service/AnalyticsService.java    # Folds sessions + IEP progress into buckets
│
└── controller/
    └── HealthController.java            # GET /, GET /health (no auth required)
```

---

## REST API Summary

All protected routes require `Authorization: Bearer <access_token>`.  
All responses are wrapped: `{ "success": true, "data": ..., "timestamp": "..." }`.

| Method   | Path                                    | Roles allowed                                           | Description                         |
|----------|-----------------------------------------|---------------------------------------------------------|-------------------------------------|
| POST     | `/api/v1/auth/register`                 | Public                                                  | Register new org + business owner   |
| POST     | `/api/v1/auth/login`                    | Public                                                  | Login → access + refresh tokens     |
| POST     | `/api/v1/auth/refresh`                  | Public                                                  | Rotate refresh token                |
| POST     | `/api/v1/auth/logout`                   | Authenticated                                           | Invalidate refresh token            |
| GET      | `/api/v1/users/me`                      | Authenticated                                           | Caller's profile                    |
| GET      | `/api/v1/users/therapists`              | BUSINESS_OWNER, CLINIC_HEAD                                   | All therapists/doctors in org       |
| GET      | `/api/v1/analytics/patients/{id}/progress` | BUSINESS_OWNER, CLINIC_HEAD, PARENT (own child) | Mastery series + per-domain breakdown |
| GET      | `/api/v1/analytics/patients/{id}/activities` | BUSINESS_OWNER, CLINIC_HEAD, PARENT (own child) | Activity assignment/attempt progress |
| GET      | `/api/v1/analytics/patients/{id}/frequency` | BUSINESS_OWNER, CLINIC_HEAD, PARENT (own child) | Sessions/week across every concurrent enrollment |
| GET      | `/api/v1/analytics/enrollments/{id}/success-criteria` | All staff + PARENT (own child)             | Goal mastery / therapist sign-off / parent satisfaction composite |
| PATCH    | `/api/v1/enrollments/{id}/therapist-signoff` | THERAPIST/DOCTOR (own, once care status is Review or Program Completed) | Confirm this program's goals were met |
| GET      | `/api/v1/patients/{id}/discharge/preview` | BUSINESS_OWNER, CLINIC_HEAD, DOCTOR                              | Dry run of what discharging this patient now would look like |
| POST     | `/api/v1/patients/{id}/discharge`       | BUSINESS_OWNER, CLINIC_HEAD, DOCTOR                                | Discharge — closes every enrollment in the current episode, sets patient stage |
| GET      | `/api/v1/patients/{id}/discharge`       | All staff + PARENT (own child)                               | List discharge episodes, most recent first |
| GET      | `/api/v1/patients/{id}/discharge/{dischargeId}` | All staff + PARENT (own child)                       | One discharge episode's report |
| GET      | `/api/v1/patients/{id}/discharge/{dischargeId}/pdf` | All staff + PARENT (own child)                   | Discharge PDF — generated on first call, then a fresh short-lived URL each time |
| GET      | `/api/v1/analytics/therapists/{id}/caseload` | BUSINESS_OWNER, CLINIC_HEAD                | Therapist series + a row per patient |
| GET      | `/api/v1/analytics/overview`            | BUSINESS_OWNER, CLINIC_HEAD                     | Org rollup (WEEKLY/MONTHLY only)    |
| GET      | `/api/v1/users/assignable`              | BUSINESS_OWNER, CLINIC_HEAD, THERAPIST, DOCTOR  | Staff names + roles for assignee pickers |
| GET      | `/api/v1/review-meetings`               | All staff + PARENT (own children)                       | List review meetings                |
| POST     | `/api/v1/review-meetings`               | BUSINESS_OWNER, CLINIC_HEAD                     | Add one review meeting to a plan    |
| POST     | `/api/v1/review-meetings/schedule/{enrollmentId}` | BUSINESS_OWNER, CLINIC_HEAD           | Generate a recurring review schedule |
| PATCH    | `/api/v1/review-meetings/{id}/reschedule` | BUSINESS_OWNER, CLINIC_HEAD                   | Move a meeting; resends the invite  |
| PATCH    | `/api/v1/review-meetings/{id}/cancel`   | BUSINESS_OWNER, CLINIC_HEAD                     | Cancel; sends a CANCEL ics          |
| PATCH    | `/api/v1/review-meetings/{id}/complete` | All staff                                               | Mark a meeting completed            |
| PUT      | `/api/v1/review-meetings/{id}/parent-feedback`    | PARENT (linked to patient)                    | Rating + comments on the therapist  |
| PUT      | `/api/v1/review-meetings/{id}/therapist-feedback` | THERAPIST, DOCTOR, CLINIC_HEAD, BUSINESS_OWNER      | Summary + progress notes            |
| PATCH    | `/api/v1/enrollments/{id}/therapist`    | BUSINESS_OWNER, CLINIC_HEAD                     | Reassign an ongoing plan's therapist |
| PATCH    | `/api/v1/enrollments/{id}/care-status`  | THERAPIST (own), CLINIC_HEAD, BUSINESS_OWNER    | Set clinical-health signal; PROGRAM_COMPLETED also completes the enrollment |
| POST     | `/api/v1/enrollment-concerns`           | PARENT (own child)                                      | Raise a concern about an active program |
| GET      | `/api/v1/enrollment-concerns`           | All staff + PARENT (own child)                          | List concerns by `enrollmentId`, `patientId`, or org-wide `status` |
| GET      | `/api/v1/enrollment-concerns/open-count`| All staff                                                | Open-concern count (own caseload for THERAPIST/DOCTOR) |
| PATCH    | `/api/v1/enrollment-concerns/{id}/acknowledge` | All staff (own caseload for THERAPIST/DOCTOR)     | Acknowledge a concern |
| PATCH    | `/api/v1/enrollment-concerns/{id}/resolve` | All staff (own caseload for THERAPIST/DOCTOR)         | Resolve a concern |
| POST     | `/api/v1/therapy-sessions/ad-hoc`       | BUSINESS_OWNER, CLINIC_HEAD                     | Book a one-off session from the calendar |
| GET      | `/api/v1/therapy-sessions/{id}/feedback` | THERAPIST, DOCTOR, CLINIC_HEAD, BUSINESS_OWNER | Session feedback checklist template (per the session's program) + this session's answers |
| PUT      | `/api/v1/therapy-sessions/{id}/feedback` | THERAPIST, DOCTOR, CLINIC_HEAD, BUSINESS_OWNER | Save this session's feedback checklist answers |
| GET      | `/api/v1/programs/{id}/feedback-template` | BUSINESS_OWNER, CLINIC_HEAD                    | Get a program's session feedback checklist template |
| PUT      | `/api/v1/programs/{id}/feedback-template` | BUSINESS_OWNER, CLINIC_HEAD                    | Replace a program's session feedback checklist template |
| GET      | `/api/v1/patients/{patientId}/assessments/{type}/definition` | All staff + PARENT (own child) | Fixed ISAA/PRBA item/section definition |
| GET      | `/api/v1/patients/{patientId}/assessments/{type}` | All staff + PARENT (own child)             | List a patient's ISAA/PRBA fills, oldest first |
| POST     | `/api/v1/patients/{patientId}/assessments/{type}` | BUSINESS_OWNER, CLINIC_HEAD, THERAPIST, DOCTOR | Record a new ISAA/PRBA fill — score + classification computed server-side |
| POST     | `/api/v1/meetings`                      | All staff (not PARENT/PATIENT)                          | Schedule a meeting + email invites  |
| GET      | `/api/v1/meetings`                      | Authenticated                                           | Meetings in a date range (scoped)   |
| GET      | `/api/v1/meetings/{id}`                 | Authenticated                                           | One meeting with participants       |
| PATCH    | `/api/v1/meetings/{id}/cancel`          | All staff (not PARENT/PATIENT)                          | Cancel + send CANCEL ics            |
| GET      | `/api/v1/users/search`                  | BUSINESS_OWNER, CLINIC_HEAD                                   | Search users by email               |
| GET      | `/api/v1/organisation`                  | BUSINESS_OWNER, CLINIC_HEAD                                   | Org profile                         |
| PATCH    | `/api/v1/organisation`                  | BUSINESS_OWNER, CLINIC_HEAD                                   | Update org profile                  |
| GET      | `/api/v1/clinics`                       | All authenticated                                       | List clinics in org                 |
| POST     | `/api/v1/clinics`                       | BUSINESS_OWNER, CLINIC_HEAD                                   | Create clinic                       |
| GET      | `/api/v1/clinics/{id}`                  | All authenticated                                       | Clinic detail                       |
| PATCH    | `/api/v1/clinics/{id}`                  | BUSINESS_OWNER, CLINIC_HEAD                                   | Update clinic                       |
| GET      | `/api/v1/patients`                      | BUSINESS_OWNER, CLINIC_HEAD, THERAPIST, DOCTOR                | List patients                       |
| POST     | `/api/v1/patients`                      | BUSINESS_OWNER, CLINIC_HEAD, THERAPIST                        | Create patient                      |
| GET      | `/api/v1/patients/{id}`                 | BUSINESS_OWNER, CLINIC_HEAD, THERAPIST, DOCTOR                | Patient detail                      |
| POST     | `/api/v1/patients/{id}/conditions`      | BUSINESS_OWNER, CLINIC_HEAD, THERAPIST                        | Add condition to patient            |
| POST     | `/api/v1/patients/{id}/parents`         | BUSINESS_OWNER, CLINIC_HEAD                                   | Link parent to patient              |
| POST     | `/api/v1/patients/{id}/therapists`      | BUSINESS_OWNER, CLINIC_HEAD                                   | Assign therapist to patient         |
| GET      | `/api/v1/conditions`                    | All authenticated                                       | List all conditions (lookup)        |
| POST     | `/api/v1/invitations`                   | BUSINESS_OWNER, CLINIC_HEAD                                   | Invite user by email + role         |
| GET      | `/api/v1/invitations`                   | BUSINESS_OWNER, CLINIC_HEAD                                   | List sent invitations               |
| POST     | `/api/v1/inquiries/manual`              | BUSINESS_OWNER, CLINIC_HEAD                     | Record a walk-in / phoned-in inquiry |
| PATCH    | `/api/v1/invitations/{id}/cancel`       | BUSINESS_OWNER, CLINIC_HEAD                                   | Withdraw an unaccepted invitation   |
| POST     | `/api/v1/invitations/accept`            | Public                                                  | Accept invite → create account      |
| GET      | `/api/v1/availability-slots`            | All authenticated                                       | List availability slots             |
| POST     | `/api/v1/availability-slots`            | BUSINESS_OWNER, CLINIC_HEAD                                   | Create availability slot            |
| DELETE   | `/api/v1/availability-slots/{id}`       | BUSINESS_OWNER, CLINIC_HEAD                                   | Delete availability slot            |
| GET      | `/api/v1/appointments`                  | All authenticated                                       | List appointments (role-scoped)     |
| POST     | `/api/v1/appointments`                  | PARENT, BUSINESS_OWNER, CLINIC_HEAD                           | Book appointment                    |
| PATCH    | `/api/v1/appointments/{id}/status`      | All authenticated                                       | Update appointment status           |
| POST     | `/api/v1/leaves`                        | THERAPIST, DOCTOR                                       | Apply for leave                     |
| GET      | `/api/v1/leaves`                        | BUSINESS_OWNER/CLINIC_HEAD (all), THERAPIST/DOCTOR (own only) | List leave requests; optional `?status=PENDING\|APPROVED\|REJECTED` |
| PATCH    | `/api/v1/leaves/{id}/review`            | BUSINESS_OWNER, CLINIC_HEAD                                   | Approve or reject a leave request   |
| DELETE   | `/api/v1/leaves/{id}`                   | THERAPIST, DOCTOR                                       | Cancel own pending leave            |

---

## Database Migrations

Located in `src/main/resources/db/changelog/`.  
Master file: `db.changelog-master.yaml` — lists migrations in order.

| File                               | Description                                                       |
|------------------------------------|-------------------------------------------------------------------|
| 001-create-clinics.sql             | `clinics` table                                                   |
| 002-create-users.sql               | `users` table                                                     |
| 003-create-refresh-tokens.sql      | `refresh_tokens` table                                            |
| 004-create-invitations.sql         | `invitations` table                                               |
| 005-create-organisations.sql       | `organisations` table                                             |
| 006-alter-clinics-add-org.sql      | Add `org_id` FK to clinics                                        |
| 007-alter-users-add-org.sql        | Add `org_id` FK to users                                          |
| 008-create-patients.sql            | `patients` table                                                  |
| 009-create-conditions.sql          | `conditions` lookup table                                         |
| 010-create-patient-conditions.sql  | `patient_conditions` join table                                   |
| 011-create-patient-parents.sql     | `patient_parents` join table                                      |
| 012-create-therapist-patients.sql  | `therapist_patients` join table                                   |
| 013-create-user-roles.sql          | `user_roles` join table (extra roles)                             |
| 014-create-therapist-slots.sql     | `therapist_slots` table                                           |
| 015-create-appointments.sql        | `appointments` table                                              |
| 016-create-leaves.sql              | `leaves` table (org_id, therapist_id, leave_date, leave_type, status, reason, reviewed_by, reviewed_at) |
| 043-create-password-reset-tokens.sql | `password_reset_tokens` table (hashed single-use tokens) |
| 044-create-review-meetings.sql     | `review_meetings` table + `enrollments.end_date`                  |
| 045-analytics-indexes-and-score-scale.sql | 1-5 CHECK on `therapy_sessions.performance_score` + date-range indexes |
| 046-normalise-emails.sql            | Lower-cases existing user/invitation emails; unique index on `lower(email)` |
| 047-create-meetings.sql             | `meetings` + `meeting_participants` tables (general meetings with attendees) |
| 048-performance-score-percentage.sql | `performance_score` moves from a 1-5 rubric to 0-100; existing scores cleared |
| 049-inquiry-source.sql              | `inquiries.source` — WEBSITE / WALK_IN / PHONE, existing rows backfilled to WEBSITE |
| 050-parent-reschedule-limit.sql     | `therapy_sessions.parent_reschedule_requested` — durable flag backing the per-plan parent allowance |
| 051-adhoc-sessions.sql              | `therapy_sessions.ad_hoc` + `counts_toward_plan` for sessions booked from the calendar |
| 052-reschedule-history.sql          | `therapy_sessions.reschedule_count` — durable count of moves, for reschedule analytics |
| 053-adhoc-payment.sql               | `therapy_sessions.requires_payment` — whether an extra session is chargeable |
| 054-create-activities.sql           | `activities` table (reusable therapy activity library)            |
| 055-create-activity-lookups.sql     | `skills`, `languages`, `props` lookups + activity join tables      |
| 056-create-activity-instructions-checklist.sql | `activity_instructions`, `activity_checklist_questions/options` |
| 057-create-activity-resources.sql   | `activity_resources`, `activity_links`                             |
| 058-create-activity-assignments.sql | `activity_assignments`, `activity_attempt_logs/answers`            |
| 059-org-ai-settings.sql             | Org-level Anthropic API key/model for Activity "Magic Fill"        |
| 060-activities-use-programs.sql     | `activities.therapy_id` → `program_id` (draws from Programs, not a separate Therapies lookup) |
| 061-enrollment-care-status.sql      | `enrollments.care_status` (+ note/updated-by/at) — clinical-health signal, separate from `status` and from patient `stage` |
| 062-create-enrollment-concerns.sql  | `enrollment_concerns` table — parent-raised concerns on an active enrollment |
| 063-review-meeting-rating-axes.sql  | `review_meetings.communication_rating` (1-5, backfilled from `parent_rating`) + `progress_rating_pct` (0-100, new); `parent_rating` kept but deprecated |
| —                                    | (no migration) `GET /analytics/patients/{id}/frequency` — session cadence folded across a patient's concurrent enrollments |
| 064-iep-plan-enrollment-link.sql    | `iep_plans.enrollment_id` (nullable) — lets goal mastery attribute to the right program |
| 065-enrollment-therapist-signoff.sql | `enrollments.therapist_signed_off` (+ by/at/notes) — one of the three discharge success criteria |
| 066-success-criteria-settings.sql   | `organisations.goal_mastery_threshold_pct` (90), `parent_satisfaction_threshold_pct` (70), `require_all_enrollments_for_discharge` (true) |
| 067-create-discharge-records.sql    | `discharge_records` table — one row per discharge episode, frozen snapshots + success-criteria composite |
| 068-enrollments-discharge-link.sql  | `enrollments.discharged_in_record_id` — the episode-of-care boundary (NULL = still open) |
| 077-program-feedback-checklist.sql  | `program_feedback_questions`/`options` (per-program checklist template) + `session_feedback_answers`/`answer_options` (per-session fill) + `therapy_sessions.checklist_notes` |
| 078-create-patient-assessments.sql  | `patient_assessments` — repeated ISAA/PRBA clinical assessment fills per patient, item scores as JSON, server-computed total + classification |

**To add a migration:** create `NNN-description.sql` with the Liquibase header, then add it to the master YAML.

SQL file template:
```sql
--liquibase formatted sql

--changeset simplehearing:NNN-description
CREATE TABLE ... ;

--rollback DROP TABLE ...;
```

---

## Coding Conventions

### Entities
- UUID primary key with `@GeneratedValue(strategy = GenerationType.UUID)`
- Always include `orgId` for multi-tenancy
- `@CreationTimestamp` / `@UpdateTimestamp` for audit fields
- Enums stored as `VARCHAR` via `@Enumerated(EnumType.STRING)`
- Plain getters/setters (no Lombok — project does not use it)

### DTOs
- Use Java **records** for response DTOs
- Include a static `from(Entity, ...)` factory method
- Enrich with human-readable names (therapist name, clinic name) at the controller/service layer

### Controllers
- `@RestController @RequestMapping("/api/v1")`
- Role guards via `@PreAuthorize("hasAnyRole('...')")`
- Extract caller context via `@AuthenticationPrincipal UserPrincipal principal`
- Return `ResponseEntity<ApiResponse<T>>`
- 201 for creates, 200 for reads/updates, 204 for deletes

### Exception Handling
- Throw `ApiException(HttpStatus.XXX, "message")` for business errors
- `ResourceNotFoundException` for 404s
- `ConflictException` for 409s
- `GlobalExceptionHandler` maps them all to `ApiResponse`

### Analytics
- Reschedules are counted from `reschedule_count`, not from the `PENDING_RESCHEDULE` status — status is cleared the moment the clinic actions a request
- Mastery is **ratio of sums** (Σ`trials_passed` ÷ Σ`trials_total`), never an average of per-session ratios
- A period with no data serialises `masteryPct` as null — never 0, which would read as a regression
- Bucket on `LocalDate` fields (`session_date`, `meeting_date`), never on `created_at` (`Instant`)
- Always return coverage alongside a trend; a series built on thin coverage is a sampling artefact
- A parent may reschedule at most `PARENT_RESCHEDULE_LIMIT` (3) sessions per enrollment; the count comes from `parent_reschedule_requested`, which is never cleared
- `performance_score` is a 0-100 percentage (see `UpdateSessionNotesRequest`), read through named bands in the UI — keep it bounded

### Multi-Tenancy
- Every query must filter by `orgId` from `principal.getOrgId()`
- Never expose data across organisations

---

## Adding a New Feature Module

1. Create package `com.simplehearing.<feature>/`
2. Add sub-packages: `entity/`, `dto/`, `repository/`, `service/`, `controller/`, `enums/` (if needed)
3. Write a Liquibase migration SQL file and register it in the master YAML
4. Entity → Repository → Service → Controller → DTO
5. Update this file's API table and migration table

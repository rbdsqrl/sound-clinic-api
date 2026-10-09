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
| GET    | `/health/db`       | Runs `SELECT 1` against the DB — no auth, meant for an external pinger (cron/UptimeRobot) to stop the free-tier DB from auto-suspending on idle |
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
│   ├── CacheConfig.java                 # Caffeine cache for org-level analytics — 60s TTL, ~3MB/cache weight cap, soft values (sized for Render's 512MB)
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
│   │   ├── Role.java                    # BUSINESS_OWNER, CLINIC_HEAD, THERAPIST, PARENT, PATIENT
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
├── memberdocument/
│   ├── controller/MemberDocumentController.java  # /api/v1/users/{userId}/documents — list/upload/delete; Business Owner + Clinic Head only (HR-sensitive)
│   ├── dto/MemberDocumentResponse.java
│   ├── entity/MemberDocument.java                # category, title, file name/url/type/size, notes, uploader
│   ├── enums/MemberDocumentCategory.java         # IDENTITY_PROOF, QUALIFICATION, CERTIFICATION, OFFER_LETTER, EMPLOYMENT_CONTRACT, JOINING_DETAILS, INDUCTION, OTHER
│   └── repository/MemberDocumentRepository.java
│
├── evidence/
│   ├── controller/EvidenceController.java     # /api/v1/patients/{id}/evidence + /organisation/evidence-settings
│   ├── dto/                                   # EvidenceResponse, CannotUploadRequest, EvidenceSettings, EvidenceAnalyticsResponse
│   ├── entity/GoalEvidence.java               # a VIDEO file or a CANNOT_UPLOAD reason, linked to child + optional goal/session/therapy
│   ├── enums/                                 # EvidenceKind, EvidenceReason
│   ├── repository/GoalEvidenceRepository.java
│   └── service/                               # EvidenceService (settings + the goal-completion rule), EvidenceAnalyticsService
│
├── sharedmedia/
│   ├── controller/SharedMediaController.java  # /api/v1/patients/{patientId}/shared-media — list/upload/delete
│   ├── dto/SharedMediaResponse.java            # id, direction, fileUrl (presigned), note, uploader name/role, createdAt
│   ├── entity/SharedMedia.java                 # video optional, note optional — at least one required (CHECK constraint)
│   ├── enums/SharedMediaDirection.java         # PARENT_TO_CLINIC, CLINIC_TO_PARENT
│   └── repository/SharedMediaRepository.java
│
├── reassignment/
│   ├── controller/TherapistReassignmentController.java  # POST/GET /api/v1/therapist-reassignments, PATCH /{id}/cancel — Admin Roles only
│   ├── dto/                             # CreateReassignmentRequest, ReassignmentResponse, ReassignmentCaseSummary
│   ├── entity/                          # TherapistReassignment (batch header), TherapistReassignmentCase (one row per patient)
│   ├── enums/                           # ReassignmentType (PERMANENT/TEMPORARY), ReassignmentStatus (ACTIVE/REVERTED/CANCELLED)
│   ├── job/ReassignmentRevertJob.java   # @Scheduled nightly — hands back expired TEMPORARY batches
│   ├── repository/                      # TherapistReassignmentRepository, TherapistReassignmentCaseRepository
│   └── service/TherapistReassignmentService.java  # Bulk-moves sessions/review meetings/IEP plans + caseload links; revert() shared by the job and early-cancel
│
└── controller/
    └── HealthController.java            # GET /, GET /health, GET /health/db (no auth required)
```

---

## REST API Summary

All protected routes require `Authorization: Bearer <access_token>`.  
All responses are wrapped: `{ "success": true, "data": ..., "timestamp": "..." }`.

| Method   | Path                                    | Roles allowed                                           | Description                         |
|----------|-----------------------------------------|---------------------------------------------------------|-------------------------------------|
| POST     | `/api/v1/auth/register`                 | Public                                                  | Register new org + business owner   |
| POST     | `/api/v1/auth/login`                    | Public                                                  | Login with `identifier` (email or phone number, auto-detected) + password → access + refresh tokens |
| POST     | `/api/v1/auth/refresh`                  | Public                                                  | Rotate refresh token                |
| POST     | `/api/v1/auth/logout`                   | Authenticated                                           | Invalidate refresh token            |
| GET      | `/api/v1/users/me`                      | Authenticated                                           | Caller's profile                    |
| GET      | `/api/v1/users/therapists`              | BUSINESS_OWNER, CLINIC_HEAD                                   | All therapists in org       |
| GET      | `/api/v1/users/members`                 | BUSINESS_OWNER, CLINIC_HEAD, OFFICE_ADMIN                     | Paginated staff list — 20/page, sorted `createdAt` desc by default; `search`, `role`, `clinicId`, `active` (default true) filters |
| GET      | `/api/v1/users/{id}/profile`            | BUSINESS_OWNER, CLINIC_HEAD                                   | One member's profile — contact, qualification, specialization, languages, case count |
| PATCH    | `/api/v1/users/{id}/profile`            | BUSINESS_OWNER, CLINIC_HEAD (role change: BUSINESS_OWNER only) | Update a member's phone/clinic/qualification/specialization/languages, and their role (staff roles only, not own role) — phone is normalised and rejected with 409 if another user already has it |
| GET      | `/api/v1/users/{id}/documents`          | BUSINESS_OWNER, CLINIC_HEAD                     | List the documents on a staff member's record (ID proof, qualifications, contracts…) with short-lived download links |
| POST     | `/api/v1/users/{id}/documents`          | BUSINESS_OWNER, CLINIC_HEAD                     | Add a document — multipart `file` + `category` (+ optional `title`, `notes`); PDF/Office/text/image only, 25 MB max |
| DELETE   | `/api/v1/users/{id}/documents/{documentId}` | BUSINESS_OWNER, CLINIC_HEAD                 | Remove a document and its stored file |
| GET      | `/api/v1/organisation/evidence-settings` | All staff                                      | The org's goal video-evidence rules — videos required per goal (0 = optional), max video MB, max video seconds |
| PUT      | `/api/v1/organisation/evidence-settings` | BUSINESS_OWNER, CLINIC_HEAD, OFFICE_ADMIN      | Update those rules |
| GET      | `/api/v1/patients/{id}/evidence`        | BUSINESS_OWNER, CLINIC_HEAD, THERAPIST (assigned), PARENT (own child) | A child's video evidence, newest first (`?goalId=` to filter); parents see videos only, never the "couldn't upload" records |
| POST     | `/api/v1/patients/{id}/evidence`        | BUSINESS_OWNER, CLINIC_HEAD, THERAPIST (assigned) | Upload a video (multipart `file`, optional `goalId`, `sessionId`, `note`, `durationSeconds`) — checked against the org's size/length limits; goal and session are optional so evidence can be ad hoc |
| POST     | `/api/v1/patients/{id}/evidence/upload-url` | BUSINESS_OWNER, CLINIC_HEAD, THERAPIST (assigned) | Direct-to-storage upload, step 1 — checks the video against the org's rules and returns a short-lived signed PUT link (size and type are part of the signature); the file never passes through this server |
| POST     | `/api/v1/patients/{id}/evidence/uploads/{uploadId}/complete` | same as above                        | Step 2 — verifies the stored object exists and matches the approved size, then registers it as evidence (409 if it hasn't finished arriving, so the client can retry) |
| POST     | `/api/v1/patients/{id}/evidence/cannot-upload` | BUSINESS_OWNER, CLINIC_HEAD, THERAPIST (assigned) | Record why a video couldn't be uploaded (categorised reason; `OTHER` needs text) — lets the goal be completed without one, and shows in analytics |
| DELETE   | `/api/v1/patients/{id}/evidence/{evidenceId}` | Recorder, or BUSINESS_OWNER/CLINIC_HEAD  | Delete evidence and its stored video |
| GET      | `/api/v1/analytics/evidence`            | BUSINESS_OWNER, CLINIC_HEAD, OFFICE_ADMIN       | Goals completed per therapist for a window — with video / couldn't upload / no evidence, compliance %, videos uploaded, and the reasons videos couldn't be uploaded |
| GET      | `/api/v1/analytics/evidence/monthly`    | BUSINESS_OWNER, CLINIC_HEAD, OFFICE_ADMIN       | The evidence report per therapist month by month — calendar months in the organisation's timezone, every month in the window zero-filled |
| GET      | `/api/v1/analytics/evidence/children`   | BUSINESS_OWNER, CLINIC_HEAD, OFFICE_ADMIN       | The evidence report per child — their therapies (via linked plans), active goals, goals completed with video / couldn't upload / no evidence, videos uploaded, sessions completed |
| GET      | `/api/v1/leave-policy/categories`       | All staff                                       | Active leave categories (Casual, Sick…); `?all=true` (admin roles) includes deactivated ones |
| POST/PATCH | `/api/v1/leave-policy/categories[/{id}]` | BUSINESS_OWNER, CLINIC_HEAD, OFFICE_ADMIN    | Add, rename, set/clear the yearly allowance, or (de)activate a category — never deleted, so past leaves keep their label |
| GET/PUT  | `/api/v1/leave-policy/settings`         | GET all staff; PUT admin roles                  | The month the leave year starts (1 = calendar year, 4 = April–March) |
| GET      | `/api/v1/leave-policy/my-balances`      | All staff                                       | The caller's allocated / used / pending / remaining working days per category for a leave year |
| GET      | `/api/v1/leave-policy/balances`         | BUSINESS_OWNER, CLINIC_HEAD, OFFICE_ADMIN       | Every staff member's balances plus their pending / approved / rejected request counts — therapist-wise leave status |
| PUT      | `/api/v1/leave-policy/allocations`      | BUSINESS_OWNER, CLINIC_HEAD, OFFICE_ADMIN       | One person's own allocation for a category in a leave year (overrides the category default); omit `days` to reset |
| GET      | `/api/v1/iep/{planId}/pacing`           | THERAPIST, BUSINESS_OWNER, CLINIC_HEAD          | How the plan's active goals are spread across the upcoming sessions of its linked therapy — per-goal session budget and suggested target date, sessions left, sessions per week, and a "tight" flag when the average goal would get under 3 sessions |
| POST     | `/api/v1/iep/{planId}/pacing/apply`     | THERAPIST, BUSINESS_OWNER, CLINIC_HEAD          | Set each active goal's target date to its suggested date (re-spreads across the sessions that currently exist) |
| GET      | `/api/v1/iep/custom-domains`            | THERAPIST, BUSINESS_OWNER, CLINIC_HEAD, OFFICE_ADMIN | The organisation's own IEP goal domains (in addition to the built-in ones) |
| POST     | `/api/v1/iep/custom-domains`            | THERAPIST, BUSINESS_OWNER, CLINIC_HEAD          | Add a custom domain — returns the existing one if the name is already there (case-insensitive) |
| DELETE   | `/api/v1/iep/custom-domains/{id}`       | BUSINESS_OWNER, CLINIC_HEAD                     | Remove a custom domain from the picker; goals already using it keep its name |
| GET      | `/api/v1/dashboard/org-overview`        | BUSINESS_OWNER, CLINIC_HEAD, OFFICE_ADMIN       | Active/inactive case counts + active/invited member counts for the dashboard's Organisation Overview rings (SQL counts, no row loading) |
| GET      | `/api/v1/dashboard/attention-counts`    | BUSINESS_OWNER, CLINIC_HEAD, OFFICE_ADMIN       | Counts behind the needs-attention cards — sessions awaiting reschedule, cancellation requests, open concerns (SQL counts). The dashboard fetches each card's full list only when its count is above zero |
| GET      | `/api/v1/analytics/patients/{id}/progress` | BUSINESS_OWNER, CLINIC_HEAD, PARENT (own child) | Mastery series + per-domain breakdown |
| GET      | `/api/v1/analytics/patients/{id}/activities` | BUSINESS_OWNER, CLINIC_HEAD, PARENT (own child) | Activity assignment/attempt progress |
| GET      | `/api/v1/analytics/patients/{id}/frequency` | BUSINESS_OWNER, CLINIC_HEAD, PARENT (own child) | Sessions/week across every concurrent enrollment |
| GET      | `/api/v1/analytics/enrollments/{id}/success-criteria` | All staff + PARENT (own child)             | Goal mastery / therapist sign-off / parent satisfaction composite |
| PATCH    | `/api/v1/enrollments/{id}/therapist-signoff` | THERAPIST (own, once care status is Review or Program Completed) | Confirm this program's goals were met |
| GET      | `/api/v1/patients/{id}/discharge/preview` | BUSINESS_OWNER, CLINIC_HEAD | Dry run of what discharging this patient now would look like |
| POST     | `/api/v1/patients/{id}/discharge`       | BUSINESS_OWNER, CLINIC_HEAD | Discharge — closes every enrollment in the current episode, sets patient stage |
| GET      | `/api/v1/patients/{id}/discharge`       | All staff + PARENT (own child)                               | List discharge episodes, most recent first |
| GET      | `/api/v1/patients/{id}/discharge/{dischargeId}` | All staff + PARENT (own child)                       | One discharge episode's report |
| GET      | `/api/v1/patients/{id}/discharge/{dischargeId}/pdf` | All staff + PARENT (own child)                   | Discharge PDF — generated on first call, then a fresh short-lived URL each time |
| GET      | `/api/v1/analytics/therapists/{id}/caseload` | BUSINESS_OWNER, CLINIC_HEAD                | Therapist series + a row per patient + program/therapy-type breakdown of their caseload |
| GET      | `/api/v1/analytics/overview`            | BUSINESS_OWNER, CLINIC_HEAD                     | Org rollup (WEEKLY/MONTHLY only)    |
| GET      | `/api/v1/analytics/engagement-overview` | BUSINESS_OWNER, CLINIC_HEAD                     | Org-wide engagement rollup for the Overview analytics tab — users, sessions, skills, checklist fills |
| GET      | `/api/v1/analytics/session-heatmap`     | BUSINESS_OWNER, CLINIC_HEAD                     | Session count per day in the window — powers the calendar heatmap |
| GET      | `/api/v1/analytics/cases`               | BUSINESS_OWNER, CLINIC_HEAD                     | One row per active patient — sessions, members/activities assigned, checklist fills, LT goals, payment status |
| GET      | `/api/v1/analytics/cases/trends`        | BUSINESS_OWNER, CLINIC_HEAD, OFFICE_ADMIN       | Trend buckets for every active case in one batched call — feeds the Cases tab's multi-case chart (replaces one `/patients/{id}/progress` request per case) |
| GET      | `/api/v1/analytics/members`             | BUSINESS_OWNER, CLINIC_HEAD                     | One row per therapist — cases/activities assigned, activities created, sessions cancelled, IEP plans |
| GET      | `/api/v1/analytics/sessions`            | BUSINESS_OWNER, CLINIC_HEAD                     | Flat session log + KPI strip for the Schedule tab, optionally filtered by patientId/therapistId/programId |
| GET      | `/api/v1/users/assignable`              | BUSINESS_OWNER, CLINIC_HEAD, THERAPIST, OFFICE_ADMIN | Staff names + roles for assignee pickers; optional `role` param scopes to one role (e.g. the review-meeting Clinic-Head picker) |
| GET      | `/api/v1/review-meetings`               | All staff + PARENT (own children)                       | List review meetings                |
| GET      | `/api/v1/review-meetings/slots`         | BUSINESS_OWNER, CLINIC_HEAD, OFFICE_ADMIN       | The Review Session grid (`clinicHeadIds`, `date`, optional `excludeMeetingId`) — each Clinic Head's own configured times (or the org default), available/booked |
| GET      | `/api/v1/review-meetings/clinic-heads/{id}/slot-times` | BUSINESS_OWNER, CLINIC_HEAD, OFFICE_ADMIN | One Clinic Head's effective Review Session grid — their own override if set, else the org default |
| PUT      | `/api/v1/review-meetings/clinic-heads/{id}/slot-times` | BUSINESS_OWNER, CLINIC_HEAD, OFFICE_ADMIN | Full-replacement of a Clinic Head's own grid — empty `times` clears the override |
| POST     | `/api/v1/review-meetings`               | BUSINESS_OWNER, CLINIC_HEAD, OFFICE_ADMIN       | Add one review meeting to a plan — invites the patient's parents + the given Clinic Head(s); the therapist is not a participant |
| POST     | `/api/v1/review-meetings/schedule/{enrollmentId}` | BUSINESS_OWNER, CLINIC_HEAD, OFFICE_ADMIN | Generate a recurring review schedule — same Clinic-Head-picker requirement |
| PATCH    | `/api/v1/review-meetings/{id}/participants` | BUSINESS_OWNER, CLINIC_HEAD, OFFICE_ADMIN   | Full-replacement edit of a meeting's participant list (any active org user, not Clinic-Head-restricted); bumps the ics sequence and resends invites |
| PATCH    | `/api/v1/review-meetings/{id}/reschedule` | BUSINESS_OWNER, CLINIC_HEAD                   | Move a meeting; resends the invite  |
| PATCH    | `/api/v1/review-meetings/{id}/cancel`   | BUSINESS_OWNER, CLINIC_HEAD                     | Cancel; sends a CANCEL ics          |
| PATCH    | `/api/v1/review-meetings/{id}/complete` | All staff                                               | Mark a meeting completed            |
| PUT      | `/api/v1/review-meetings/{id}/parent-feedback`    | PARENT (linked to patient)                    | Rating + comments on the therapist  |
| PUT      | `/api/v1/review-meetings/{id}/therapist-feedback` | THERAPIST, CLINIC_HEAD, BUSINESS_OWNER      | Summary + progress notes            |
| PATCH    | `/api/v1/enrollments/{id}/therapist`    | BUSINESS_OWNER, CLINIC_HEAD                     | Reassign an ongoing plan's therapist |
| PATCH    | `/api/v1/enrollments/{id}/schedule`     | BUSINESS_OWNER, CLINIC_HEAD, OFFICE_ADMIN       | Edit an active plan's start time and/or session days and/or therapist, effective from a chosen date — sessions before that date are untouched, sessions on/after it are re-dated onto the new pattern (same session count) |
| POST     | `/api/v1/therapist-reassignments`       | Admin Roles                                     | Bulk-reassign selected cases from one therapist to another — permanent, or bounded to a start/end window that hands back automatically; moves scheduled sessions, upcoming review meetings, active IEP plans and the caseload link |
| GET      | `/api/v1/therapist-reassignments`       | Admin Roles                                     | List reassignment batches a therapist appears in (either side); optional `status` filter |
| PATCH    | `/api/v1/therapist-reassignments/{id}/cancel` | Admin Roles                               | End a TEMPORARY batch early — reverts still-`SCHEDULED`/`ACTIVE` rows; not available for PERMANENT batches |
| PATCH    | `/api/v1/enrollments/{id}/care-status`  | THERAPIST (own, not PROGRAM_COMPLETED), CLINIC_HEAD, BUSINESS_OWNER, OFFICE_ADMIN | Set clinical-health signal; PROGRAM_COMPLETED also completes the enrollment and cancels its still-upcoming sessions (admin tier only — overrides the normal all-sessions-attended completion path), and optionally accepts `manualGoalMasteryPct`/`manualParentSatisfactionPct`/`therapistSignedOff` to fill in the discharge success criteria by hand |
| PATCH    | `/api/v1/enrollments/{id}/reactivate`   | CLINIC_HEAD, BUSINESS_OWNER, OFFICE_ADMIN | Undo a force-complete override — back to ACTIVE/ON_TRACK, clears the manual success-criteria overrides, restores exactly the sessions that override auto-cancelled; refused if the enrollment was closed by a discharge instead |
| POST     | `/api/v1/enrollment-concerns`           | PARENT (own child)                                      | Raise a concern about an active program |
| GET      | `/api/v1/enrollment-concerns`           | All staff + PARENT (own child)                          | List concerns by `enrollmentId`, `patientId`, or org-wide `status` |
| GET      | `/api/v1/enrollment-concerns/open-count`| All staff                                                | Open-concern count (own caseload for THERAPIST) |
| PATCH    | `/api/v1/enrollment-concerns/{id}/acknowledge` | All staff (own caseload for THERAPIST)     | Acknowledge a concern |
| PATCH    | `/api/v1/enrollment-concerns/{id}/resolve` | All staff (own caseload for THERAPIST)         | Resolve a concern |
| POST     | `/api/v1/therapy-sessions/ad-hoc`       | BUSINESS_OWNER, CLINIC_HEAD                     | Book a one-off session from the calendar |
| GET      | `/api/v1/therapy-sessions/summary`      | BUSINESS_OWNER, CLINIC_HEAD, THERAPIST, PARENT, OFFICE_ADMIN | Slim session rows for `from`/`to` — names, program, times, status only (one joined query; role-scoped like the full list). For lists that just draw a row (dashboard Today's Sessions, sidebar calendar badge) |
| GET      | `/api/v1/therapy-sessions/{id}`         | BUSINESS_OWNER, CLINIC_HEAD, THERAPIST (own), PARENT (own child), OFFICE_ADMIN | One session in full — notes, feedback, plan totals, and the parent's remaining reschedules. Only this endpoint (and single-session mutation responses) computes `parentReschedulesRemaining`; the list endpoints return 0 for it |
| GET      | `/api/v1/therapy-sessions/{id}/activity` | THERAPIST, CLINIC_HEAD, BUSINESS_OWNER, OFFICE_ADMIN | A session's Activity Log, newest first — status changes (completed, cancelled, no show), cancellation requests/approvals, reschedules, scores given, notes saved/edited, checklist saves and attachments, each with before → after values; includes older notes-edit history (`legacy`) |
| GET      | `/api/v1/therapy-sessions/{id}/feedback` | THERAPIST, CLINIC_HEAD, BUSINESS_OWNER | Session feedback checklist template (per the session's program) + this session's answers |
| PUT      | `/api/v1/therapy-sessions/{id}/feedback` | THERAPIST, CLINIC_HEAD, BUSINESS_OWNER | Save this session's feedback checklist answers |
| GET      | `/api/v1/programs/{id}/feedback-template` | BUSINESS_OWNER, CLINIC_HEAD                    | Get a program's session feedback checklist template |
| PUT      | `/api/v1/programs/{id}/feedback-template` | BUSINESS_OWNER, CLINIC_HEAD                    | Replace a program's session feedback checklist template |
| GET      | `/api/v1/patients/{patientId}/assessments/{type}/definition` | All staff + PARENT (own child) | Fixed ISAA/PRBA item/section definition |
| GET      | `/api/v1/patients/{patientId}/assessments/{type}` | All staff + PARENT (own child)             | List a patient's ISAA/PRBA fills, oldest first |
| GET      | `/api/v1/patients/{patientId}/assessments/{type}/{assessmentId}/pdf` | All staff + PARENT (own child) | Assessment fill as a PDF, laid out like the paper form |
| POST     | `/api/v1/patients/{patientId}/assessments/{type}` | BUSINESS_OWNER, CLINIC_HEAD | Record a new ISAA/PRBA fill — score + classification computed server-side; THERAPIST and OFFICE_ADMIN are view-only |
| GET      | `/api/v1/patients/{patientId}/baseline-report` | All staff + PARENT (own child) | Baseline vs. current tracking, or null if none created yet |
| POST/PATCH | `/api/v1/patients/{patientId}/baseline-report` | BUSINESS_OWNER, CLINIC_HEAD | Create/update the baseline report; THERAPIST and OFFICE_ADMIN are view-only |
| POST     | `/api/v1/patients/{patientId}/baseline-report/domains/{domain}/progress` | BUSINESS_OWNER, CLINIC_HEAD | Log a dated "current" entry for one domain |
| GET      | `/api/v1/patients/{patientId}/baseline-report/domains/{domain}/progress` | All staff + PARENT (own child) | List one domain's dated entries, newest first |
| GET      | `/api/v1/therapist-activity` | BUSINESS_OWNER, CLINIC_HEAD | One therapist's session notes, free-text notes and media over a date range (`therapistId`, `from`, `to` params — a single day is just from == to; capped at 63 days), broken down per child |
| POST     | `/api/v1/meetings`                      | BUSINESS_OWNER, CLINIC_HEAD                             | Schedule a meeting + email invites — optionally recurring (`recurring`/`recurrenceDays`/`recurrenceEndDate`), generating one row per matching weekday up to `recurrenceEndDate` (holidays and the org's weekly off days skipped), all sharing a `seriesId`; exactly one invite email per participant announces the whole series (ICS carries an RRULE), not one per occurrence |
| GET      | `/api/v1/meetings`                      | Authenticated                                           | Meetings in a date range (scoped)   |
| GET      | `/api/v1/meetings/{id}`                 | Authenticated                                           | One meeting with participants       |
| PATCH    | `/api/v1/meetings/{id}/notes`           | BUSINESS_OWNER, CLINIC_HEAD, THERAPIST, OFFICE_ADMIN (organiser/participant, or admin tier) | Per-occurrence post-meeting write-up — independent per row, even within a recurring series |
| PATCH    | `/api/v1/meetings/{id}/cancel`          | All staff (not PARENT/PATIENT)                          | Cancel + send CANCEL ics            |
| GET      | `/api/v1/users/search`                  | BUSINESS_OWNER, CLINIC_HEAD                                   | Search users by email               |
| GET      | `/api/v1/organisation`                  | BUSINESS_OWNER, CLINIC_HEAD                                   | Org profile                         |
| PATCH    | `/api/v1/organisation`                  | BUSINESS_OWNER, CLINIC_HEAD                                   | Update org profile                  |
| GET      | `/api/v1/clinics`                       | All authenticated                                       | List clinics in org                 |
| POST     | `/api/v1/clinics`                       | BUSINESS_OWNER, CLINIC_HEAD                                   | Create clinic                       |
| GET      | `/api/v1/clinics/{id}`                  | All authenticated                                       | Clinic detail                       |
| PATCH    | `/api/v1/clinics/{id}`                  | BUSINESS_OWNER, CLINIC_HEAD                                   | Update clinic                       |
| GET      | `/api/v1/patients`                      | BUSINESS_OWNER, CLINIC_HEAD, THERAPIST | Paginated patients list — 20/page, sorted `createdAt` desc by default; `search`, `mine`, `status` (comma-separated ACTIVE/INACTIVE — Active = not discharged, Inactive = stage DISCHARGED), `compact` (id-only parent/therapist stubs) filters |
| POST     | `/api/v1/patients`                      | BUSINESS_OWNER, CLINIC_HEAD, OFFICE_ADMIN                     | Create patient                      |
| GET      | `/api/v1/patients/{id}`                 | BUSINESS_OWNER, CLINIC_HEAD, THERAPIST | Patient detail                      |
| GET      | `/api/v1/patients/my-children`          | PARENT                                                  | The calling parent's linked children |
| GET      | `/api/v1/patients/by-parent/{parentId}` | BUSINESS_OWNER, CLINIC_HEAD                             | Admin-facing equivalent of `my-children` for an arbitrary parent — powers the Parent view on their Member Profile page |
| POST     | `/api/v1/patients/{id}/conditions`      | BUSINESS_OWNER, CLINIC_HEAD, THERAPIST                        | Add condition to patient            |
| POST     | `/api/v1/patients/{id}/parents`         | BUSINESS_OWNER, CLINIC_HEAD                                   | Link parent to patient              |
| POST     | `/api/v1/patients/{id}/therapists`      | BUSINESS_OWNER, CLINIC_HEAD                                   | Assign therapist to patient         |
| GET      | `/api/v1/conditions`                    | All authenticated                                       | List all conditions (lookup)        |
| POST     | `/api/v1/invitations`                   | BUSINESS_OWNER, CLINIC_HEAD                                   | Invite user by email + role         |
| GET      | `/api/v1/invitations`                   | BUSINESS_OWNER, CLINIC_HEAD                                   | List sent invitations               |
| POST     | `/api/v1/inquiries/manual`              | BUSINESS_OWNER, CLINIC_HEAD                     | Record a walk-in / phoned-in inquiry |
| POST     | `/api/v1/invitations/{id}/resend`       | BUSINESS_OWNER, CLINIC_HEAD, OFFICE_ADMIN               | Resend an unaccepted invitation     |
| PATCH    | `/api/v1/invitations/{id}/cancel`       | BUSINESS_OWNER, CLINIC_HEAD, OFFICE_ADMIN               | Withdraw an unaccepted invitation   |
| POST     | `/api/v1/invitations/accept`            | Public                                                  | Accept invite → create account      |
| GET      | `/api/v1/availability-slots`            | All authenticated                                       | List availability slots             |
| POST     | `/api/v1/availability-slots`            | BUSINESS_OWNER, CLINIC_HEAD                                   | Create availability slot            |
| DELETE   | `/api/v1/availability-slots/{id}`       | BUSINESS_OWNER, CLINIC_HEAD                                   | Delete availability slot            |
| GET      | `/api/v1/appointments`                  | All authenticated                                       | List appointments (role-scoped)     |
| POST     | `/api/v1/appointments`                  | PARENT, BUSINESS_OWNER, CLINIC_HEAD                           | Book appointment                    |
| PATCH    | `/api/v1/appointments/{id}/status`      | All authenticated                                       | Update appointment status           |
| POST     | `/api/v1/leaves`                        | THERAPIST | Apply for leave                     |
| GET      | `/api/v1/leaves`                        | BUSINESS_OWNER/CLINIC_HEAD (all), THERAPIST (own only) | List leave requests; optional `?status=PENDING\|APPROVED\|REJECTED` |
| PATCH    | `/api/v1/leaves/{id}/review`            | BUSINESS_OWNER, CLINIC_HEAD                                   | Approve or reject a leave request   |
| DELETE   | `/api/v1/leaves/{id}`                   | THERAPIST | Cancel own pending leave            |
| GET      | `/api/v1/patients/{patientId}/shared-media` | BUSINESS_OWNER, CLINIC_HEAD, THERAPIST (assigned), PARENT (own child) | List videos/notes shared between the parent and the care team |
| POST     | `/api/v1/patients/{patientId}/shared-media` | BUSINESS_OWNER, CLINIC_HEAD, THERAPIST (assigned), PARENT (own child) | Share a video and/or a note — video is optional |
| DELETE   | `/api/v1/patients/{patientId}/shared-media/{id}` | Uploader, or BUSINESS_OWNER/CLINIC_HEAD             | Delete a shared video/note          |
| GET      | `/api/v1/calendar-blocks`               | All authenticated                                       | List the org's recurring calendar blocks (e.g. Lunch Break) — shown on every user's calendar, a rule the frontend expands into date-specific events, not materialized rows |
| POST     | `/api/v1/calendar-blocks`               | BUSINESS_OWNER, CLINIC_HEAD, OFFICE_ADMIN               | Add a recurring calendar block — title, time range, days of week, start date, optional end date |
| DELETE   | `/api/v1/calendar-blocks/{id}`          | BUSINESS_OWNER, CLINIC_HEAD, OFFICE_ADMIN               | Delete a recurring calendar block   |

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
| 080-member-profile-fields.sql       | `users.qualification`/`users.specialization` (free text) + `user_languages` join table (reuses the existing `languages` lookup) — member profile page |
| 081-baseline-score-percent.sql      | `baseline_domain_values.score_percent`/`baseline_progress_entries.score_percent` — optional 0-100 score alongside the free-text value, so a domain can be charted |
| 082-create-shared-media.sql         | `shared_media` table — videos/notes shared between parents and the care team per patient; either `file_url` or `note` must be set |
| 092-session-notes-history.sql       | `session_notes_history` — snapshot of a session's feedback/progress report/notes/performance score right before a later edit overwrites them |
| 093-enrollment-manual-success-criteria.sql | `enrollments.manual_goal_mastery_pct`/`manual_parent_satisfaction_pct` — admin-entered override for the two computed discharge success criteria, used only when a program is force-completed |
| 094-session-cancelled-by-completion.sql | `therapy_sessions.cancelled_by_program_completion` — marks a session auto-cancelled by the force-complete override, so reactivating restores exactly those and no others |
| 095-therapist-reassignments.sql     | `therapist_reassignments` (batch header) + `therapist_reassignment_cases` (one row per patient touched) |
| 096-reassignment-marker-columns.sql | Nullable `reassignment_id` FK on `therapy_sessions`, `review_meetings`, `therapist_patients`, `iep_plans` — marks which batch currently owns a row's `therapistId`, so revert only touches rows that batch moved |
| 097-review-meeting-participants.sql | `review_meeting_participants` — persisted, editable attendee list for review meetings (parents + chosen Clinic Head(s)); backfilled from existing linked parents, therapist deliberately not backfilled |
| 098-list-sort-indexes.sql           | `idx_patients_org_created` / `idx_users_org_created` — composite (org_id, created_at desc) indexes backing the paginated Cases/Members lists' default sort |
| 099-more-list-indexes.sql           | Composite indexes on `subscriptions`, `enrollments` (had none beyond PK), `tasks`, `invitations`, `attendance` — same missing-sort/scan-index gap as 098, found via a full-repository audit |
| 105-meeting-recurrence-and-notes.sql | `meetings.notes` (per-occurrence write-up) + `meetings.series_id`/`occurrence_number`/`total_occurrences` (recurring series) |
| 106-session-cancelled-by-case-inactive.sql | `therapy_sessions.cancelled_by_case_inactive` — marks a session auto-cancelled by marking a case inactive (patient-level analogue of 094), so reactivating the case restores exactly those |
| 109-org-calendar-blocks.sql          | `org_calendar_blocks` + `org_calendar_block_days` — org-wide recurring calendar blocks (e.g. Lunch Break), a rule not materialized rows, same approach as `organisation_weekly_off_days` |
| 110-org-geofence.sql                 | `organisations.latitude`/`longitude`/`geo_fence_radius_meters` — mirrors `clinics` (036); lets a BUSINESS_OWNER's attendance check-in be verified against the org's own address instead of a clinic |
| 111-review-session-slots.sql         | `organisation_review_slot_times` — the org-wide default daily grid a Review Meeting is booked into (a configurable list of times, as many as wanted); same collection-table pattern as `organisation_weekly_off_days` |
| 112-clinic-head-review-slots.sql     | `user_review_slot_times` — a CLINIC_HEAD's own grid, overriding the org default when set (no rows = inherits the org default) |
| 116-create-member-documents.sql      | `member_documents` — files kept on a staff member's record (category, title, stored file reference, notes, uploader) |
| 117-session-activity-events.sql      | `session_activity_events` — one row per thing that happened to a session (type, summary, actor, before/after JSON); powers the Activity Log |
| 118-widen-session-status.sql         | Widens `therapy_sessions.status` from VARCHAR(20) when still too narrow — `CANCELLATION_REQUESTED` is 22 chars and could not be stored |
| 119-goal-video-evidence.sql          | `goal_evidence` (video or can't-upload reason per child/goal/session/therapy), `iep_goals.completed_at` (backfilled), and `organisations.evidence_videos_required/max_video_mb/max_video_seconds` |
| 120-leave-policy.sql                 | `leave_categories`, `leave_allocations` (per-person override per leave year), `leaves.category_id`, `organisations.leave_year_start_month` |
| 121-evidence-uploads.sql             | `evidence_uploads` — direct uploads in flight (what was approved, until completed or swept) |
| 122-iep-custom-domains.sql           | `iep_custom_domains` (an organisation's own goal domains) + `custom_domain` on `iep_goals` / `iep_template_goals` |
| 113-phone-uniqueness.sql             | Normalises existing `users.phone` values (digits + leading `+` only) and adds `uq_users_phone` — a second login identity alongside email, so it must be unique too |

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

### Caching
- `@Cacheable` is used only on the org-level analytics service methods (`CacheConfig`); the key must start with `orgId` so one org can never be served another's data, and only immutable response DTOs are cached — never entities.
- New cached methods need a named cache registered in `CacheConfig.cacheManager()` and, if the result can be large, a case in `CacheConfig.weigh()` so the memory cap stays honest.

### IEP goal completion
- A goal can only move to COMPLETED once it has the org's required number of videos, or a recorded "couldn't upload" reason (`EvidenceService.assertGoalSatisfied`, called from `IEPService.updateGoal`). Completion stamps `iep_goals.completed_at`; reopening clears it. Deleting a goal or plan removes its evidence files too.
- An IEP plan can optionally link to one of the child's *ongoing* therapies (`enrollmentId`, validated in `IEPService`); `unlinkEnrollment` removes the link.
- Evidence endpoints judge access by `principal.getActiveRole()`, not "has the role" — a person who is both a therapist and a parent gets the rules of the hat they're wearing.

### Leave quotas
- Leave categories are optional: with none configured, leave is requested/approved with no quota exactly as before. Once any active category exists, a request must name one (`categoryId`) and is rejected (409) if it would exceed that category's allowance for the leave year.
- Quotas count **working days** — weekly off days and public holidays inside a range are excluded (`LeavePolicyService`). A request spanning two leave years draws on each year's balance for its own days. A person's own allocation (`leave_allocations`) overrides the category default; a null allowance means no limit.

### IEP goal pacing
- `GoalPacingService` divides a linked plan's not-completed goals (in creation order) across the therapy's *upcoming* sessions (counts-toward-plan, not cancelled, dated today or later): each gets floor(R/n) sessions, the first R mod n one more, and its suggested date is the date of the last session in its share. It reads the sessions that actually exist, so a change to the therapy's frequency or dates changes the suggestion — nothing is stored until "apply" writes `targetDate`.

### Direct-to-storage uploads
- `StorageService.prepareDirectUpload` issues a signed PUT (S3: presigned URL with `content-type` + `content-length` signed; local dev: an HMAC token handled by `FileController`'s `PUT /api/v1/files/direct/{token}`), and `storedSize` lets the server verify what arrived. `EvidenceUploadCleanupJob` (hourly) removes uploads — record and object — whose link expired over an hour ago.
- **Ops requirement:** the storage bucket's CORS must allow `PUT` with the `Content-Type` header from the website origins (`app.cors.allowed-origins`). If it doesn't, the website notices the browser can't reach storage and falls back to the older through-the-server upload (`POST /patients/{id}/evidence`, still supported), so nothing breaks — but large videos then go through the API server again.

### IEP custom domains
- A goal's domain is a built-in `IEPGoalDomain`, or `CUSTOM` with the name in `customDomain`. Saving a goal (or template goal) with a CUSTOM domain adds the name to the organisation's `iep_custom_domains` list via `IEPCustomDomainService.resolve` — so typing one new domain while creating a goal both defines and uses it. Names are tidied (trimmed, whitespace collapsed, 60 chars max), matched case-insensitively against the existing list, and a name that is really a built-in ("speech") resolves to the built-in.
- Anything that groups or filters by domain (analytics series, the analytics domain filter, discharge snapshots) must use `IEPGoal.domainKey()` — the custom name or the built-in's name — never `getDomain().name()`, which would lump every custom domain under "CUSTOM". CSV import still accepts built-in domains only.

### Multi-Tenancy
- Every query must filter by `orgId` from `principal.getOrgId()`
- Never expose data across organisations

### Scheduled jobs
- `@EnableScheduling` on `SimpleHearingApplication` (alongside `@EnableAsync`) — `reassignment/job/ReassignmentRevertJob.java` is the first `@Scheduled` job in the codebase; a small dedicated `@Component` delegating straight to a service method is the pattern to follow for the next one, rather than putting `@Scheduled` methods directly on a service.

---

## Adding a New Feature Module

1. Create package `com.simplehearing.<feature>/`
2. Add sub-packages: `entity/`, `dto/`, `repository/`, `service/`, `controller/`, `enums/` (if needed)
3. Write a Liquibase migration SQL file and register it in the master YAML
4. Entity → Repository → Service → Controller → DTO
5. Update this file's API table and migration table

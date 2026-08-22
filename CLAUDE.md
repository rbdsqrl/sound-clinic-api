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
│   │   ├── Role.java                    # BUSINESS_OWNER, ADMIN, THERAPIST, DOCTOR, PARENT, PATIENT
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
| GET      | `/api/v1/users/therapists`              | BUSINESS_OWNER, ADMIN                                   | All therapists/doctors in org       |
| GET      | `/api/v1/analytics/patients/{id}/progress` | BUSINESS_OWNER, ADMIN, OFFICE_ADMIN                  | Mastery series + per-domain breakdown |
| GET      | `/api/v1/analytics/therapists/{id}/caseload` | BUSINESS_OWNER, ADMIN, OFFICE_ADMIN                | Therapist series + a row per patient |
| GET      | `/api/v1/analytics/overview`            | BUSINESS_OWNER, ADMIN, OFFICE_ADMIN                     | Org rollup (WEEKLY/MONTHLY only)    |
| GET      | `/api/v1/users/assignable`              | BUSINESS_OWNER, ADMIN, OFFICE_ADMIN, THERAPIST, DOCTOR  | Staff names + roles for assignee pickers |
| GET      | `/api/v1/review-meetings`               | All staff + PARENT (own children)                       | List review meetings                |
| POST     | `/api/v1/review-meetings`               | BUSINESS_OWNER, ADMIN, OFFICE_ADMIN                     | Add one review meeting to a plan    |
| POST     | `/api/v1/review-meetings/schedule/{enrollmentId}` | BUSINESS_OWNER, ADMIN, OFFICE_ADMIN           | Generate a recurring review schedule |
| PATCH    | `/api/v1/review-meetings/{id}/reschedule` | BUSINESS_OWNER, ADMIN, OFFICE_ADMIN                   | Move a meeting; resends the invite  |
| PATCH    | `/api/v1/review-meetings/{id}/cancel`   | BUSINESS_OWNER, ADMIN, OFFICE_ADMIN                     | Cancel; sends a CANCEL ics          |
| PATCH    | `/api/v1/review-meetings/{id}/complete` | All staff                                               | Mark a meeting completed            |
| PUT      | `/api/v1/review-meetings/{id}/parent-feedback`    | PARENT (linked to patient)                    | Rating + comments on the therapist  |
| PUT      | `/api/v1/review-meetings/{id}/therapist-feedback` | THERAPIST, DOCTOR, ADMIN, BUSINESS_OWNER      | Summary + progress notes            |
| PATCH    | `/api/v1/enrollments/{id}/therapist`    | BUSINESS_OWNER, ADMIN, OFFICE_ADMIN                     | Reassign an ongoing plan's therapist |
| POST     | `/api/v1/meetings`                      | All staff (not PARENT/PATIENT)                          | Schedule a meeting + email invites  |
| GET      | `/api/v1/meetings`                      | Authenticated                                           | Meetings in a date range (scoped)   |
| GET      | `/api/v1/meetings/{id}`                 | Authenticated                                           | One meeting with participants       |
| PATCH    | `/api/v1/meetings/{id}/cancel`          | All staff (not PARENT/PATIENT)                          | Cancel + send CANCEL ics            |
| GET      | `/api/v1/users/search`                  | BUSINESS_OWNER, ADMIN                                   | Search users by email               |
| GET      | `/api/v1/organisation`                  | BUSINESS_OWNER, ADMIN                                   | Org profile                         |
| PATCH    | `/api/v1/organisation`                  | BUSINESS_OWNER, ADMIN                                   | Update org profile                  |
| GET      | `/api/v1/clinics`                       | All authenticated                                       | List clinics in org                 |
| POST     | `/api/v1/clinics`                       | BUSINESS_OWNER, ADMIN                                   | Create clinic                       |
| GET      | `/api/v1/clinics/{id}`                  | All authenticated                                       | Clinic detail                       |
| PATCH    | `/api/v1/clinics/{id}`                  | BUSINESS_OWNER, ADMIN                                   | Update clinic                       |
| GET      | `/api/v1/patients`                      | BUSINESS_OWNER, ADMIN, THERAPIST, DOCTOR                | List patients                       |
| POST     | `/api/v1/patients`                      | BUSINESS_OWNER, ADMIN, THERAPIST                        | Create patient                      |
| GET      | `/api/v1/patients/{id}`                 | BUSINESS_OWNER, ADMIN, THERAPIST, DOCTOR                | Patient detail                      |
| POST     | `/api/v1/patients/{id}/conditions`      | BUSINESS_OWNER, ADMIN, THERAPIST                        | Add condition to patient            |
| POST     | `/api/v1/patients/{id}/parents`         | BUSINESS_OWNER, ADMIN                                   | Link parent to patient              |
| POST     | `/api/v1/patients/{id}/therapists`      | BUSINESS_OWNER, ADMIN                                   | Assign therapist to patient         |
| GET      | `/api/v1/conditions`                    | All authenticated                                       | List all conditions (lookup)        |
| POST     | `/api/v1/invitations`                   | BUSINESS_OWNER, ADMIN                                   | Invite user by email + role         |
| GET      | `/api/v1/invitations`                   | BUSINESS_OWNER, ADMIN                                   | List sent invitations               |
| PATCH    | `/api/v1/invitations/{id}/cancel`       | BUSINESS_OWNER, ADMIN                                   | Withdraw an unaccepted invitation   |
| POST     | `/api/v1/invitations/accept`            | Public                                                  | Accept invite → create account      |
| GET      | `/api/v1/availability-slots`            | All authenticated                                       | List availability slots             |
| POST     | `/api/v1/availability-slots`            | BUSINESS_OWNER, ADMIN                                   | Create availability slot            |
| DELETE   | `/api/v1/availability-slots/{id}`       | BUSINESS_OWNER, ADMIN                                   | Delete availability slot            |
| GET      | `/api/v1/appointments`                  | All authenticated                                       | List appointments (role-scoped)     |
| POST     | `/api/v1/appointments`                  | PARENT, BUSINESS_OWNER, ADMIN                           | Book appointment                    |
| PATCH    | `/api/v1/appointments/{id}/status`      | All authenticated                                       | Update appointment status           |
| POST     | `/api/v1/leaves`                        | THERAPIST, DOCTOR                                       | Apply for leave                     |
| GET      | `/api/v1/leaves`                        | BUSINESS_OWNER/ADMIN (all), THERAPIST/DOCTOR (own only) | List leave requests; optional `?status=PENDING\|APPROVED\|REJECTED` |
| PATCH    | `/api/v1/leaves/{id}/review`            | BUSINESS_OWNER, ADMIN                                   | Approve or reject a leave request   |
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
- Mastery is **ratio of sums** (Σ`trials_passed` ÷ Σ`trials_total`), never an average of per-session ratios
- A period with no data serialises `masteryPct` as null — never 0, which would read as a regression
- Bucket on `LocalDate` fields (`session_date`, `meeting_date`), never on `created_at` (`Instant`)
- Always return coverage alongside a trend; a series built on thin coverage is a sampling artefact
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

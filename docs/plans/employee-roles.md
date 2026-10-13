# Employee Roles (custom roles with selectable access) — plan

Status: **proposed, not started** · Drafted 2026-10-13 · Revisit before building.

Goal: let Admin Roles create their own roles from **Organisation → Employee Roles**, choose which
access buckets each role gets, and pick those roles when inviting or editing a member. A custom
role behaves like a built-in role for all practical purposes, but is user-created.

---

## What the code looks like today

- Roles are a fixed Java enum (`Role`: CLINIC_HEAD, BUSINESS_OWNER, THERAPIST, PATIENT, PARENT,
  OFFICE_ADMIN), stored on `users.role`, `user_roles` (additional roles) and `invitations.role`,
  and embedded in the JWT (`role` + `roles` claims).
- Access is checked in ~278 `@PreAuthorize("hasAnyRole(...)")` annotations, ~100 in-service role
  checks (`hasRole(Role.X)`, `getRole()`, `getActiveRole()`), and ~130 role checks in the website.
  The sidebar is keyed by role (`NAV_BY_ROLE`).
- Roles also decide **data scoping**, not only access: a Therapist sees their own caseload, a
  Parent only their own children.

So a custom role cannot be just a new name — there has to be a permission layer beside roles.

## Core model

**Custom role = name + base type + set of access buckets.**

- **Base type** — drives data scoping and landing experience: *Admin-type* (whole organisation),
  *Therapist-type* (own caseload), or *Support* (no caseload).
- **Access buckets** — drive which actions are allowed.
- Built-in roles keep working unchanged: each gets a fixed, built-in bucket set that reproduces
  exactly what it can do today.

## Access buckets (View / Manage per bucket)

| Bucket | Covers |
|---|---|
| Cases | List/open cases; create/edit; assign therapists; discharge. Scope toggle: all cases or assigned only |
| Clinical records | IEP, assessments, baseline report, activities, session notes and media |
| Scheduling | Calendar, sessions, reschedule/cancellation approvals, review meetings, meetings |
| Members and parents | View, invite, edit profile, deactivate/reactivate, reassign cases |
| Staff documents | HR files (separate because sensitive) |
| Leave and attendance | Own leave and check-in, review requests, leave policy |
| Inquiries | Lead pipeline |
| Programs and library | Programs, activity library, IEP library |
| Analytics and reports | Dashboards, exports |
| Communication | Feed, Minutes of Meeting, tasks |
| Organisation settings | Profile, clinics, holidays, evidence rules, subscriptions |
| Employee roles and audit | Create/assign roles, see the Activity Log |

## Guardrails

- Business Owner is not editable or cloneable.
- A role cannot be granted a permission its creator does not hold (no privilege escalation).
- "Employee roles and audit" is grantable only by a Business Owner.
- The last Business Owner cannot be removed or downgraded.
- A role cannot be deleted while members still hold it (reassign first).

## Phases

**1. Permission layer — no visible change**
- Permission catalogue (`Permission` enum) + built-in bucket set per existing role matching today's behaviour.
- Effective permissions computed at login, exposed in JWT and `/users/me`; `hasPermission` helper for services and annotations.
- Migrate annotations gradually, most common admin-tier ones first (96 use the BUSINESS_OWNER/CLINIC_HEAD/OFFICE_ADMIN triple). Role checks stay valid meanwhile.
- Test matrix per base role proving no behaviour change.

**2. Custom roles — backend**
- Tables `custom_roles` (org, name, base type, active) and `custom_role_permissions`.
- Nullable `custom_role_id` on `users` and `invitations` — the enum columns stay untouched.
- CRUD endpoints + "who has this role"; changes recorded in the Activity Log.

**3. Organisation → Employee Roles (website)**
- New tab: role list, create/edit modal (name, base type, bucket picker with View/Manage), sidebar preview, members count, clone from existing role.

**4. Invite and assign**
- Invite modal lists custom roles beside base roles; member profile can change a member's custom role; badges and chips show the custom name.

**5. Frontend enforcement**
- Replace the ~130 role checks and `NAV_BY_ROLE` with permission checks driven by `/users/me`, page by page, sidebar first.

## Risks

- **Stale tokens** — role edits should apply without re-login. Recommended: look permissions up per request (cached) rather than trusting the token.
- **Scoping regressions** — ~100 in-service checks mix "may do X" with "only their own"; each needs review. Phase 1's test matrix is the safety net.
- **Role deletion / reassignment** — see guardrails.
- **Audit** — role create/edit/assign/delete must write to the Activity Log (`AuditLogService`).

## Open decisions

1. Base type — keep "pick a base type", or make custom roles purely bucket sets? (Recommended: keep; otherwise data scoping gets harder.)
2. One custom role per person, or custom + base roles together? (Recommended: one custom role, plus the existing additional roles.)
3. Staff only, or parents too? (Recommended: staff only.)
4. Granularity — View/Manage per bucket, or finer toggles such as "can discharge"? (Recommended: start coarse.)
5. Rollout — phases 1–2 behind a flag, migrating annotations across a few releases, or all at once? (Recommended: flag + gradual.)

## Rough size

Phase 1 is the heavy one (~278 annotations, ~100 service checks). Phases 2–4 are straightforward
once it lands.

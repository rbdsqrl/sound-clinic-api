--liquibase formatted sql

--changeset simplehearing:099-more-list-indexes
-- subscriptions/enrollments had no index at all beyond primary key, despite org-scoped lookups
-- (subscriptions: PatientService.buildResponse — runs once per patient row on every Cases/Patient
-- Detail/Dashboard load; enrollments: Analytics Overview, therapist caseload) hitting them constantly.
CREATE INDEX IF NOT EXISTS idx_subscriptions_org_patient_created ON subscriptions (org_id, patient_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_enrollments_org_patient_created   ON enrollments (org_id, patient_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_enrollments_org_therapist         ON enrollments (org_id, therapist_id);

-- tasks/invitations sort by created_at desc (Tasks page, Dashboard "My Tasks", Members > Invites)
-- with no index supporting that sort — same gap as patients/users fixed in migration 098.
CREATE INDEX IF NOT EXISTS idx_tasks_org_created       ON tasks (org_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_invitations_org_created ON invitations (org_id, created_at DESC);

-- attendance filters org_id + attendance_date together (daily attendance list, date-range queries)
-- but only had them indexed separately.
CREATE INDEX IF NOT EXISTS idx_attendance_org_date ON attendance (org_id, attendance_date DESC);

--rollback DROP INDEX idx_subscriptions_org_patient_created; DROP INDEX idx_enrollments_org_patient_created; DROP INDEX idx_enrollments_org_therapist; DROP INDEX idx_tasks_org_created; DROP INDEX idx_invitations_org_created; DROP INDEX idx_attendance_org_date;

--liquibase formatted sql

--changeset simplehearing:069-merge-admin-into-clinic-head
-- The ADMIN role is removed. OFFICE_ADMIN takes over its identity and gains every permission
-- ADMIN had — the two roles are merged into one, renamed CLINIC_HEAD ("Clinic Head" in the UI).
-- Every existing ADMIN and OFFICE_ADMIN user becomes CLINIC_HEAD.

-- user_roles has a composite (user_id, role) primary key — collapse any user who would end up
-- with a duplicate row (e.g. already has CLINIC_HEAD from a prior migration, or holds both
-- ADMIN and OFFICE_ADMIN as additional roles) before the rename, so the UPDATE below can't
-- hit a primary-key conflict.
DELETE FROM user_roles ur
WHERE ur.role IN ('ADMIN', 'OFFICE_ADMIN')
  AND EXISTS (
      SELECT 1 FROM user_roles ur2
      WHERE ur2.user_id = ur.user_id AND ur2.role = 'CLINIC_HEAD'
  );

DELETE FROM user_roles ur
WHERE ur.role = 'OFFICE_ADMIN'
  AND EXISTS (
      SELECT 1 FROM user_roles ur2
      WHERE ur2.user_id = ur.user_id AND ur2.role = 'ADMIN'
  );

UPDATE user_roles SET role = 'CLINIC_HEAD' WHERE role IN ('ADMIN', 'OFFICE_ADMIN');

-- users.role and invitations.role are single-value columns — no collision risk.
UPDATE users SET role = 'CLINIC_HEAD' WHERE role IN ('ADMIN', 'OFFICE_ADMIN');
UPDATE invitations SET role = 'CLINIC_HEAD' WHERE role IN ('ADMIN', 'OFFICE_ADMIN');

-- No data rollback: once merged, former ADMIN and OFFICE_ADMIN rows are indistinguishable.
--rollback SELECT 1;

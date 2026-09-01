--liquibase formatted sql

--changeset simplehearing:090-remove-doctor-role
-- The DOCTOR role is removed from the system. Every existing DOCTOR user becomes a THERAPIST
-- and keeps every permission/behaviour a THERAPIST has going forward.

-- user_roles has a composite (user_id, role) primary key — collapse any user who would end up
-- with a duplicate row (already holds THERAPIST from a prior migration, or holds both DOCTOR
-- and THERAPIST as additional roles) before the rename, so the UPDATE below can't hit a
-- primary-key conflict.
DELETE FROM user_roles ur
WHERE ur.role = 'DOCTOR'
  AND EXISTS (
      SELECT 1 FROM user_roles ur2
      WHERE ur2.user_id = ur.user_id AND ur2.role = 'THERAPIST'
  );

UPDATE user_roles SET role = 'THERAPIST' WHERE role = 'DOCTOR';

-- users.role and invitations.role are single-value columns — no collision risk.
UPDATE users SET role = 'THERAPIST' WHERE role = 'DOCTOR';
UPDATE invitations SET role = 'THERAPIST' WHERE role = 'DOCTOR';

-- No data rollback: once merged, former DOCTOR rows are indistinguishable from THERAPIST.
--rollback SELECT 1;

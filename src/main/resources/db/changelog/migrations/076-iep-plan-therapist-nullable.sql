--liquibase formatted sql

--changeset simplehearing:076-iep-plan-therapist-nullable
-- An IEP plan can now be created without a therapist attached (e.g. by a Clinic Head or
-- Business Owner before one is assigned); a Business Owner/Clinic Head fills it in later.
ALTER TABLE iep_plans ALTER COLUMN therapist_id DROP NOT NULL;

--rollback ALTER TABLE iep_plans ALTER COLUMN therapist_id SET NOT NULL;

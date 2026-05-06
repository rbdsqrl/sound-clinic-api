--liquibase formatted sql

--changeset simplehearing:026-make-enrollment-day-optional
ALTER TABLE enrollments ALTER COLUMN day_of_week DROP NOT NULL;

--rollback ALTER TABLE enrollments ALTER COLUMN day_of_week VARCHAR(10) NOT NULL;

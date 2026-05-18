--liquibase formatted sql

--changeset simplehearing:033-session-performance-score
ALTER TABLE therapy_sessions ADD COLUMN performance_score INTEGER;

--rollback ALTER TABLE therapy_sessions DROP COLUMN performance_score;

--liquibase formatted sql

--changeset simplehearing:139-session-moved-from
-- A session created by moving an incorrectly completed one to a later date points back at the session it replaces, so
-- it stays in the same program run (original or renewal batch). Null for every session that wasn't made that way.
ALTER TABLE therapy_sessions ADD COLUMN IF NOT EXISTS moved_from_session_id UUID;

--rollback ALTER TABLE therapy_sessions DROP COLUMN IF EXISTS moved_from_session_id;

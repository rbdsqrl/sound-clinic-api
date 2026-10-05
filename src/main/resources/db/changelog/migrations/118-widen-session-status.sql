--liquibase formatted sql

--changeset simplehearing:118-widen-session-status splitStatements:false
-- therapy_sessions.status was created as VARCHAR(20) (024), but CANCELLATION_REQUESTED is 22
-- characters, so a database built from these migrations could never store a cancellation request
-- (the UPDATE failed with "value too long for type character varying(20)"). Widen only when the
-- column is still too narrow, so a database whose column is already wider is left exactly as is.
DO $$
BEGIN
    IF (SELECT character_maximum_length
          FROM information_schema.columns
         WHERE table_name = 'therapy_sessions' AND column_name = 'status') < 30 THEN
        ALTER TABLE therapy_sessions ALTER COLUMN status TYPE VARCHAR(30);
    END IF;
END $$;

--rollback SELECT 1;

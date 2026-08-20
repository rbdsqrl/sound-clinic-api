--liquibase formatted sql

--changeset simplehearing:046-email-collision-check splitStatements:false
-- Email is the login identity, but the column was a plain VARCHAR with a case-sensitive UNIQUE
-- constraint, so "Owner@x.com" and "owner@x.com" could both exist as separate accounts. Lowering
-- them would then violate the new unique index. Fail loudly and list the offenders rather than
-- silently merging or dropping an account — resolving a collision is a business decision.
DO $$
DECLARE
    collisions TEXT;
BEGIN
    SELECT string_agg(DISTINCT lower(btrim(email)), ', ')
      INTO collisions
      FROM users
     GROUP BY lower(btrim(email))
    HAVING count(*) > 1;

    IF collisions IS NOT NULL THEN
        RAISE EXCEPTION
            'Cannot normalise emails — these differ only by case or whitespace: %. '
            'Deactivate or merge the duplicate accounts, then re-run.', collisions;
    END IF;
END $$;

--rollback SELECT 1;

--changeset simplehearing:046-normalise-existing-emails
-- Bring stored values into the canonical form the application now writes.
UPDATE users       SET email = lower(btrim(email)) WHERE email <> lower(btrim(email));
UPDATE invitations SET email = lower(btrim(email)) WHERE email <> lower(btrim(email));

--rollback SELECT 1;

--changeset simplehearing:046-email-lower-indexes
-- Case-insensitive uniqueness on users, and an index the new lower(email) lookups can actually use.
-- The old constraint is redundant once this exists: a lower() collision implies an exact collision.
ALTER TABLE users DROP CONSTRAINT IF EXISTS uq_users_email;

CREATE UNIQUE INDEX IF NOT EXISTS uq_users_email_lower       ON users (lower(email));
CREATE INDEX        IF NOT EXISTS idx_invitations_email_lower ON invitations (lower(email));

--rollback DROP INDEX IF EXISTS idx_invitations_email_lower;
--rollback DROP INDEX IF EXISTS uq_users_email_lower;
--rollback ALTER TABLE users ADD CONSTRAINT uq_users_email UNIQUE (email);

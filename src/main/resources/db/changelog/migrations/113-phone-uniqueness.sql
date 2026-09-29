--liquibase formatted sql

--changeset simplehearing:113-normalise-existing-phones
-- Bring stored values into the canonical form PhoneNormalizer now writes: digits and a
-- leading '+' only. Mirrors 046's email normalisation.
UPDATE users SET phone = regexp_replace(btrim(phone), '[^0-9+]', '', 'g') WHERE phone IS NOT NULL;
UPDATE users SET phone = NULL WHERE phone = '';

--rollback SELECT 1;

--changeset simplehearing:113-phone-collision-check splitStatements:false
-- Phone becomes a second login identity, so — same reasoning and same DO-block-first pattern as
-- 046 for email — fail loudly and list the offenders rather than silently keeping duplicates
-- that a unique index would then reject anyway. Resolving a real collision is a business decision.
DO $$
DECLARE
    collisions TEXT;
BEGIN
    SELECT string_agg(DISTINCT phone, ', ')
      INTO collisions
      FROM users
     WHERE phone IS NOT NULL
     GROUP BY phone
    HAVING count(*) > 1;

    IF collisions IS NOT NULL THEN
        RAISE EXCEPTION
            'Cannot add the phone uniqueness constraint — these numbers are shared by more than '
            'one user: %. Clear or fix the duplicates, then re-run.', collisions;
    END IF;
END $$;

--rollback SELECT 1;

--changeset simplehearing:113-phone-unique-index
-- Multiple NULLs are allowed by a plain unique index (users with no phone don't collide).
CREATE UNIQUE INDEX IF NOT EXISTS uq_users_phone ON users (phone);

--rollback DROP INDEX IF EXISTS uq_users_phone;

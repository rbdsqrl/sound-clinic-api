--liquibase formatted sql

--changeset simplehearing:080-member-profile-fields
ALTER TABLE users ADD COLUMN IF NOT EXISTS qualification VARCHAR(500);
ALTER TABLE users ADD COLUMN IF NOT EXISTS specialization VARCHAR(500);

CREATE TABLE IF NOT EXISTS user_languages (
    user_id     UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    language_id UUID NOT NULL REFERENCES languages(id) ON DELETE CASCADE,
    PRIMARY KEY (user_id, language_id)
);
CREATE INDEX IF NOT EXISTS idx_user_languages_user ON user_languages (user_id);

--rollback ALTER TABLE users DROP COLUMN IF EXISTS qualification; ALTER TABLE users DROP COLUMN IF EXISTS specialization; DROP TABLE IF EXISTS user_languages;

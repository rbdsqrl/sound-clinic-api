--liquibase formatted sql

--changeset simplehearing:059-org-ai-settings
-- Powers the Activity "Magic Fill" drafting feature. The API key is set once by the Business
-- Owner in Organisation Settings and never re-serialised to the frontend — only a
-- "configured yes/no" flag is. When ai_provider/ai_api_key are unset, Magic Fill is absent
-- from the Activity module for that org rather than erroring.
ALTER TABLE organisations ADD COLUMN IF NOT EXISTS ai_provider VARCHAR(20);
ALTER TABLE organisations ADD COLUMN IF NOT EXISTS ai_api_key VARCHAR(500);

--rollback ALTER TABLE organisations DROP COLUMN ai_provider; ALTER TABLE organisations DROP COLUMN ai_api_key;

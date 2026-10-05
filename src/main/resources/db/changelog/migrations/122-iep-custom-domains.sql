--liquibase formatted sql

--changeset simplehearing:122-iep-custom-domains
-- Organisation-defined IEP goal domains for when none of the built-in ones fits. A goal (or template goal)
-- using one has domain = 'CUSTOM' and the name in custom_domain; the name is kept on the goal itself so
-- reports and history stay right even if the picker list is later tidied.
CREATE TABLE IF NOT EXISTS iep_custom_domains (
    id          UUID          NOT NULL PRIMARY KEY,
    org_id      UUID          NOT NULL,
    name        VARCHAR(60)   NOT NULL,
    created_by  UUID,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);
CREATE UNIQUE INDEX IF NOT EXISTS uq_iep_custom_domains_org_name ON iep_custom_domains (org_id, lower(name));

ALTER TABLE iep_goals ADD COLUMN IF NOT EXISTS custom_domain VARCHAR(60);
ALTER TABLE iep_template_goals ADD COLUMN IF NOT EXISTS custom_domain VARCHAR(60);

--rollback ALTER TABLE iep_template_goals DROP COLUMN IF EXISTS custom_domain; ALTER TABLE iep_goals DROP COLUMN IF EXISTS custom_domain; DROP TABLE IF EXISTS iep_custom_domains;

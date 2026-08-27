--liquibase formatted sql

--changeset simplehearing:079-create-baseline-reports
CREATE TABLE IF NOT EXISTS baseline_reports (
    id                UUID NOT NULL PRIMARY KEY,
    org_id            UUID NOT NULL,
    patient_id        UUID NOT NULL UNIQUE REFERENCES patients (id) ON DELETE CASCADE,
    age_at_admission  VARCHAR(50),
    age_on_date       VARCHAR(50),
    cdct              VARCHAR(100),
    created_by        UUID NOT NULL REFERENCES users (id),
    created_at        TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at        TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS baseline_domain_values (
    id           UUID NOT NULL PRIMARY KEY,
    report_id    UUID NOT NULL REFERENCES baseline_reports (id) ON DELETE CASCADE,
    domain       VARCHAR(40) NOT NULL,
    value        TEXT,
    updated_by   UUID REFERENCES users (id),
    updated_at   TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    CONSTRAINT uq_baseline_domain_value UNIQUE (report_id, domain)
);

CREATE TABLE IF NOT EXISTS baseline_progress_entries (
    id           UUID NOT NULL PRIMARY KEY,
    report_id    UUID NOT NULL REFERENCES baseline_reports (id) ON DELETE CASCADE,
    domain       VARCHAR(40) NOT NULL,
    entry_date   DATE NOT NULL,
    value        TEXT NOT NULL,
    logged_by    UUID NOT NULL REFERENCES users (id),
    created_at   TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_baseline_progress_report_domain ON baseline_progress_entries (report_id, domain);

--rollback DROP TABLE baseline_progress_entries; DROP TABLE baseline_domain_values; DROP TABLE baseline_reports;

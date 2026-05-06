--liquibase formatted sql

--changeset simplehearing:020-create-inquiry-logs
CREATE TABLE inquiry_logs (
    id              UUID                     NOT NULL DEFAULT gen_random_uuid(),
    inquiry_id      UUID                     NOT NULL,
    log_type        VARCHAR(50)              NOT NULL,
    notes           TEXT,
    created_by      UUID,
    created_by_name VARCHAR(255),
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    CONSTRAINT pk_inquiry_logs PRIMARY KEY (id),
    CONSTRAINT fk_inquiry_logs_inquiry FOREIGN KEY (inquiry_id) REFERENCES inquiries(id)
);

CREATE INDEX idx_inquiry_logs_inquiry ON inquiry_logs(inquiry_id);

--rollback DROP TABLE inquiry_logs;

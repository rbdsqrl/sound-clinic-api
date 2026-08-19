--liquibase formatted sql

--changeset simplehearing:043-create-password-reset-tokens
CREATE TABLE IF NOT EXISTS password_reset_tokens (
    id           UUID         PRIMARY KEY,
    user_id      UUID         NOT NULL,
    token_hash   VARCHAR(255) NOT NULL,
    expires_at   TIMESTAMP WITH TIME ZONE  NOT NULL,
    used_at      TIMESTAMP WITH TIME ZONE,
    requested_ip VARCHAR(45),
    created_at   TIMESTAMP WITH TIME ZONE  NOT NULL DEFAULT now(),
    CONSTRAINT uq_password_reset_tokens_hash    UNIQUE (token_hash),
    CONSTRAINT fk_password_reset_tokens_user_id FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_password_reset_tokens_user_id ON password_reset_tokens (user_id);
CREATE INDEX IF NOT EXISTS idx_password_reset_tokens_created ON password_reset_tokens (created_at);

--rollback DROP TABLE password_reset_tokens;

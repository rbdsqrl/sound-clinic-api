--liquibase formatted sql

--changeset simplehearing:013-create-user-roles
-- Stores secondary/additional roles a user has beyond their primary role.
-- E.g. a THERAPIST who is also a PARENT will have a row (user_id, 'PARENT') here.
CREATE TABLE user_roles (
    user_id UUID        NOT NULL,
    role    VARCHAR(20) NOT NULL,
    CONSTRAINT pk_user_roles PRIMARY KEY (user_id, role),
    CONSTRAINT fk_user_roles_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE INDEX idx_user_roles_user_id ON user_roles (user_id);

--rollback DROP TABLE user_roles;

--liquibase formatted sql

--changeset simplehearing:009-create-conditions
CREATE TABLE conditions (
    id          UUID         PRIMARY KEY,
    name        VARCHAR(255) NOT NULL,
    description TEXT,
    is_active   BOOLEAN      NOT NULL DEFAULT true,
    CONSTRAINT uq_conditions_name UNIQUE (name)
);

-- Seed common conditions
INSERT INTO conditions (id, name, description) VALUES
    ('00000000-0000-0000-0000-000000000001', 'Hearing Loss',           'Partial or total inability to hear'),
    ('00000000-0000-0000-0000-000000000002', 'Speech Delay',           'Delayed development of speech and language skills'),
    ('00000000-0000-0000-0000-000000000003', 'Autism Spectrum Disorder','Neurodevelopmental condition affecting communication and behaviour'),
    ('00000000-0000-0000-0000-000000000004', 'Cerebral Palsy',         'Group of disorders affecting movement and motor skills'),
    ('00000000-0000-0000-0000-000000000005', 'Down Syndrome',          'Chromosomal condition causing intellectual disability'),
    ('00000000-0000-0000-0000-000000000006', 'Dyslexia',               'Learning disorder affecting reading and language processing'),
    ('00000000-0000-0000-0000-000000000007', 'ADHD',                   'Attention deficit hyperactivity disorder'),
    ('00000000-0000-0000-0000-000000000008', 'Cleft Palate',           'Structural difference in the palate affecting speech');

--rollback DROP TABLE conditions;

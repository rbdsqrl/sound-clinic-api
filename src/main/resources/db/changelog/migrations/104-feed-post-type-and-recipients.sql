--liquibase formatted sql

--changeset simplehearing:104-feed-post-type-and-recipients
ALTER TABLE feed_posts ADD COLUMN IF NOT EXISTS type VARCHAR(20) NOT NULL DEFAULT 'POST';

CREATE TABLE IF NOT EXISTS feed_post_recipients (
    post_id  UUID NOT NULL REFERENCES feed_posts (id) ON DELETE CASCADE,
    user_id  UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    PRIMARY KEY (post_id, user_id)
);
CREATE INDEX IF NOT EXISTS idx_feed_post_recipients_post ON feed_post_recipients (post_id);
CREATE INDEX IF NOT EXISTS idx_feed_post_recipients_user ON feed_post_recipients (user_id);

-- Who attended the meeting a MOM-type post records. Meaningless for a plain POST, and never
-- used for access control (that's recipients, above) — just an attendance record.
CREATE TABLE IF NOT EXISTS feed_post_attendees (
    post_id  UUID NOT NULL REFERENCES feed_posts (id) ON DELETE CASCADE,
    user_id  UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    PRIMARY KEY (post_id, user_id)
);
CREATE INDEX IF NOT EXISTS idx_feed_post_attendees_post ON feed_post_attendees (post_id);
CREATE INDEX IF NOT EXISTS idx_feed_post_attendees_user ON feed_post_attendees (user_id);

--rollback ALTER TABLE feed_posts DROP COLUMN type;
--rollback DROP TABLE feed_post_recipients;
--rollback DROP TABLE feed_post_attendees;

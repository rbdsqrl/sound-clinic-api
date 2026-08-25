--liquibase formatted sql

--changeset simplehearing:071-feed-post-engagement
-- Likes and views are unique-per-user rows (composite PK) — a like/view is idempotent per
-- user, not a raw hit counter. Comments and images mirror task_comments/task_attachments.

CREATE TABLE IF NOT EXISTS feed_post_likes (
    post_id     UUID NOT NULL REFERENCES feed_posts(id) ON DELETE CASCADE,
    user_id     UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    PRIMARY KEY (post_id, user_id)
);

CREATE TABLE IF NOT EXISTS feed_post_views (
    post_id           UUID NOT NULL REFERENCES feed_posts(id) ON DELETE CASCADE,
    user_id           UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    first_viewed_at   TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    PRIMARY KEY (post_id, user_id)
);

CREATE TABLE IF NOT EXISTS feed_post_comments (
    id          UUID         PRIMARY KEY,
    org_id      UUID         NOT NULL,
    post_id     UUID         NOT NULL REFERENCES feed_posts(id) ON DELETE CASCADE,
    author_id   UUID         NOT NULL REFERENCES users(id),
    body        TEXT         NOT NULL,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS feed_post_images (
    id               UUID         PRIMARY KEY,
    org_id           UUID         NOT NULL,
    post_id          UUID         NOT NULL REFERENCES feed_posts(id) ON DELETE CASCADE,
    uploaded_by      UUID         NOT NULL REFERENCES users(id),
    file_name        VARCHAR(255) NOT NULL,
    file_url         VARCHAR(1000) NOT NULL,
    content_type     VARCHAR(100),
    file_size_bytes  BIGINT,
    order_index      INT          NOT NULL DEFAULT 0,
    created_at       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_feed_post_likes_post    ON feed_post_likes(post_id);
CREATE INDEX IF NOT EXISTS idx_feed_post_views_post    ON feed_post_views(post_id);
CREATE INDEX IF NOT EXISTS idx_feed_post_comments_post ON feed_post_comments(post_id);
CREATE INDEX IF NOT EXISTS idx_feed_post_images_post   ON feed_post_images(post_id);

--rollback DROP TABLE feed_post_images; DROP TABLE feed_post_comments; DROP TABLE feed_post_views; DROP TABLE feed_post_likes;

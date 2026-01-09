-- Create table to track daily refresh usage per user
CREATE TABLE suggestion_refresh_tracker (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES app_users(id) ON DELETE CASCADE,
    refresh_date DATE NOT NULL,
    refresh_count INT NOT NULL DEFAULT 0,
    CONSTRAINT uq_user_refresh_date UNIQUE(user_id, refresh_date)
);

-- Index for efficient lookups by user and date
CREATE INDEX idx_refresh_tracker_user_date ON suggestion_refresh_tracker(user_id, refresh_date);

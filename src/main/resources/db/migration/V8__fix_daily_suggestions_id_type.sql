-- Drop and recreate daily_suggestions table with correct BIGINT id type
DROP TABLE IF EXISTS daily_suggestions;

CREATE TABLE daily_suggestions (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    movie_ids VARCHAR(100) NOT NULL,
    suggestion_date DATE NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_daily_suggestions_user 
        FOREIGN KEY (user_id) REFERENCES app_users(id) ON DELETE CASCADE,
    CONSTRAINT uq_user_suggestion_date 
        UNIQUE (user_id, suggestion_date)
);

CREATE INDEX idx_daily_suggestions_user_date 
    ON daily_suggestions(user_id, suggestion_date);

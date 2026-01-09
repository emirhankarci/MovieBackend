-- Create daily TV suggestions table for AI-generated TV series recommendations
CREATE TABLE daily_tv_suggestions (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    tv_series_ids VARCHAR(100) NOT NULL,
    suggestion_date DATE NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_daily_tv_suggestions_user 
        FOREIGN KEY (user_id) REFERENCES app_users(id) ON DELETE CASCADE,
    CONSTRAINT uq_user_tv_suggestion_date 
        UNIQUE (user_id, suggestion_date)
);

-- Index for efficient querying by user and date
CREATE INDEX idx_daily_tv_suggestions_user_date 
    ON daily_tv_suggestions(user_id, suggestion_date);

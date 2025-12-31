-- Search history table for AI recommendations
CREATE TABLE search_history (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    search_type VARCHAR(20) NOT NULL,
    query VARCHAR(500),
    filters TEXT,
    result_count INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_search_history_user FOREIGN KEY (user_id) REFERENCES app_users(id) ON DELETE CASCADE
);

-- Index for faster user-based queries (AI recommendations)
CREATE INDEX idx_search_history_user_id ON search_history(user_id);
CREATE INDEX idx_search_history_created_at ON search_history(created_at);
CREATE INDEX idx_search_history_user_created ON search_history(user_id, created_at DESC);

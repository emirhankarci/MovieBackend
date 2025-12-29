-- Chat messages table for AI conversation history
CREATE TABLE chat_messages (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    content TEXT NOT NULL,
    role VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Index for fetching user's messages
CREATE INDEX idx_chat_messages_user_id ON chat_messages(user_id);

-- Composite index for fetching user's messages ordered by time
CREATE INDEX idx_chat_messages_user_created ON chat_messages(user_id, created_at);

-- Index for daily message count queries
CREATE INDEX idx_chat_messages_user_role_created ON chat_messages(user_id, role, created_at);

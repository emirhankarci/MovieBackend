CREATE TABLE chat_reactions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id BIGINT NOT NULL REFERENCES app_users(id) ON DELETE CASCADE,
    message_id BIGINT NOT NULL,
    movie_id INT,
    reaction VARCHAR(10) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(user_id, message_id)
);

CREATE INDEX idx_chat_reactions_user_id ON chat_reactions(user_id);
CREATE INDEX idx_chat_reactions_message_id ON chat_reactions(message_id);

-- User Collections table
CREATE TABLE user_collections (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_user_collections_user FOREIGN KEY (user_id) REFERENCES app_users(id) ON DELETE CASCADE,
    CONSTRAINT uq_user_collection_name UNIQUE (user_id, name)
);

CREATE INDEX idx_user_collections_user_id ON user_collections(user_id);
CREATE INDEX idx_user_collections_created_at ON user_collections(created_at);

-- User Collection Movies table
CREATE TABLE user_collection_movies (
    id BIGSERIAL PRIMARY KEY,
    collection_id BIGINT NOT NULL,
    movie_id BIGINT NOT NULL,
    movie_title VARCHAR(255) NOT NULL,
    poster_path VARCHAR(500),
    added_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_collection_movies_collection FOREIGN KEY (collection_id) REFERENCES user_collections(id) ON DELETE CASCADE,
    CONSTRAINT uq_collection_movie UNIQUE (collection_id, movie_id)
);

CREATE INDEX idx_collection_movies_collection_id ON user_collection_movies(collection_id);
CREATE INDEX idx_collection_movies_added_at ON user_collection_movies(added_at);

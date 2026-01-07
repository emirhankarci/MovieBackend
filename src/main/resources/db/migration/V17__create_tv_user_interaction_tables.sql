-- TV User Interaction Tables
-- TV Watchlist, Watched Episodes, Ratings, Collections

-- TV Watchlist Table
CREATE TABLE tv_watchlist (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES app_users(id) ON DELETE CASCADE,
    series_id BIGINT NOT NULL,
    series_name VARCHAR(255) NOT NULL,
    poster_path VARCHAR(500),
    vote_average DECIMAL(3,1),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(user_id, series_id)
);

CREATE INDEX idx_tv_watchlist_user_id ON tv_watchlist(user_id);

-- Watched Episodes Table
CREATE TABLE watched_episodes (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES app_users(id) ON DELETE CASCADE,
    series_id BIGINT NOT NULL,
    series_name VARCHAR(255) NOT NULL,
    season_number INT NOT NULL,
    episode_number INT NOT NULL,
    episode_name VARCHAR(255),
    watched_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(user_id, series_id, season_number, episode_number)
);

CREATE INDEX idx_watched_episodes_user_series ON watched_episodes(user_id, series_id);

-- TV Series Ratings Table
CREATE TABLE tv_series_ratings (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES app_users(id) ON DELETE CASCADE,
    series_id BIGINT NOT NULL,
    series_name VARCHAR(255) NOT NULL,
    poster_path VARCHAR(500),
    rating DECIMAL(3,1) NOT NULL CHECK (rating >= 1.0 AND rating <= 10.0),
    rated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(user_id, series_id)
);

CREATE INDEX idx_tv_series_ratings_user_id ON tv_series_ratings(user_id);

-- Episode Ratings Table
CREATE TABLE episode_ratings (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES app_users(id) ON DELETE CASCADE,
    series_id BIGINT NOT NULL,
    series_name VARCHAR(255) NOT NULL,
    season_number INT NOT NULL,
    episode_number INT NOT NULL,
    episode_name VARCHAR(255),
    rating DECIMAL(3,1) NOT NULL CHECK (rating >= 1.0 AND rating <= 10.0),
    rated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(user_id, series_id, season_number, episode_number)
);

CREATE INDEX idx_episode_ratings_user_series ON episode_ratings(user_id, series_id);


-- TV Collections Table
CREATE TABLE tv_collections (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES app_users(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_tv_collections_user_id ON tv_collections(user_id);

-- TV Collection Series Table
CREATE TABLE tv_collection_series (
    id BIGSERIAL PRIMARY KEY,
    collection_id BIGINT NOT NULL REFERENCES tv_collections(id) ON DELETE CASCADE,
    series_id BIGINT NOT NULL,
    series_name VARCHAR(255) NOT NULL,
    poster_path VARCHAR(500),
    added_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(collection_id, series_id)
);

CREATE INDEX idx_tv_collection_series_collection_id ON tv_collection_series(collection_id);

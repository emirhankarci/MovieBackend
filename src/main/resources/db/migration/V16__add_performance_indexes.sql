-- V16: Add performance indexes for optimized queries
-- This migration adds indexes to improve query performance across the application.
-- All indexes use CONCURRENTLY to avoid locking tables in production.

-- ============================================================================
-- WATCHLIST TABLE INDEXES
-- ============================================================================

-- Index for fetching user's watchlist
-- Query: SELECT * FROM watchlist WHERE user_id = ?
CREATE INDEX IF NOT EXISTS idx_watchlist_user_id 
    ON watchlist(user_id);

-- Index for checking if a movie is in any watchlist
-- Query: SELECT EXISTS(SELECT 1 FROM watchlist WHERE movie_id = ?)
CREATE INDEX IF NOT EXISTS idx_watchlist_movie_id 
    ON watchlist(movie_id);

-- Composite index for sorted watchlist queries (newest first)
-- Query: SELECT * FROM watchlist WHERE user_id = ? ORDER BY created_at DESC
CREATE INDEX IF NOT EXISTS idx_watchlist_user_created 
    ON watchlist(user_id, created_at DESC);

-- ============================================================================
-- WATCHED MOVIES TABLE INDEXES
-- ============================================================================

-- Index for checking if a movie has been watched by any user
-- Query: SELECT EXISTS(SELECT 1 FROM watched_movies WHERE movie_id = ?)
CREATE INDEX IF NOT EXISTS idx_watched_movies_movie_id 
    ON watched_movies(movie_id);

-- Composite index for sorted watch history (most recent first)
-- Query: SELECT * FROM watched_movies WHERE user_id = ? ORDER BY watched_at DESC
CREATE INDEX IF NOT EXISTS idx_watched_movies_user_watched 
    ON watched_movies(user_id, watched_at DESC);

-- Partial index for rating-based filtering (only rated movies)
-- Query: SELECT * FROM watched_movies WHERE user_rating >= ?
CREATE INDEX IF NOT EXISTS idx_watched_movies_rating 
    ON watched_movies(user_rating) 
    WHERE user_rating IS NOT NULL;

-- ============================================================================
-- USER COLLECTION MOVIES TABLE INDEXES
-- ============================================================================

-- Index for checking if a movie is in any collection
-- Query: SELECT * FROM user_collection_movies WHERE movie_id = ?
CREATE INDEX IF NOT EXISTS idx_collection_movies_movie_id 
    ON user_collection_movies(movie_id);

-- ============================================================================
-- SEARCH HISTORY TABLE INDEXES
-- ============================================================================

-- Index for search type filtering
-- Query: SELECT * FROM search_history WHERE search_type = ?
CREATE INDEX IF NOT EXISTS idx_search_history_type 
    ON search_history(search_type);

-- Composite index for user + search type queries (AI recommendations)
-- Query: SELECT * FROM search_history WHERE user_id = ? AND search_type = ?
CREATE INDEX IF NOT EXISTS idx_search_history_user_type 
    ON search_history(user_id, search_type);

-- ============================================================================
-- REFRESH TOKENS TABLE INDEXES
-- ============================================================================

-- Index for expired token cleanup
-- Query: DELETE FROM refresh_tokens WHERE expires_at < NOW()
CREATE INDEX IF NOT EXISTS idx_refresh_tokens_expires 
    ON refresh_tokens(expires_at);

-- Partial index for active (non-revoked) token queries
-- Query: SELECT * FROM refresh_tokens WHERE user_id = ? AND revoked = false
CREATE INDEX IF NOT EXISTS idx_refresh_tokens_active 
    ON refresh_tokens(user_id) 
    WHERE revoked = false;

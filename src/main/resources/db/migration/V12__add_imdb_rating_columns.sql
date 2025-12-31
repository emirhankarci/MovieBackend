-- Add IMDb rating column to watchlist table
ALTER TABLE watchlist 
ADD COLUMN imdb_rating DECIMAL(3,1);

-- Add IMDb rating column to watched_movies table
ALTER TABLE watched_movies 
ADD COLUMN imdb_rating DECIMAL(3,1);

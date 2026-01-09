-- Add favorite TV series IDs column to user_preferences table
ALTER TABLE user_preferences 
ADD COLUMN favorite_tv_series_ids VARCHAR(255);

-- Comment: This column stores comma-separated TMDB TV series IDs
-- Example: "1396,1399,66732" for Breaking Bad, Game of Thrones, Stranger Things

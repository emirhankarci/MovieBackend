-- Convert user_rating from INTEGER to DECIMAL(3,1) to support half-point ratings (e.g., 7.5)
ALTER TABLE watched_movies 
ALTER COLUMN user_rating TYPE DECIMAL(3,1);

-- Drop existing constraint
ALTER TABLE watched_movies 
DROP CONSTRAINT IF EXISTS chk_user_rating;

-- Add new constraint for 0.5 increments (rating * 2 must be a whole number)
ALTER TABLE watched_movies 
ADD CONSTRAINT chk_user_rating 
CHECK (user_rating IS NULL OR (user_rating >= 1.0 AND user_rating <= 10.0 AND (user_rating * 2) = FLOOR(user_rating * 2)));

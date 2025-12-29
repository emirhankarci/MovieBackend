ALTER TABLE app_users
ADD COLUMN first_name VARCHAR(255) NOT NULL DEFAULT '',
ADD COLUMN last_name VARCHAR(255) NOT NULL DEFAULT '';

-- Remove default after adding columns if you want them to be strictly provided in future inserts
-- But for existing rows, we need a default or allow nulls temporarily.
-- Since the app is in development, default blank strings are fine.
ALTER TABLE app_users ALTER COLUMN first_name DROP DEFAULT;
ALTER TABLE app_users ALTER COLUMN last_name DROP DEFAULT;

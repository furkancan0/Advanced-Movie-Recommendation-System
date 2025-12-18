ALTER TABLE users
    ADD COLUMN google_id VARCHAR(255) UNIQUE,
ADD COLUMN oauth_provider VARCHAR(50) DEFAULT 'local';

-- Make password nullable for OAuth users
ALTER TABLE users ALTER COLUMN password DROP NOT NULL;

-- Create index on google_id
CREATE INDEX idx_users_google_id ON users(google_id);
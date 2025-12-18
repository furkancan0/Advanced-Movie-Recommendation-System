-- Add role column to users table
ALTER TABLE users
    ADD COLUMN role VARCHAR(20) NOT NULL DEFAULT 'USER';

-- Add enabled and account_locked columns
ALTER TABLE users
    ADD COLUMN enabled BOOLEAN DEFAULT TRUE,
ADD COLUMN account_locked BOOLEAN DEFAULT FALSE;

-- Create index on role for faster queries
CREATE INDEX idx_users_role ON users(role);

-- Update existing users to have USER role (if any exist)
UPDATE users SET role = 'USER' WHERE role IS NULL;

-- Add constraint to ensure role is valid
ALTER TABLE users
    ADD CONSTRAINT chk_user_role CHECK (role IN ('USER', 'ADMIN', 'MODERATOR'));
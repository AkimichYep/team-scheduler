-- Create user if it doesn't exist
CREATE USER scheduler_user WITH PASSWORD 'scheduler_password';

-- Create database
CREATE DATABASE team_scheduler OWNER scheduler_user;

-- Grant privileges
GRANT ALL PRIVILEGES ON DATABASE team_scheduler TO scheduler_user;

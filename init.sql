-- Create database if it doesn't exist
CREATE DATABASE team_scheduler;

-- Grant privileges to the scheduler_user
GRANT ALL PRIVILEGES ON DATABASE team_scheduler TO scheduler_user;

-- Connect to the database and grant schema privileges
\c team_scheduler
GRANT ALL ON SCHEMA public TO scheduler_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO scheduler_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON SEQUENCES TO scheduler_user;

-- =============================================================================
-- patch.sql  — run this ONCE against an existing database to add columns that
-- were added to the User entity after the initial schema was created.
--
-- Safe to run multiple times: every statement uses IF NOT EXISTS / IF EXISTS.
--
-- Usage (psql):
--   psql -h localhost -U <username> -d <dbname> -f src/main/resources/migrate/patch.sql
-- =============================================================================

-- 1. Add role column if missing (Hibernate stores UserRole as TEXT)
ALTER TABLE users
    ADD COLUMN IF NOT EXISTS role TEXT;

-- 2. Add email_verified with a DB-level default so existing rows get FALSE
ALTER TABLE users
    ADD COLUMN IF NOT EXISTS email_verified BOOLEAN NOT NULL DEFAULT FALSE;

-- 3. Add email_verified_at (nullable timestamp)
ALTER TABLE users
    ADD COLUMN IF NOT EXISTS email_verified_at TIMESTAMP;

-- 4. Add admin_approved with a DB-level default so existing rows get FALSE
ALTER TABLE users
    ADD COLUMN IF NOT EXISTS admin_approved BOOLEAN NOT NULL DEFAULT FALSE;

-- 5. Add admin_approved_at (nullable timestamp)
ALTER TABLE users
    ADD COLUMN IF NOT EXISTS admin_approved_at TIMESTAMP;

-- Verify
SELECT column_name, data_type, is_nullable, column_default
FROM information_schema.columns
WHERE table_name = 'users'
ORDER BY ordinal_position;

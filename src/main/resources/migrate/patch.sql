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

-- 6. Ensure carrier_id and broker_id are nullable (a broker user has no carrier and vice-versa)
ALTER TABLE users ALTER COLUMN carrier_id DROP NOT NULL;
ALTER TABLE users ALTER COLUMN broker_id DROP NOT NULL;

-- 7. Add login_disabled flag
ALTER TABLE users
    ADD COLUMN IF NOT EXISTS login_disabled BOOLEAN NOT NULL DEFAULT FALSE;

-- 8. Add last_modified_by / last_modified_at for Auditable
ALTER TABLE users ADD COLUMN IF NOT EXISTS created_by       TEXT;
ALTER TABLE users ADD COLUMN IF NOT EXISTS last_modified_by TEXT;
ALTER TABLE users ADD COLUMN IF NOT EXISTS last_modified_at TIMESTAMP;

-- 9. Security tokens table
CREATE TABLE IF NOT EXISTS security_tokens (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    token       TEXT    NOT NULL UNIQUE,
    token_type  TEXT    NOT NULL,
    user_id     UUID    NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    expires_at  TIMESTAMP NOT NULL,
    used        BOOLEAN NOT NULL DEFAULT FALSE,
    used_at     TIMESTAMP,
    created_by  TEXT,
    created_at  TIMESTAMP DEFAULT now(),
    last_modified_by TEXT,
    last_modified_at TIMESTAMP
);

-- Verify
SELECT column_name, data_type, is_nullable, column_default
FROM information_schema.columns
WHERE table_name = 'users'
ORDER BY ordinal_position;

-- =============================================================================
-- MISSING TABLES — create only if they do not already exist
-- =============================================================================

-- 10. loads table (LoadPosting entity)
CREATE TABLE IF NOT EXISTS loads (
    id                  UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    broker_id           UUID        NOT NULL REFERENCES brokers(id),
    assigned_carrier_id UUID        REFERENCES carriers(id),
    status              VARCHAR(20),
    pickup_type         VARCHAR(20),
    drop_type           VARCHAR(20),
    pickup_street       TEXT,
    pickup_city         TEXT,
    pickup_state        TEXT,
    pickup_zip          TEXT,
    pickup_country      TEXT,
    pickup_lot_number   TEXT,
    drop_street         TEXT,
    drop_city           TEXT,
    drop_state          TEXT,
    drop_zip            TEXT,
    drop_country        TEXT,
    drop_lot_number     TEXT,
    vehicle_make        TEXT,
    vehicle_model       TEXT,
    vehicle_year        INT,
    description         TEXT,
    weight              DOUBLE PRECISION,
    price               DOUBLE PRECISION,
    created_at          TIMESTAMP DEFAULT now()
);

-- If loads already existed with a stale carrier_id column, drop it.
ALTER TABLE loads DROP COLUMN IF EXISTS carrier_id;

-- 11b. Add pickup_date / delivery_date to loads (nullable, added after initial schema)
ALTER TABLE loads ADD COLUMN IF NOT EXISTS pickup_date   DATE;
ALTER TABLE loads ADD COLUMN IF NOT EXISTS delivery_date DATE;

-- 11. bids table (Bid entity)
CREATE TABLE IF NOT EXISTS bids (
    id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    load_id     UUID        NOT NULL REFERENCES loads(id),
    carrier_id  UUID        NOT NULL REFERENCES carriers(id),
    amount      NUMERIC(19, 2),
    book_now    BOOLEAN     NOT NULL DEFAULT FALSE,
    status      VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at  TIMESTAMP,
    updated_at  TIMESTAMP
);

-- 12. documents table (Document entity)
CREATE TABLE IF NOT EXISTS documents (
    id              UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    owner_id        UUID         NOT NULL,
    owner_type      VARCHAR(20)  NOT NULL,
    document_type   VARCHAR(50)  NOT NULL DEFAULT 'W9',
    original_name   TEXT         NOT NULL,
    stored_path     VARCHAR(500) NOT NULL,
    file_url        VARCHAR(500) NOT NULL,
    uploaded_at     TIMESTAMP    NOT NULL DEFAULT now()
);

-- 13. broker_validations table (BrokerValidation entity)
CREATE TABLE IF NOT EXISTS broker_validations (
    id                      UUID    PRIMARY KEY DEFAULT gen_random_uuid(),
    version                 BIGINT,
    mc_number               TEXT,
    dot_number              TEXT,
    legal_name              TEXT,
    operating_status        TEXT,
    broker_authority_active BOOLEAN,
    created_at              TIMESTAMP DEFAULT now()
);

-- 14. carrier_validations table (CarrierValidation entity) — guard for fresh DBs
CREATE TABLE IF NOT EXISTS carrier_validations (
    id                  UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    version             BIGINT,
    lookup_value        TEXT,
    lookup_type         VARCHAR(10),
    dot_number          TEXT,
    mc_number           TEXT,
    legal_name          TEXT,
    dba_name            TEXT,
    operating_status    TEXT,
    allowed_to_operate  TEXT,
    phy_street          TEXT,
    phy_city            TEXT,
    phy_state           TEXT,
    phy_zip             TEXT,
    phy_country         TEXT,
    total_drivers       INT,
    total_power_units   INT,
    created_at          TIMESTAMP DEFAULT now()
);

-- 15. audit_logs table (AuditLog entity)
CREATE TABLE IF NOT EXISTS audit_logs (
    id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID        REFERENCES users(id),
    action      TEXT,
    entity_type TEXT,
    entity_id   UUID,
    details     TEXT,
    created_at  TIMESTAMP NOT NULL DEFAULT now()
);

-- Fix bids FK: drop old constraint and re-add with ON DELETE CASCADE
--   so deleting a load automatically removes its bids at the DB level.
ALTER TABLE bids DROP CONSTRAINT IF EXISTS bids_load_id_fkey;
ALTER TABLE bids ADD CONSTRAINT bids_load_id_fkey
    FOREIGN KEY (load_id) REFERENCES loads(id) ON DELETE CASCADE;

-- 16. Add declined / declined_at columns to users (registration rejection tracking)
ALTER TABLE users
    ADD COLUMN IF NOT EXISTS declined BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE users
    ADD COLUMN IF NOT EXISTS declined_at TIMESTAMP;

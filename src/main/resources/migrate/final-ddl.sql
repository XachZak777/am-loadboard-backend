-- ============================================================================
-- final-ddl.sql — Complete database schema for Haulius LoadBoard
-- Target:          PostgreSQL 16
-- Hibernate mode:  ddl-auto=none  (validate on start via prod profile)
-- Source of truth: entity classes as of 2025-05-10
--
-- Usage (fresh database):
--   psql -h <host> -U <user> -d <dbname> -f final-ddl.sql
--
-- Tables (dependency order):
--   carriers → brokers → dealers → users → loads → bids
--   documents → broker_validations → carrier_validations
--   security_tokens → audit_logs
-- ============================================================================

-- Required for gen_random_uuid() on PostgreSQL < 13.
-- On PG 13+ it is a built-in core function; this is a no-op.
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- ── 1. carriers ──────────────────────────────────────────────────────────────
-- Maps to: Carrier.java
CREATE TABLE carriers (
    id                  UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    dot_number          TEXT         NOT NULL UNIQUE,
    mc_number           TEXT         UNIQUE,
    legal_name          TEXT,
    dba_name            TEXT,
    operating_status    TEXT,
    safety_rating       TEXT,
    verified            BOOLEAN      NOT NULL DEFAULT FALSE,
    subscription_active BOOLEAN               DEFAULT FALSE,
    phy_street          TEXT,
    phy_city            TEXT,
    phy_state           TEXT,
    phy_zip             TEXT,
    phy_country         TEXT,
    total_drivers       INT,
    total_power_units   INT,
    -- Registration wizard profile fields
    company_name        TEXT,
    phone_number        TEXT,
    mailing_address     TEXT,
    city                TEXT,
    state               TEXT,
    zip_code            TEXT,
    -- Insurance
    insurance_company   TEXT,
    cargo_insurance     NUMERIC(15,2),
    liability_insurance NUMERIC(15,2),
    -- Tax
    tax_id_type         VARCHAR(10),
    tax_id              VARCHAR(20),
    -- Preferred lanes (JSON array of {fromState, toState})
    preferred_lines     TEXT,
    created_at          TIMESTAMP    DEFAULT now()
);

-- ── 2. brokers ───────────────────────────────────────────────────────────────
-- Maps to: Broker.java
CREATE TABLE brokers (
    id                      UUID      PRIMARY KEY DEFAULT gen_random_uuid(),
    mc_number               TEXT      NOT NULL UNIQUE,
    dot_number              TEXT      UNIQUE,
    legal_name              TEXT,
    operating_status        TEXT,
    broker_authority_active BOOLEAN   NOT NULL DEFAULT FALSE,
    -- Registration wizard profile fields
    company_name            TEXT,
    phone_number            TEXT,
    mailing_address         TEXT,
    city                    TEXT,
    state                   TEXT,
    zip_code                TEXT,
    -- Bond information
    bond_company            TEXT,
    bond_policy_number      TEXT,
    bond_coverage           TEXT,
    bond_effective_date     TEXT,
    bond_agent_first_name   TEXT,
    bond_agent_last_name    TEXT,
    bond_agent_email        TEXT,
    bond_agent_phone        TEXT,
    -- Insurance
    insurance_company       TEXT,
    cargo_insurance         NUMERIC(15,2),
    liability_insurance     NUMERIC(15,2),
    -- Tax
    tax_id_type             VARCHAR(10),
    tax_id                  VARCHAR(20),
    created_at              TIMESTAMP DEFAULT now()
);

-- ── 3. dealers ───────────────────────────────────────────────────────────────
-- Maps to: Dealer.java
CREATE TABLE dealers (
    id                    UUID      PRIMARY KEY DEFAULT gen_random_uuid(),
    company_name          TEXT,
    owner_first_name      TEXT,
    owner_last_name       TEXT,
    business_phone        TEXT,
    company_address       TEXT,
    city                  TEXT,
    state                 TEXT,
    zip_code              TEXT,
    year_established      TEXT,
    dealer_license_number TEXT,
    auction_access_number TEXT,
    how_did_you_hear      TEXT,
    created_at            TIMESTAMP DEFAULT now()
);

-- ── 4. users ─────────────────────────────────────────────────────────────────
-- Maps to: User.java (extends Auditable)
-- Auditable fields: created_by, created_at, last_modified_by, last_modified_at
CREATE TABLE users (
    id                    UUID      PRIMARY KEY DEFAULT gen_random_uuid(),
    -- Role associations (at most one is non-null)
    carrier_id            UUID      REFERENCES carriers(id),
    broker_id             UUID      REFERENCES brokers(id),
    dealer_id             UUID      REFERENCES dealers(id),
    -- Identity
    role                  TEXT,
    email                 TEXT      NOT NULL UNIQUE,
    password_hash         TEXT      NOT NULL,
    -- Email verification
    email_verified        BOOLEAN   NOT NULL DEFAULT FALSE,
    email_verified_at     TIMESTAMP,
    -- Admin approval
    admin_approved        BOOLEAN   NOT NULL DEFAULT FALSE,
    admin_approved_at     TIMESTAMP,
    -- Registration rejection
    declined              BOOLEAN   NOT NULL DEFAULT FALSE,
    declined_at           TIMESTAMP,
    -- Account lockout
    login_disabled        BOOLEAN   NOT NULL DEFAULT FALSE,
    failed_login_attempts INT       NOT NULL DEFAULT 0,
    locked_until          TIMESTAMP,
    -- Auditable
    created_by            TEXT,
    created_at            TIMESTAMP,
    last_modified_by      TEXT,
    last_modified_at      TIMESTAMP
);

-- ── 5. loads ─────────────────────────────────────────────────────────────────
-- Maps to: LoadPosting.java
-- Embedded: LoadAddress (pickup + drop), VehicleInfo
CREATE TABLE loads (
    id                      UUID             PRIMARY KEY DEFAULT gen_random_uuid(),
    -- Posting owner (exactly one of broker_id or dealer_id is non-null)
    broker_id               UUID             REFERENCES brokers(id),
    dealer_id               UUID             REFERENCES dealers(id),
    assigned_carrier_id     UUID             REFERENCES carriers(id),
    -- Status: OPEN | ASSIGNED | PICKED_UP | DELIVERED | PAID | CANCELLED | COMPLETED
    status                  TEXT             NOT NULL DEFAULT 'OPEN'
                                             CHECK (status IN ('OPEN','ASSIGNED','PICKED_UP','DELIVERED','PAID','CANCELLED','COMPLETED')),
    -- Pickup location (LoadAddress embedded)
    pickup_type             TEXT,
    pickup_street           TEXT,
    pickup_city             TEXT,
    pickup_state            TEXT,
    pickup_zip              TEXT,
    pickup_country          TEXT,
    pickup_lot_number       TEXT,
    pickup_contact_name     TEXT,
    pickup_contact_phone    TEXT,
    -- Delivery location (LoadAddress embedded)
    drop_type               TEXT,
    drop_street             TEXT,
    drop_city               TEXT,
    drop_state              TEXT,
    drop_zip                TEXT,
    drop_country            TEXT,
    drop_lot_number         TEXT,
    drop_contact_name       TEXT,
    drop_contact_phone      TEXT,
    -- Vehicle info (VehicleInfo embedded)
    vehicle_make            TEXT,
    vehicle_model           TEXT,
    vehicle_year            INT,
    vehicle_type            TEXT,
    vehicle_condition       TEXT,
    vehicle_vin             TEXT,
    trailer_type            TEXT,
    vehicle_additional_info TEXT,
    -- Scheduling
    pickup_date             DATE,
    delivery_date           DATE,
    -- Pricing / logistics
    price                   DOUBLE PRECISION,
    weight                  DOUBLE PRECISION,
    distance                DOUBLE PRECISION,
    payment_method          TEXT,
    payment_timing          TEXT,
    -- Load-level contact (public-facing broker/dealer contact)
    contact_name            TEXT,
    contact_phone           TEXT,
    contact_email           TEXT,
    order_id                TEXT,
    description             TEXT,
    created_at              TIMESTAMP        DEFAULT now()
);

-- ── 6. bids ──────────────────────────────────────────────────────────────────
-- Maps to: Bid.java
-- Status: PENDING | APPROVED | REJECTED | CANCELLED
CREATE TABLE bids (
    id          UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    load_id     UUID         NOT NULL REFERENCES loads(id) ON DELETE CASCADE,
    carrier_id  UUID         NOT NULL REFERENCES carriers(id),
    amount      NUMERIC(19,2),
    book_now    BOOLEAN      NOT NULL DEFAULT FALSE,
    status      TEXT         NOT NULL DEFAULT 'PENDING',
    created_at  TIMESTAMP,
    updated_at  TIMESTAMP
);

-- ── 7. documents ─────────────────────────────────────────────────────────────
-- Maps to: Document.java
-- File content stored as BYTEA (no GridFS / S3 dependency)
CREATE TABLE documents (
    id            UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    owner_id      UUID         NOT NULL,
    owner_type    VARCHAR(20)  NOT NULL,
    document_type VARCHAR(50)  NOT NULL DEFAULT 'W9',
    original_name TEXT         NOT NULL,
    stored_path   VARCHAR(500) NOT NULL,
    file_content  BYTEA        NOT NULL,
    content_type  VARCHAR(100) NOT NULL,
    file_url      VARCHAR(500) NOT NULL,
    uploaded_at   TIMESTAMP    NOT NULL DEFAULT now()
);

-- ── 8. broker_validations ────────────────────────────────────────────────────
-- Maps to: BrokerValidation.java
-- Cache table for FMCSA broker lookup results
CREATE TABLE broker_validations (
    id                       UUID      PRIMARY KEY DEFAULT gen_random_uuid(),
    version                  BIGINT    NOT NULL DEFAULT 0,
    -- Identity
    mc_number                TEXT,
    dot_number               TEXT,
    legal_name               TEXT,
    dba_name                 TEXT,
    entity_type              TEXT,
    -- Status
    operating_status         TEXT,
    allowed_to_operate       TEXT,
    out_of_service_date      TEXT,
    latest_update            TEXT,
    -- Physical address
    phy_street               TEXT,
    phy_city                 TEXT,
    phy_state                TEXT,
    phy_zip                  TEXT,
    phy_country              TEXT,
    -- Mailing address
    mailing_street           TEXT,
    mailing_city             TEXT,
    mailing_state            TEXT,
    mailing_zip              TEXT,
    mailing_country          TEXT,
    -- Contact / fleet
    phone                    TEXT,
    total_drivers            INT,
    total_power_units        INT,
    -- Operation (JSON arrays)
    operation_classification JSONB,
    carrier_operation        JSONB,
    cargo_carried            JSONB,
    -- MCS-150
    mcs150_date              TEXT,
    mcs150_mileage           INT,
    mcs150_year              INT,
    -- Safety
    safety_rating            TEXT,
    safety_rating_date       TEXT,
    safety_review_date       TEXT,
    safety_type              TEXT,
    -- Broker-specific
    broker_authority_active  BOOLEAN   NOT NULL DEFAULT FALSE,
    created_at               TIMESTAMP DEFAULT now()
);

-- ── 9. carrier_validations ───────────────────────────────────────────────────
-- Maps to: CarrierValidation.java
-- Cache table for FMCSA carrier lookup results
CREATE TABLE carrier_validations (
    id                       UUID      PRIMARY KEY DEFAULT gen_random_uuid(),
    version                  BIGINT    NOT NULL DEFAULT 0,
    -- Lookup key
    lookup_value             TEXT,
    lookup_type              TEXT,
    -- Identity
    dot_number               TEXT,
    mc_number                TEXT,
    legal_name               TEXT,
    dba_name                 TEXT,
    entity_type              TEXT,
    -- Status
    operating_status         TEXT,
    allowed_to_operate       TEXT,
    out_of_service_date      TEXT,
    latest_update            TEXT,
    -- Physical address
    phy_street               TEXT,
    phy_city                 TEXT,
    phy_state                TEXT,
    phy_zip                  TEXT,
    phy_country              TEXT,
    -- Mailing address
    mailing_street           TEXT,
    mailing_city             TEXT,
    mailing_state            TEXT,
    mailing_zip              TEXT,
    mailing_country          TEXT,
    -- Contact / fleet
    phone                    TEXT,
    total_drivers            INT,
    total_power_units        INT,
    -- Operation (JSON arrays)
    operation_classification JSONB,
    carrier_operation        JSONB,
    cargo_carried            JSONB,
    -- MCS-150
    mcs150_date              TEXT,
    mcs150_mileage           INT,
    mcs150_year              INT,
    -- Safety
    safety_rating            TEXT,
    safety_rating_date       TEXT,
    safety_review_date       TEXT,
    safety_type              TEXT,
    created_at               TIMESTAMP DEFAULT now()
);

-- ── 10. security_tokens ──────────────────────────────────────────────────────
-- Maps to: SecurityToken.java (extends Auditable)
-- Token types: EMAIL_VERIFICATION | PASSWORD_RESET | EMAIL_CHANGE
CREATE TABLE security_tokens (
    id               UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    token            TEXT        NOT NULL UNIQUE,
    token_type       VARCHAR(40) NOT NULL,
    user_id          UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    expires_at       TIMESTAMP   NOT NULL,
    used             BOOLEAN     NOT NULL DEFAULT FALSE,
    used_at          TIMESTAMP,
    -- Auditable
    created_by       TEXT,
    created_at       TIMESTAMP,
    last_modified_by TEXT,
    last_modified_at TIMESTAMP
);

-- ── 11. audit_logs ───────────────────────────────────────────────────────────
-- Maps to: AuditLog.java
CREATE TABLE audit_logs (
    id          UUID      PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID      REFERENCES users(id),
    action      TEXT,
    entity_type TEXT,
    entity_id   UUID,
    details     TEXT,
    created_at  TIMESTAMP NOT NULL DEFAULT now()
);

-- ============================================================================
-- Indexes
-- ============================================================================

-- users
CREATE INDEX idx_users_role       ON users(role);
CREATE INDEX idx_users_carrier_id ON users(carrier_id);
CREATE INDEX idx_users_broker_id  ON users(broker_id);
CREATE INDEX idx_users_dealer_id  ON users(dealer_id);

-- loads (most-queried table — filter by status and owner)
CREATE INDEX idx_loads_status     ON loads(status);
CREATE INDEX idx_loads_broker_id  ON loads(broker_id);
CREATE INDEX idx_loads_dealer_id  ON loads(dealer_id);
CREATE INDEX idx_loads_created_at ON loads(created_at DESC);

-- bids
CREATE INDEX idx_bids_load_id     ON bids(load_id);
CREATE INDEX idx_bids_carrier_id  ON bids(carrier_id);
CREATE INDEX idx_bids_status      ON bids(status);

-- documents
CREATE INDEX idx_docs_owner       ON documents(owner_id);
CREATE INDEX idx_docs_owner_type  ON documents(owner_id, document_type);

-- security_tokens (hit on every auth check)
CREATE INDEX idx_tokens_user_id   ON security_tokens(user_id);
CREATE INDEX idx_tokens_type_used ON security_tokens(token_type, used);

-- audit_logs
CREATE INDEX idx_audit_user_id    ON audit_logs(user_id);
CREATE INDEX idx_audit_entity     ON audit_logs(entity_type, entity_id);

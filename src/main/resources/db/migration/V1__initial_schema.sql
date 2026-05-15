-- ============================================================================
-- V1__initial_schema.sql — Complete database schema for Haulius LoadBoard
-- Target:  PostgreSQL 16
-- Managed: Flyway (replaces manual final-ddl.sql)
-- ============================================================================

CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- ── 1. carriers ──────────────────────────────────────────────────────────────
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
    company_name        TEXT,
    phone_number        TEXT,
    mailing_address     TEXT,
    city                TEXT,
    state               TEXT,
    zip_code            TEXT,
    insurance_company   TEXT,
    cargo_insurance     NUMERIC(15,2),
    liability_insurance NUMERIC(15,2),
    tax_id_type         VARCHAR(10),
    tax_id              VARCHAR(20),
    preferred_lines     TEXT,
    created_at          TIMESTAMP    DEFAULT now()
);

-- ── 2. brokers ───────────────────────────────────────────────────────────────
CREATE TABLE brokers (
    id                      UUID      PRIMARY KEY DEFAULT gen_random_uuid(),
    mc_number               TEXT      NOT NULL UNIQUE,
    dot_number              TEXT      UNIQUE,
    legal_name              TEXT,
    operating_status        TEXT,
    broker_authority_active BOOLEAN   NOT NULL DEFAULT FALSE,
    company_name            TEXT,
    phone_number            TEXT,
    mailing_address         TEXT,
    city                    TEXT,
    state                   TEXT,
    zip_code                TEXT,
    bond_company            TEXT,
    bond_policy_number      TEXT,
    bond_coverage           TEXT,
    bond_effective_date     TEXT,
    bond_agent_first_name   TEXT,
    bond_agent_last_name    TEXT,
    bond_agent_email        TEXT,
    bond_agent_phone        TEXT,
    insurance_company       TEXT,
    cargo_insurance         NUMERIC(15,2),
    liability_insurance     NUMERIC(15,2),
    tax_id_type             VARCHAR(10),
    tax_id                  VARCHAR(20),
    created_at              TIMESTAMP DEFAULT now()
);

-- ── 3. dealers ───────────────────────────────────────────────────────────────
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
CREATE TABLE users (
    id                    UUID      PRIMARY KEY DEFAULT gen_random_uuid(),
    carrier_id            UUID      REFERENCES carriers(id),
    broker_id             UUID      REFERENCES brokers(id),
    dealer_id             UUID      REFERENCES dealers(id),
    role                  TEXT,
    email                 TEXT      NOT NULL UNIQUE,
    password_hash         TEXT      NOT NULL,
    email_verified        BOOLEAN   NOT NULL DEFAULT FALSE,
    email_verified_at     TIMESTAMP,
    admin_approved        BOOLEAN   NOT NULL DEFAULT FALSE,
    admin_approved_at     TIMESTAMP,
    declined              BOOLEAN   NOT NULL DEFAULT FALSE,
    declined_at           TIMESTAMP,
    login_disabled        BOOLEAN   NOT NULL DEFAULT FALSE,
    failed_login_attempts INT       NOT NULL DEFAULT 0,
    locked_until          TIMESTAMP,
    created_by            TEXT,
    created_at            TIMESTAMP,
    last_modified_by      TEXT,
    last_modified_at      TIMESTAMP
);

-- ── 5. loads ─────────────────────────────────────────────────────────────────
CREATE TABLE loads (
    id                      UUID             PRIMARY KEY DEFAULT gen_random_uuid(),
    broker_id               UUID             REFERENCES brokers(id),
    dealer_id               UUID             REFERENCES dealers(id),
    assigned_carrier_id     UUID             REFERENCES carriers(id),
    status                  TEXT             NOT NULL DEFAULT 'OPEN'
                                             CHECK (status IN ('OPEN','ASSIGNED','PICKED_UP','DELIVERED','PAID','CANCELLED','COMPLETED')),
    pickup_type             TEXT,
    pickup_street           TEXT,
    pickup_city             TEXT,
    pickup_state            TEXT,
    pickup_zip              TEXT,
    pickup_country          TEXT,
    pickup_lot_number       TEXT,
    pickup_contact_name     TEXT,
    pickup_contact_phone    TEXT,
    drop_type               TEXT,
    drop_street             TEXT,
    drop_city               TEXT,
    drop_state              TEXT,
    drop_zip                TEXT,
    drop_country            TEXT,
    drop_lot_number         TEXT,
    drop_contact_name       TEXT,
    drop_contact_phone      TEXT,
    vehicle_make            TEXT,
    vehicle_model           TEXT,
    vehicle_year            INT,
    vehicle_type            TEXT,
    vehicle_condition       TEXT,
    vehicle_vin             TEXT,
    trailer_type            TEXT,
    vehicle_additional_info TEXT,
    pickup_date             DATE,
    delivery_date           DATE,
    price                   DOUBLE PRECISION,
    weight                  DOUBLE PRECISION,
    distance                DOUBLE PRECISION,
    payment_method          TEXT,
    payment_timing          TEXT,
    contact_name            TEXT,
    contact_phone           TEXT,
    contact_email           TEXT,
    order_id                TEXT,
    description             TEXT,
    created_at              TIMESTAMP        DEFAULT now()
);

-- ── 6. bids ──────────────────────────────────────────────────────────────────
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
CREATE TABLE broker_validations (
    id                       UUID      PRIMARY KEY DEFAULT gen_random_uuid(),
    version                  BIGINT    NOT NULL DEFAULT 0,
    mc_number                TEXT,
    dot_number               TEXT,
    legal_name               TEXT,
    dba_name                 TEXT,
    entity_type              TEXT,
    operating_status         TEXT,
    allowed_to_operate       TEXT,
    out_of_service_date      TEXT,
    latest_update            TEXT,
    phy_street               TEXT,
    phy_city                 TEXT,
    phy_state                TEXT,
    phy_zip                  TEXT,
    phy_country              TEXT,
    mailing_street           TEXT,
    mailing_city             TEXT,
    mailing_state            TEXT,
    mailing_zip              TEXT,
    mailing_country          TEXT,
    phone                    TEXT,
    total_drivers            INT,
    total_power_units        INT,
    operation_classification JSONB,
    carrier_operation        JSONB,
    cargo_carried            JSONB,
    mcs150_date              TEXT,
    mcs150_mileage           INT,
    mcs150_year              INT,
    safety_rating            TEXT,
    safety_rating_date       TEXT,
    safety_review_date       TEXT,
    safety_type              TEXT,
    broker_authority_active  BOOLEAN   NOT NULL DEFAULT FALSE,
    created_at               TIMESTAMP DEFAULT now()
);

-- ── 9. carrier_validations ───────────────────────────────────────────────────
CREATE TABLE carrier_validations (
    id                       UUID      PRIMARY KEY DEFAULT gen_random_uuid(),
    version                  BIGINT    NOT NULL DEFAULT 0,
    lookup_value             TEXT,
    lookup_type              TEXT,
    dot_number               TEXT,
    mc_number                TEXT,
    legal_name               TEXT,
    dba_name                 TEXT,
    entity_type              TEXT,
    operating_status         TEXT,
    allowed_to_operate       TEXT,
    out_of_service_date      TEXT,
    latest_update            TEXT,
    phy_street               TEXT,
    phy_city                 TEXT,
    phy_state                TEXT,
    phy_zip                  TEXT,
    phy_country              TEXT,
    mailing_street           TEXT,
    mailing_city             TEXT,
    mailing_state            TEXT,
    mailing_zip              TEXT,
    mailing_country          TEXT,
    phone                    TEXT,
    total_drivers            INT,
    total_power_units        INT,
    operation_classification JSONB,
    carrier_operation        JSONB,
    cargo_carried            JSONB,
    mcs150_date              TEXT,
    mcs150_mileage           INT,
    mcs150_year              INT,
    safety_rating            TEXT,
    safety_rating_date       TEXT,
    safety_review_date       TEXT,
    safety_type              TEXT,
    created_at               TIMESTAMP DEFAULT now()
);

-- ── 10. security_tokens ──────────────────────────────────────────────────────
CREATE TABLE security_tokens (
    id               UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    token            TEXT        NOT NULL UNIQUE,
    token_type       VARCHAR(40) NOT NULL,
    user_id          UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    expires_at       TIMESTAMP   NOT NULL,
    used             BOOLEAN     NOT NULL DEFAULT FALSE,
    used_at          TIMESTAMP,
    created_by       TEXT,
    created_at       TIMESTAMP,
    last_modified_by TEXT,
    last_modified_at TIMESTAMP
);

-- ── 11. audit_logs ───────────────────────────────────────────────────────────
CREATE TABLE audit_logs (
    id          UUID      PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID      REFERENCES users(id),
    action      TEXT,
    entity_type TEXT,
    entity_id   UUID,
    details     TEXT,
    created_at  TIMESTAMP NOT NULL DEFAULT now()
);

-- ── 12. ratings ──────────────────────────────────────────────────────────────
CREATE TABLE ratings (
    id           UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    target_id    UUID        NOT NULL,
    target_type  VARCHAR(20) NOT NULL,
    load_id      UUID        NOT NULL REFERENCES loads(id),
    submitter_id UUID        NOT NULL REFERENCES users(id),
    type         VARCHAR(20) NOT NULL,
    tags         TEXT,
    comment      VARCHAR(500),
    created_at   TIMESTAMP   NOT NULL DEFAULT now()
);

-- ============================================================================
-- Indexes
-- ============================================================================

CREATE INDEX idx_users_role       ON users(role);
CREATE INDEX idx_users_carrier_id ON users(carrier_id);
CREATE INDEX idx_users_broker_id  ON users(broker_id);
CREATE INDEX idx_users_dealer_id  ON users(dealer_id);

CREATE INDEX idx_loads_status     ON loads(status);
CREATE INDEX idx_loads_broker_id  ON loads(broker_id);
CREATE INDEX idx_loads_dealer_id  ON loads(dealer_id);
CREATE INDEX idx_loads_created_at ON loads(created_at DESC);

CREATE INDEX idx_bids_load_id     ON bids(load_id);
CREATE INDEX idx_bids_carrier_id  ON bids(carrier_id);
CREATE INDEX idx_bids_status      ON bids(status);

CREATE INDEX idx_docs_owner       ON documents(owner_id);
CREATE INDEX idx_docs_owner_type  ON documents(owner_id, document_type);

CREATE INDEX idx_tokens_user_id   ON security_tokens(user_id);
CREATE INDEX idx_tokens_type_used ON security_tokens(token_type, used);

CREATE INDEX idx_audit_user_id    ON audit_logs(user_id);
CREATE INDEX idx_audit_entity     ON audit_logs(entity_type, entity_id);

CREATE INDEX idx_ratings_target   ON ratings(target_id, target_type);
CREATE INDEX idx_ratings_load_id  ON ratings(load_id);

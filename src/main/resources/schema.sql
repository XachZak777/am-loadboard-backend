-- ============================================================================
-- Haulius LoadBoard — full schema
-- Run once against a fresh PostgreSQL database.
-- ============================================================================

-- ── 1. carriers ──────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS carriers (
    id                  UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    dot_number          VARCHAR     UNIQUE NOT NULL,
    mc_number           VARCHAR     UNIQUE,
    legal_name          VARCHAR,
    dba_name            VARCHAR,
    operating_status    VARCHAR,
    safety_rating       VARCHAR,
    verified            BOOLEAN     DEFAULT FALSE,
    subscription_active BOOLEAN     DEFAULT FALSE,
    phy_street          VARCHAR,
    phy_city            VARCHAR,
    phy_state           VARCHAR,
    phy_zip             VARCHAR,
    phy_country         VARCHAR,
    total_drivers       INT,
    total_power_units   INT,
    company_name        VARCHAR,
    phone_number        VARCHAR,
    mailing_address     VARCHAR,
    city                VARCHAR,
    state               VARCHAR,
    zip_code            VARCHAR,
    insurance_company   VARCHAR,
    cargo_insurance     NUMERIC(15,2),
    liability_insurance NUMERIC(15,2),
    tax_id_type         VARCHAR(10),
    tax_id              VARCHAR(20),
    preferred_lines     TEXT,
    created_at          TIMESTAMP
);

-- ── 2. brokers ───────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS brokers (
    id                      UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    mc_number               VARCHAR     UNIQUE NOT NULL,
    dot_number              VARCHAR     UNIQUE,
    legal_name              VARCHAR,
    operating_status        VARCHAR,
    broker_authority_active BOOLEAN     DEFAULT FALSE,
    company_name            VARCHAR,
    phone_number            VARCHAR,
    mailing_address         VARCHAR,
    city                    VARCHAR,
    state                   VARCHAR,
    zip_code                VARCHAR,
    insurance_company       VARCHAR,
    cargo_insurance         NUMERIC(15,2),
    liability_insurance     NUMERIC(15,2),
    tax_id_type             VARCHAR(10),
    tax_id                  VARCHAR(20),
    bond_company            VARCHAR,
    bond_policy_number      VARCHAR,
    bond_coverage           VARCHAR,
    bond_effective_date     VARCHAR,
    bond_agent_first_name   VARCHAR,
    bond_agent_last_name    VARCHAR,
    bond_agent_email        VARCHAR,
    bond_agent_phone        VARCHAR,
    created_at              TIMESTAMP
);

-- ── 3. dealers ───────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS dealers (
    id                    UUID    PRIMARY KEY DEFAULT gen_random_uuid(),
    company_name          VARCHAR,
    owner_first_name      VARCHAR,
    owner_last_name       VARCHAR,
    business_phone        VARCHAR,
    company_address       VARCHAR,
    city                  VARCHAR,
    state                 VARCHAR,
    zip_code              VARCHAR,
    year_established      VARCHAR,
    dealer_license_number VARCHAR,
    auction_access_number VARCHAR,
    how_did_you_hear      VARCHAR,
    created_at            TIMESTAMP
);

-- ── 4. users ─────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS users (
    id                    UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    carrier_id            UUID        REFERENCES carriers(id),
    broker_id             UUID        REFERENCES brokers(id),
    dealer_id             UUID        REFERENCES dealers(id),
    role                  VARCHAR     NOT NULL,
    email                 VARCHAR     UNIQUE NOT NULL,
    password_hash         VARCHAR     NOT NULL,
    email_verified        BOOLEAN     NOT NULL DEFAULT FALSE,
    email_verified_at     TIMESTAMP,
    admin_approved        BOOLEAN     NOT NULL DEFAULT FALSE,
    admin_approved_at     TIMESTAMP,
    declined              BOOLEAN     NOT NULL DEFAULT FALSE,
    declined_at           TIMESTAMP,
    login_disabled        BOOLEAN     NOT NULL DEFAULT FALSE,
    failed_login_attempts INT         NOT NULL DEFAULT 0,
    locked_until          TIMESTAMP,
    created_by            VARCHAR,
    created_at            TIMESTAMP,
    last_modified_by      VARCHAR,
    last_modified_at      TIMESTAMP
);

-- ── 5. loads ─────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS loads (
    id                      UUID    PRIMARY KEY DEFAULT gen_random_uuid(),
    broker_id               UUID    REFERENCES brokers(id),
    dealer_id               UUID    REFERENCES dealers(id),
    assigned_carrier_id     UUID    REFERENCES carriers(id),
    status                  VARCHAR NOT NULL DEFAULT 'OPEN',
    pickup_type             VARCHAR,
    drop_type               VARCHAR,
    pickup_street           VARCHAR,
    pickup_city             VARCHAR,
    pickup_state            VARCHAR,
    pickup_zip              VARCHAR,
    pickup_country          VARCHAR,
    pickup_lot_number       VARCHAR,
    pickup_contact_name     VARCHAR,
    pickup_contact_phone    VARCHAR,
    drop_street             VARCHAR,
    drop_city               VARCHAR,
    drop_state              VARCHAR,
    drop_zip                VARCHAR,
    drop_country            VARCHAR,
    drop_lot_number         VARCHAR,
    drop_contact_name       VARCHAR,
    drop_contact_phone      VARCHAR,
    vehicle_make            VARCHAR,
    vehicle_model           VARCHAR,
    vehicle_year            INT,
    vehicle_type            VARCHAR,
    vehicle_condition       VARCHAR,
    vehicle_vin             VARCHAR,
    trailer_type            VARCHAR,
    vehicle_additional_info VARCHAR,
    description             TEXT,
    weight                  DOUBLE PRECISION,
    price                   DOUBLE PRECISION,
    distance                DOUBLE PRECISION,
    pickup_date             DATE,
    pickup_time             VARCHAR,
    delivery_date           DATE,
    delivery_time           VARCHAR,
    contact_name            VARCHAR,
    contact_phone           VARCHAR,
    contact_email           VARCHAR,
    order_id                VARCHAR,
    payment_method          VARCHAR,
    payment_timing          VARCHAR,
    additional_vehicles     TEXT,
    created_at              TIMESTAMP
);

-- ── 6. bids ──────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS bids (
    id                    UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    load_id               UUID        NOT NULL REFERENCES loads(id),
    carrier_id            UUID        NOT NULL REFERENCES carriers(id),
    amount                NUMERIC,
    book_now              BOOLEAN     DEFAULT FALSE,
    requested_pickup_date DATE,
    requested_pickup_time VARCHAR,
    requested_drop_date   DATE,
    requested_drop_time   VARCHAR,
    status                VARCHAR     NOT NULL DEFAULT 'PENDING',
    created_at            TIMESTAMP,
    updated_at            TIMESTAMP
);

-- ── 7. documents ─────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS documents (
    id            UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    owner_id      UUID        NOT NULL,
    owner_type    VARCHAR(20) NOT NULL,
    document_type VARCHAR(50) NOT NULL DEFAULT 'W9',
    original_name VARCHAR     NOT NULL,
    stored_path   VARCHAR(500) NOT NULL,
    file_content  BYTEA       NOT NULL,
    content_type  VARCHAR(100) NOT NULL,
    file_url      VARCHAR(500) NOT NULL,
    uploaded_at   TIMESTAMP   NOT NULL DEFAULT now()
);

-- ── 8. security_tokens ───────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS security_tokens (
    id               UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    token            VARCHAR     UNIQUE NOT NULL,
    token_type       VARCHAR(40) NOT NULL,
    user_id          UUID        NOT NULL REFERENCES users(id),
    expires_at       TIMESTAMP   NOT NULL,
    used_at          TIMESTAMP,
    used             BOOLEAN     NOT NULL DEFAULT FALSE,
    created_by       VARCHAR,
    created_at       TIMESTAMP,
    last_modified_by VARCHAR,
    last_modified_at TIMESTAMP
);

-- ── 9. audit_logs ────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS audit_logs (
    id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID        REFERENCES users(id) ON DELETE SET NULL,
    action      VARCHAR(100),
    entity_type VARCHAR(100),
    entity_id   UUID,
    details     TEXT,
    created_at  TIMESTAMP   NOT NULL DEFAULT now()
);

-- ── 10. ratings ──────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS ratings (
    id           UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    target_id    UUID        NOT NULL,
    target_type  VARCHAR(20) NOT NULL,
    load_id      UUID        NOT NULL,
    submitter_id UUID        NOT NULL,
    type         VARCHAR(20) NOT NULL,
    tags         TEXT,
    comment      VARCHAR(500),
    created_at   TIMESTAMP   NOT NULL DEFAULT now()
);

-- ── 11. carrier_validations ──────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS carrier_validations (
    id                       UUID    PRIMARY KEY DEFAULT gen_random_uuid(),
    version                  BIGINT,
    lookup_value             VARCHAR,
    lookup_type              VARCHAR,
    dot_number               VARCHAR,
    mc_number                VARCHAR,
    legal_name               VARCHAR,
    dba_name                 VARCHAR,
    entity_type              VARCHAR,
    operating_status         VARCHAR,
    allowed_to_operate       VARCHAR,
    out_of_service_date      VARCHAR,
    latest_update            VARCHAR,
    phy_street               VARCHAR,
    phy_city                 VARCHAR,
    phy_state                VARCHAR,
    phy_zip                  VARCHAR,
    phy_country              VARCHAR,
    mailing_street           VARCHAR,
    mailing_city             VARCHAR,
    mailing_state            VARCHAR,
    mailing_zip              VARCHAR,
    mailing_country          VARCHAR,
    phone                    VARCHAR,
    total_drivers            INT,
    total_power_units        INT,
    operation_classification JSONB,
    carrier_operation        JSONB,
    cargo_carried            JSONB,
    mcs150_date              VARCHAR,
    mcs150_mileage           INT,
    mcs150_year              INT,
    safety_rating            VARCHAR,
    safety_rating_date       VARCHAR,
    safety_review_date       VARCHAR,
    safety_type              VARCHAR,
    created_at               TIMESTAMP
);

-- ── 12. broker_validations ───────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS broker_validations (
    id                       UUID    PRIMARY KEY DEFAULT gen_random_uuid(),
    version                  BIGINT,
    mc_number                VARCHAR,
    dot_number               VARCHAR,
    legal_name               VARCHAR,
    dba_name                 VARCHAR,
    entity_type              VARCHAR,
    operating_status         VARCHAR,
    allowed_to_operate       VARCHAR,
    out_of_service_date      VARCHAR,
    latest_update            VARCHAR,
    phy_street               VARCHAR,
    phy_city                 VARCHAR,
    phy_state                VARCHAR,
    phy_zip                  VARCHAR,
    phy_country              VARCHAR,
    mailing_street           VARCHAR,
    mailing_city             VARCHAR,
    mailing_state            VARCHAR,
    mailing_zip              VARCHAR,
    mailing_country          VARCHAR,
    phone                    VARCHAR,
    total_drivers            INT,
    total_power_units        INT,
    operation_classification JSONB,
    carrier_operation        JSONB,
    cargo_carried            JSONB,
    mcs150_date              VARCHAR,
    mcs150_mileage           INT,
    mcs150_year              INT,
    safety_rating            VARCHAR,
    safety_rating_date       VARCHAR,
    safety_review_date       VARCHAR,
    safety_type              VARCHAR,
    broker_authority_active  BOOLEAN DEFAULT FALSE,
    created_at               TIMESTAMP
);

-- ── Indexes ───────────────────────────────────────────────────────────────────
CREATE INDEX IF NOT EXISTS idx_users_role        ON users(role);
CREATE INDEX IF NOT EXISTS idx_users_carrier_id  ON users(carrier_id);
CREATE INDEX IF NOT EXISTS idx_users_broker_id   ON users(broker_id);
CREATE INDEX IF NOT EXISTS idx_users_dealer_id   ON users(dealer_id);

CREATE INDEX IF NOT EXISTS idx_loads_status      ON loads(status);
CREATE INDEX IF NOT EXISTS idx_loads_broker_id   ON loads(broker_id);
CREATE INDEX IF NOT EXISTS idx_loads_dealer_id   ON loads(dealer_id);
CREATE INDEX IF NOT EXISTS idx_loads_created_at  ON loads(created_at DESC);

CREATE INDEX IF NOT EXISTS idx_bids_load_id      ON bids(load_id);
CREATE INDEX IF NOT EXISTS idx_bids_carrier_id   ON bids(carrier_id);
CREATE INDEX IF NOT EXISTS idx_bids_status       ON bids(status);

CREATE INDEX IF NOT EXISTS idx_docs_owner        ON documents(owner_id);
CREATE INDEX IF NOT EXISTS idx_docs_owner_type   ON documents(owner_id, document_type);

CREATE INDEX IF NOT EXISTS idx_tokens_user_id    ON security_tokens(user_id);
CREATE INDEX IF NOT EXISTS idx_tokens_type_used  ON security_tokens(token_type, used);

CREATE INDEX IF NOT EXISTS idx_audit_user_id     ON audit_logs(user_id);
CREATE INDEX IF NOT EXISTS idx_audit_entity      ON audit_logs(entity_type, entity_id);

CREATE INDEX IF NOT EXISTS idx_ratings_target    ON ratings(target_id, target_type);
CREATE INDEX IF NOT EXISTS idx_ratings_load_id   ON ratings(load_id);

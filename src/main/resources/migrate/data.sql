CREATE TABLE carriers (
        id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

        dot_number TEXT UNIQUE,
        mc_number TEXT,

        legal_name TEXT NOT NULL,
        dba_name TEXT,

        allowed_to_operate BOOLEAN,
        status_code CHAR(1),

        phy_street TEXT,
        phy_city TEXT,
        phy_state TEXT,
        phy_zip TEXT,
        phy_country TEXT,

        total_drivers INT,
        total_power_units INT,

        created_at TIMESTAMP DEFAULT now()
);

CREATE TABLE brokers (
        id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

        mc_number TEXT UNIQUE NOT NULL,
        dot_number TEXT UNIQUE,

        legal_name TEXT,
        operating_status TEXT,

        broker_authority_active BOOLEAN,

        created_at TIMESTAMP DEFAULT now()
);

CREATE TABLE users (
        id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
        carrier_id UUID REFERENCES carriers(id),
        broker_id UUID REFERENCES brokers(id),

        email TEXT UNIQUE NOT NULL,
        password_hash TEXT NOT NULL,

        created_at TIMESTAMP DEFAULT now()
);

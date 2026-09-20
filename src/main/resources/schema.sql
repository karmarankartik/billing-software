
CREATE TABLE IF NOT EXISTS users (
    id UUID DEFAULT RANDOM_UUID() PRIMARY KEY,
    username VARCHAR(100) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    deleted_at TIMESTAMP WITH TIME ZONE,

    CONSTRAINT uk_users_username
        UNIQUE (username),

    CONSTRAINT ck_users_role
        CHECK (role IN ('ADMIN', 'CUSTOMER'))
);

CREATE INDEX IF NOT EXISTS idx_users_role
    ON users(role);


CREATE TABLE IF NOT EXISTS user_sessions (
    id UUID DEFAULT RANDOM_UUID() PRIMARY KEY,
    user_id UUID NOT NULL,
    token VARCHAR(255) NOT NULL UNIQUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    revoked_at TIMESTAMP WITH TIME ZONE,

    CONSTRAINT fk_user_sessions_user
        FOREIGN KEY (user_id)
        REFERENCES users(id)
);

CREATE INDEX IF NOT EXISTS idx_user_sessions_token
    ON user_sessions(token);

CREATE INDEX IF NOT EXISTS idx_user_sessions_user_id
    ON user_sessions(user_id);


CREATE TABLE IF NOT EXISTS billing_plans (
    id UUID DEFAULT RANDOM_UUID() PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    code VARCHAR(50) NOT NULL,
    description VARCHAR(500),
    price_per_unit DECIMAL(19, 4) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,

    created_by UUID NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_by UUID NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    deleted_at TIMESTAMP WITH TIME ZONE,

    CONSTRAINT uk_billing_plans_code
        UNIQUE (code),

    CONSTRAINT ck_billing_plans_price
        CHECK (price_per_unit >= 0),

    CONSTRAINT fk_billing_plans_created_by
        FOREIGN KEY (created_by)
        REFERENCES users(id),

    CONSTRAINT fk_billing_plans_updated_by
        FOREIGN KEY (updated_by)
        REFERENCES users(id)
);

CREATE INDEX IF NOT EXISTS idx_billing_plans_created_by
    ON billing_plans(created_by);

CREATE INDEX IF NOT EXISTS idx_billing_plans_updated_by
    ON billing_plans(updated_by);

CREATE INDEX IF NOT EXISTS idx_billing_plans_active
    ON billing_plans(active);

CREATE INDEX IF NOT EXISTS idx_billing_plans_deleted_at
    ON billing_plans(deleted_at);


CREATE TABLE IF NOT EXISTS water_meters (
    id UUID DEFAULT RANDOM_UUID() PRIMARY KEY,
    meter_number VARCHAR(100) NOT NULL,

    created_by UUID NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_by UUID NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    deleted_at TIMESTAMP WITH TIME ZONE,

    CONSTRAINT uk_water_meters_meter_number
        UNIQUE (meter_number),

    CONSTRAINT fk_water_meters_created_by
        FOREIGN KEY (created_by)
        REFERENCES users(id),

    CONSTRAINT fk_water_meters_updated_by
        FOREIGN KEY (updated_by)
        REFERENCES users(id)
);

CREATE INDEX IF NOT EXISTS idx_water_meters_created_by
    ON water_meters(created_by);

CREATE INDEX IF NOT EXISTS idx_water_meters_updated_by
    ON water_meters(updated_by);

CREATE INDEX IF NOT EXISTS idx_water_meters_deleted_at
    ON water_meters(deleted_at);


CREATE TABLE IF NOT EXISTS water_meter_assignments (
    id UUID DEFAULT RANDOM_UUID() PRIMARY KEY,
    water_meter_id UUID NOT NULL,
    user_id UUID NOT NULL,

    assigned_at TIMESTAMP WITH TIME ZONE NOT NULL,
    unassigned_at TIMESTAMP WITH TIME ZONE,

    active_assignment_key UUID,

    created_by UUID NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,

    CONSTRAINT fk_meter_assignments_meter
        FOREIGN KEY (water_meter_id)
        REFERENCES water_meters(id),

    CONSTRAINT fk_meter_assignments_user
        FOREIGN KEY (user_id)
        REFERENCES users(id),

    CONSTRAINT fk_meter_assignments_created_by
        FOREIGN KEY (created_by)
        REFERENCES users(id),

    CONSTRAINT ck_meter_assignments_dates
        CHECK (
            unassigned_at IS NULL
            OR unassigned_at >= assigned_at
        ),

    CONSTRAINT ck_meter_assignments_active_key
        CHECK (
            (unassigned_at IS NULL
             AND active_assignment_key = water_meter_id)
            OR
            (unassigned_at IS NOT NULL
             AND active_assignment_key IS NULL)
        ),

    CONSTRAINT uk_meter_assignments_active
        UNIQUE (active_assignment_key)
);

CREATE INDEX IF NOT EXISTS idx_meter_assignments_meter_id
    ON water_meter_assignments(water_meter_id);

CREATE INDEX IF NOT EXISTS idx_meter_assignments_user_id
    ON water_meter_assignments(user_id);

CREATE INDEX IF NOT EXISTS idx_meter_assignments_created_by
    ON water_meter_assignments(created_by);

CREATE INDEX IF NOT EXISTS idx_meter_assignments_meter_active
    ON water_meter_assignments(
        water_meter_id,
        unassigned_at
    );

CREATE INDEX IF NOT EXISTS idx_meter_assignments_user_active
    ON water_meter_assignments(
        user_id,
        unassigned_at
    );


CREATE TABLE IF NOT EXISTS water_meter_billing_plans (
    id UUID DEFAULT RANDOM_UUID() PRIMARY KEY,
    water_meter_id UUID NOT NULL,
    billing_plan_id UUID NOT NULL,

    effective_from TIMESTAMP WITH TIME ZONE NOT NULL,
    effective_to TIMESTAMP WITH TIME ZONE,

    active_plan_key UUID,

    created_by UUID NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,

    CONSTRAINT fk_meter_billing_plans_meter
        FOREIGN KEY (water_meter_id)
        REFERENCES water_meters(id),

    CONSTRAINT fk_meter_billing_plans_plan
        FOREIGN KEY (billing_plan_id)
        REFERENCES billing_plans(id),

    CONSTRAINT fk_meter_billing_plans_created_by
        FOREIGN KEY (created_by)
        REFERENCES users(id),

    CONSTRAINT ck_meter_billing_plans_dates
        CHECK (
            effective_to IS NULL
            OR effective_to >= effective_from
        ),

    CONSTRAINT ck_meter_billing_plans_active_key
        CHECK (
            (effective_to IS NULL
             AND active_plan_key = water_meter_id)
            OR
            (effective_to IS NOT NULL
             AND active_plan_key IS NULL)
        ),

    CONSTRAINT uk_meter_billing_plans_active
        UNIQUE (active_plan_key)
);

CREATE INDEX IF NOT EXISTS idx_meter_billing_plans_meter_id
    ON water_meter_billing_plans(water_meter_id);

CREATE INDEX IF NOT EXISTS idx_meter_billing_plans_plan_id
    ON water_meter_billing_plans(billing_plan_id);

CREATE INDEX IF NOT EXISTS idx_meter_billing_plans_created_by
    ON water_meter_billing_plans(created_by);

CREATE INDEX IF NOT EXISTS idx_meter_billing_plans_meter_dates
    ON water_meter_billing_plans(
        water_meter_id,
        effective_from,
        effective_to
    );


CREATE TABLE IF NOT EXISTS water_meter_readings (
    id UUID DEFAULT RANDOM_UUID() PRIMARY KEY,
    water_meter_id UUID NOT NULL,
    reading_type VARCHAR(20) NOT NULL,
    reading_value DECIMAL(19, 6) NOT NULL,
    reading_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,

    CONSTRAINT fk_water_meter_readings_meter
        FOREIGN KEY (water_meter_id)
        REFERENCES water_meters(id),

    CONSTRAINT ck_water_meter_readings_type
        CHECK (reading_type IN ('TOTAL', 'FLOW')),

    CONSTRAINT ck_water_meter_readings_value
        CHECK (reading_value >= 0),

    CONSTRAINT uk_water_meter_readings_meter_type_time
        UNIQUE (
            water_meter_id,
            reading_type,
            reading_at
        )
);

CREATE INDEX IF NOT EXISTS idx_water_meter_readings_meter_id
    ON water_meter_readings(water_meter_id);

CREATE INDEX IF NOT EXISTS idx_water_meter_readings_meter_type
    ON water_meter_readings(
        water_meter_id,
        reading_type
    );

CREATE INDEX IF NOT EXISTS idx_water_meter_readings_meter_reading_at
    ON water_meter_readings(
        water_meter_id,
        reading_at
    );

CREATE INDEX IF NOT EXISTS idx_water_meter_readings_type_reading_at
    ON water_meter_readings(
        reading_type,
        reading_at
    );


CREATE TABLE IF NOT EXISTS invoices (
    id UUID DEFAULT RANDOM_UUID() PRIMARY KEY,

    water_meter_id UUID NOT NULL,
    user_id UUID NOT NULL,

    billing_month INTEGER NOT NULL,
    billing_year INTEGER NOT NULL,

    total_consumption DECIMAL(19, 6) NOT NULL,
    total_amount DECIMAL(19, 4) NOT NULL,

    generated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    generated_by UUID NOT NULL,

    CONSTRAINT fk_invoices_meter
        FOREIGN KEY (water_meter_id)
        REFERENCES water_meters(id),

    CONSTRAINT fk_invoices_user
        FOREIGN KEY (user_id)
        REFERENCES users(id),

    CONSTRAINT fk_invoices_generated_by
        FOREIGN KEY (generated_by)
        REFERENCES users(id),

    CONSTRAINT ck_invoices_month
        CHECK (billing_month BETWEEN 1 AND 12),

    CONSTRAINT ck_invoices_year
        CHECK (billing_year >= 2000),

    CONSTRAINT ck_invoices_consumption
        CHECK (total_consumption >= 0),

    CONSTRAINT ck_invoices_amount
        CHECK (total_amount >= 0),

    CONSTRAINT uk_invoices_meter_user_period
        UNIQUE (
            water_meter_id,
            user_id,
            billing_month,
            billing_year
        )
);

CREATE INDEX IF NOT EXISTS idx_invoices_user_id
    ON invoices(user_id);

CREATE INDEX IF NOT EXISTS idx_invoices_meter_id
    ON invoices(water_meter_id);

CREATE INDEX IF NOT EXISTS idx_invoices_billing_period
    ON invoices(
        billing_year,
        billing_month
    );

CREATE INDEX IF NOT EXISTS idx_invoices_user_period
    ON invoices(
        user_id,
        billing_year,
        billing_month
    );


CREATE TABLE IF NOT EXISTS invoice_items (
    id UUID DEFAULT RANDOM_UUID() PRIMARY KEY,

    invoice_id UUID NOT NULL,

    opening_reading DECIMAL(19, 6) NOT NULL,
    closing_reading DECIMAL(19, 6) NOT NULL,
    consumption DECIMAL(19, 6) NOT NULL,

    billing_plan_id UUID NOT NULL,
    price_per_unit DECIMAL(19, 4) NOT NULL,
    total_amount DECIMAL(19, 4) NOT NULL,

    period_from TIMESTAMP WITH TIME ZONE NOT NULL,
    period_to TIMESTAMP WITH TIME ZONE NOT NULL,

    CONSTRAINT fk_invoice_items_invoice
        FOREIGN KEY (invoice_id)
        REFERENCES invoices(id),

    CONSTRAINT fk_invoice_items_billing_plan
        FOREIGN KEY (billing_plan_id)
        REFERENCES billing_plans(id),

    CONSTRAINT ck_invoice_items_dates
        CHECK (period_to > period_from),

    CONSTRAINT ck_invoice_items_readings
        CHECK (closing_reading >= opening_reading),

    CONSTRAINT ck_invoice_items_consumption
        CHECK (consumption >= 0),

    CONSTRAINT ck_invoice_items_price
        CHECK (price_per_unit >= 0),

    CONSTRAINT ck_invoice_items_amount
        CHECK (total_amount >= 0)
);

CREATE INDEX IF NOT EXISTS idx_invoice_items_invoice_id
    ON invoice_items(invoice_id);

CREATE INDEX IF NOT EXISTS idx_invoice_items_billing_plan_id
    ON invoice_items(billing_plan_id);


-- ============================================================
-- WATER BILLING SYSTEM
-- Database: H2
-- Schema managed explicitly through SQL
-- ============================================================


-- ============================================================
-- USERS
-- ============================================================

CREATE TABLE IF NOT EXISTS users (
                                     id UUID DEFAULT RANDOM_UUID() PRIMARY KEY,

    username VARCHAR(100) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL,

    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    deleted_at TIMESTAMP WITH TIME ZONE,

                             CONSTRAINT uk_users_username_deleted_at
                             UNIQUE (username, deleted_at),

    CONSTRAINT ck_users_role
    CHECK (role IN ('ADMIN', 'CUSTOMER'))
    );

CREATE INDEX IF NOT EXISTS idx_users_role
    ON users(role);

CREATE INDEX IF NOT EXISTS idx_users_deleted_at
    ON users(deleted_at);


-- ============================================================
-- USER SESSIONS
-- ============================================================

CREATE TABLE IF NOT EXISTS user_sessions (
                                             id UUID DEFAULT RANDOM_UUID() PRIMARY KEY,

    user_id UUID NOT NULL,

    token VARCHAR(255) NOT NULL,

    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    revoked_at TIMESTAMP WITH TIME ZONE,

                             CONSTRAINT uk_user_sessions_token
                             UNIQUE (token),

    CONSTRAINT fk_user_sessions_user
    FOREIGN KEY (user_id)
    REFERENCES users(id)
    );

CREATE INDEX IF NOT EXISTS idx_user_sessions_user_id
    ON user_sessions(user_id);

CREATE INDEX IF NOT EXISTS idx_user_sessions_expires_at
    ON user_sessions(expires_at);


-- ============================================================
-- BILLING PLANS
-- ============================================================

CREATE TABLE IF NOT EXISTS billing_plans (
                                             id UUID DEFAULT RANDOM_UUID() PRIMARY KEY,

    name VARCHAR(100) NOT NULL,
    code VARCHAR(50) NOT NULL,
    description VARCHAR(500),

    plan_type VARCHAR(20) NOT NULL,

    /*
     * Used only for FIXED plans.
     * For SLAB plans the rates are stored in billing_plan_slabs.
     */
    price_per_unit DECIMAL(19, 4),

    active BOOLEAN NOT NULL DEFAULT TRUE,

    created_by UUID NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_by UUID NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    deleted_at TIMESTAMP WITH TIME ZONE,

                             CONSTRAINT uk_billing_plans_code
                             UNIQUE (code),

    CONSTRAINT ck_billing_plans_type
    CHECK (plan_type IN ('FIXED', 'SLAB')),

    CONSTRAINT ck_billing_plans_price
    CHECK (
              price_per_unit IS NULL
              OR price_per_unit >= 0
          ),

    CONSTRAINT ck_billing_plans_fixed_price
    CHECK (
(plan_type = 'FIXED' AND price_per_unit IS NOT NULL)
    OR
(plan_type = 'SLAB')
    ),

    CONSTRAINT fk_billing_plans_created_by
    FOREIGN KEY (created_by)
    REFERENCES users(id),

    CONSTRAINT fk_billing_plans_updated_by
    FOREIGN KEY (updated_by)
    REFERENCES users(id)
    );

CREATE INDEX IF NOT EXISTS idx_billing_plans_active
    ON billing_plans(active);

CREATE INDEX IF NOT EXISTS idx_billing_plans_deleted_at
    ON billing_plans(deleted_at);

CREATE INDEX IF NOT EXISTS idx_billing_plans_created_by
    ON billing_plans(created_by);


-- ============================================================
-- BILLING PLAN SLABS
-- ============================================================

CREATE TABLE IF NOT EXISTS billing_plan_slabs (
                                                  id UUID DEFAULT RANDOM_UUID() PRIMARY KEY,

    billing_plan_id UUID NOT NULL,

    /*
     * Slab represents:
     *
     * lower_bound <= consumption < upper_bound
     *
     * upper_bound NULL means there is no upper limit.
     */
    lower_bound DECIMAL(19, 6) NOT NULL,
    upper_bound DECIMAL(19, 6),

    price_per_unit DECIMAL(19, 4) NOT NULL,

    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    deleted_at TIMESTAMP WITH TIME ZONE,

                             CONSTRAINT fk_billing_plan_slabs_plan
                             FOREIGN KEY (billing_plan_id)
    REFERENCES billing_plans(id),

    CONSTRAINT ck_billing_plan_slabs_lower
    CHECK (lower_bound >= 0),

    CONSTRAINT ck_billing_plan_slabs_upper
    CHECK (
              upper_bound IS NULL
              OR upper_bound > lower_bound
          ),

    CONSTRAINT ck_billing_plan_slabs_price
    CHECK (price_per_unit >= 0)
    );

CREATE INDEX IF NOT EXISTS idx_billing_plan_slabs_plan
    ON billing_plan_slabs(billing_plan_id);

CREATE INDEX IF NOT EXISTS idx_billing_plan_slabs_bounds
    ON billing_plan_slabs(
    billing_plan_id,
    lower_bound,
    upper_bound
    );

CREATE INDEX IF NOT EXISTS idx_billing_plan_slabs_deleted_at
    ON billing_plan_slabs(deleted_at);


-- ============================================================
-- WATER METERS
-- ============================================================

CREATE TABLE IF NOT EXISTS water_meters (
                                            id UUID DEFAULT RANDOM_UUID() PRIMARY KEY,

    meter_number VARCHAR(100) NOT NULL,

    created_by UUID NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,

    updated_by UUID NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,

    deleted_at TIMESTAMP WITH TIME ZONE,


                             CONSTRAINT uk_water_meters_meter_number_deleted_at
                             UNIQUE (meter_number, deleted_at),
    CONSTRAINT fk_water_meters_created_by
    FOREIGN KEY (created_by)
    REFERENCES users(id),

    CONSTRAINT fk_water_meters_updated_by
    FOREIGN KEY (updated_by)
    REFERENCES users(id)
    );

CREATE INDEX IF NOT EXISTS idx_water_meters_deleted_at
    ON water_meters(deleted_at);

CREATE INDEX IF NOT EXISTS idx_water_meters_created_by
    ON water_meters(created_by);


-- ============================================================
-- WATER METER ASSIGNMENTS
-- ============================================================

CREATE TABLE IF NOT EXISTS water_meter_assignments (
                                                       id UUID DEFAULT RANDOM_UUID() PRIMARY KEY,

    water_meter_id UUID NOT NULL,
    user_id UUID NOT NULL,

    assigned_at TIMESTAMP WITH TIME ZONE NOT NULL,
    unassigned_at TIMESTAMP WITH TIME ZONE,

    /*
     * H2 does not provide a convenient PostgreSQL-style
     * partial unique index.
     *
     * For an active assignment:
     * active_assignment_key = water_meter_id
     *
     * For a historical assignment:
     * active_assignment_key = NULL
     *
     * UNIQUE therefore permits only one active assignment
     * for a meter.
     */
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
(
              unassigned_at IS NULL
              AND active_assignment_key = water_meter_id
)
    OR
(
    unassigned_at IS NOT NULL
    AND active_assignment_key IS NULL
)
    ),

    CONSTRAINT uk_meter_assignments_active
    UNIQUE (active_assignment_key)
    );

CREATE INDEX IF NOT EXISTS idx_meter_assignments_meter
    ON water_meter_assignments(
    water_meter_id
    );

CREATE INDEX IF NOT EXISTS idx_meter_assignments_user
    ON water_meter_assignments(
    user_id
    );

CREATE INDEX IF NOT EXISTS idx_meter_assignments_meter_dates
    ON water_meter_assignments(
    water_meter_id,
    assigned_at,
    unassigned_at
    );

CREATE INDEX IF NOT EXISTS idx_meter_assignments_user_dates
    ON water_meter_assignments(
    user_id,
    assigned_at,
    unassigned_at
    );


-- ============================================================
-- WATER METER BILLING PLAN HISTORY
-- ============================================================

CREATE TABLE IF NOT EXISTS water_meter_billing_plans (
                                                         id UUID DEFAULT RANDOM_UUID() PRIMARY KEY,

    water_meter_id UUID NOT NULL,
    billing_plan_id UUID NOT NULL,

    effective_from TIMESTAMP WITH TIME ZONE NOT NULL,
    effective_to TIMESTAMP WITH TIME ZONE,

    /*
     * One active/open plan per meter.
     */
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
              OR effective_to > effective_from
          ),

    CONSTRAINT ck_meter_billing_plans_active_key
    CHECK (
(
              effective_to IS NULL
              AND active_plan_key = water_meter_id
)
    OR
(
    effective_to IS NOT NULL
    AND active_plan_key IS NULL
)
    ),

    CONSTRAINT uk_meter_billing_plans_active
    UNIQUE (active_plan_key)
    );

CREATE INDEX IF NOT EXISTS idx_meter_billing_plans_meter
    ON water_meter_billing_plans(
    water_meter_id
    );

CREATE INDEX IF NOT EXISTS idx_meter_billing_plans_plan
    ON water_meter_billing_plans(
    billing_plan_id
    );

CREATE INDEX IF NOT EXISTS idx_meter_billing_plans_dates
    ON water_meter_billing_plans(
    water_meter_id,
    effective_from,
    effective_to
    );


-- ============================================================
-- WATER METER READINGS
-- ============================================================

CREATE TABLE IF NOT EXISTS water_meter_readings (
                                                    id UUID DEFAULT RANDOM_UUID() PRIMARY KEY,

    water_meter_id UUID NOT NULL,

    reading_type VARCHAR(20) NOT NULL,

    reading_value DECIMAL(19, 6) NOT NULL,

    reading_at TIMESTAMP WITH TIME ZONE NOT NULL,

                             /*
                              * Unique identifier supplied by the meter/integration
                              * for idempotent ingestion.
                              */
                             ingestion_key VARCHAR(255) NOT NULL,

    created_at TIMESTAMP WITH TIME ZONE NOT NULL,

                             CONSTRAINT fk_water_meter_readings_meter
                             FOREIGN KEY (water_meter_id)
    REFERENCES water_meters(id),

    CONSTRAINT ck_water_meter_readings_type
    CHECK (
              reading_type IN ('TOTAL', 'FLOW')
    ),

    CONSTRAINT ck_water_meter_readings_value
    CHECK (
              reading_value >= 0
          ),

    /*
     * Same ingestion event cannot be inserted twice.
     */
    CONSTRAINT uk_water_meter_readings_ingestion
    UNIQUE (
               water_meter_id,
               ingestion_key
           ),

    /*
     * Same meter/type/timestamp is also prevented.
     */
    CONSTRAINT uk_water_meter_readings_meter_type_time
    UNIQUE (
               water_meter_id,
               reading_type,
               reading_at
           )
    );

CREATE INDEX IF NOT EXISTS idx_water_meter_readings_meter
    ON water_meter_readings(
    water_meter_id
    );

CREATE INDEX IF NOT EXISTS idx_water_meter_readings_meter_type_time
    ON water_meter_readings(
    water_meter_id,
    reading_type,
    reading_at
    );

CREATE INDEX IF NOT EXISTS idx_water_meter_readings_ingestion
    ON water_meter_readings(
    water_meter_id,
    ingestion_key
    );


-- ============================================================
-- INVOICES
-- ============================================================

CREATE TABLE IF NOT EXISTS invoices (
                                        id UUID DEFAULT RANDOM_UUID() PRIMARY KEY,

    water_meter_id UUID NOT NULL,
    user_id UUID NOT NULL,

    billing_month INTEGER NOT NULL,
    billing_year INTEGER NOT NULL,

    /*
     * Exact billing period represented by the invoice.
     */
    billing_period_start TIMESTAMP WITH TIME ZONE NOT NULL,
    billing_period_end TIMESTAMP WITH TIME ZONE NOT NULL,

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
    CHECK (
              billing_month BETWEEN 1 AND 12
          ),

    CONSTRAINT ck_invoices_year
    CHECK (
              billing_year >= 2000
          ),

    CONSTRAINT ck_invoices_period
    CHECK (
              billing_period_end > billing_period_start
          ),

    CONSTRAINT ck_invoices_consumption
    CHECK (
              total_consumption >= 0
          ),

    CONSTRAINT ck_invoices_amount
    CHECK (
              total_amount >= 0
          ),

    /*
     * One invoice per customer + meter + billing month.
     */
    CONSTRAINT uk_invoices_customer_meter_period
    UNIQUE (
               water_meter_id,
               user_id,
               billing_month,
               billing_year
           )
    );

CREATE INDEX IF NOT EXISTS idx_invoices_user
    ON invoices(user_id);

CREATE INDEX IF NOT EXISTS idx_invoices_meter
    ON invoices(water_meter_id);

CREATE INDEX IF NOT EXISTS idx_invoices_period
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


-- ============================================================
-- INVOICE ITEMS
-- ============================================================

CREATE TABLE IF NOT EXISTS invoice_items (
                                             id UUID DEFAULT RANDOM_UUID() PRIMARY KEY,

    invoice_id UUID NOT NULL,

    /*
     * Snapshot of the meter readings used for this
     * historical billing segment.
     */
    opening_reading DECIMAL(19, 6) NOT NULL,
    closing_reading DECIMAL(19, 6) NOT NULL,
    consumption DECIMAL(19, 6) NOT NULL,

    /*
     * Snapshot of the billing plan used at that time.
     */
    billing_plan_id UUID NOT NULL,

    /*
     * Nullable for a fixed-plan item.
     * Populated when the item represents a specific slab.
     */
    billing_plan_slab_id UUID,

    /*
     * Snapshot values.
     *
     * These are deliberately stored on the invoice item
     * so future plan changes cannot alter old invoices.
     */
    slab_lower_bound DECIMAL(19, 6),
    slab_upper_bound DECIMAL(19, 6),

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

    CONSTRAINT fk_invoice_items_billing_plan_slab
    FOREIGN KEY (billing_plan_slab_id)
    REFERENCES billing_plan_slabs(id),

    CONSTRAINT ck_invoice_items_dates
    CHECK (
              period_to > period_from
          ),

    CONSTRAINT ck_invoice_items_readings
    CHECK (
              closing_reading >= opening_reading
          ),

    CONSTRAINT ck_invoice_items_consumption
    CHECK (
              consumption >= 0
          ),

    CONSTRAINT ck_invoice_items_price
    CHECK (
              price_per_unit >= 0
          ),

    CONSTRAINT ck_invoice_items_amount
    CHECK (
              total_amount >= 0
          ),

    CONSTRAINT ck_invoice_items_slab_bounds
    CHECK (
(
              billing_plan_slab_id IS NULL
              AND slab_lower_bound IS NULL
              AND slab_upper_bound IS NULL
)
    OR
(
    billing_plan_slab_id IS NOT NULL
    AND slab_lower_bound IS NOT NULL
    AND slab_lower_bound >= 0
    AND (
    slab_upper_bound IS NULL
    OR slab_upper_bound > slab_lower_bound
        )
    )
    )
    );

CREATE INDEX IF NOT EXISTS idx_invoice_items_invoice
    ON invoice_items(invoice_id);

CREATE INDEX IF NOT EXISTS idx_invoice_items_billing_plan
    ON invoice_items(billing_plan_id);

CREATE INDEX IF NOT EXISTS idx_invoice_items_billing_plan_slab
    ON invoice_items(billing_plan_slab_id);


-- ============================================================
-- END OF SCHEMA
-- ============================================================
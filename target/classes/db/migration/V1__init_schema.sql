-- ════════════════════════════════════════════════════════════════
-- V1__init_schema.sql
-- Utilise gen_random_uuid() — fonctionne sur PostgreSQL
-- Pour H2 : ajouter dans application-dev.yml :
--   spring.flyway.locations: classpath:db/migration,classpath:db/h2
-- ════════════════════════════════════════════════════════════════

CREATE TABLE IF NOT EXISTS tenants (
    id            UUID         NOT NULL PRIMARY KEY,
    name          VARCHAR(255) NOT NULL,
    slug          VARCHAR(100) NOT NULL UNIQUE,
    country_code  VARCHAR(2)   NOT NULL,
    sector        VARCHAR(50),
    plan          VARCHAR(20)  NOT NULL DEFAULT 'STARTER',
    mode          VARCHAR(20)  NOT NULL DEFAULT 'RH_ONLY',
    locale        VARCHAR(10)  NOT NULL DEFAULT 'fr',
    timezone      VARCHAR(50)  NOT NULL DEFAULT 'Africa/Dakar',
    currency      VARCHAR(3)   NOT NULL DEFAULT 'XOF',
    status        VARCHAR(20)  NOT NULL DEFAULT 'TRIAL',
    trial_ends_at TIMESTAMP WITH TIME ZONE,
    created_at    TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at    TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS tenant_modules (
    tenant_id UUID        NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    module    VARCHAR(50) NOT NULL,
    PRIMARY KEY (tenant_id, module)
);

CREATE TABLE IF NOT EXISTS employees (
    id                  UUID         NOT NULL PRIMARY KEY,
    tenant_id           UUID         NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    first_name          VARCHAR(100) NOT NULL,
    last_name           VARCHAR(100) NOT NULL,
    email               VARCHAR(255),
    phone               VARCHAR(30),
    role                VARCHAR(20)  NOT NULL DEFAULT 'STAFF',
    department          VARCHAR(100),
    position            VARCHAR(100),
    hire_date           DATE         NOT NULL,
    status              VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    edukira_teacher_id  VARCHAR(100),
    edukira_tenant_id   VARCHAR(100),
    last_edukira_sync   TIMESTAMP WITH TIME ZONE,
    created_at          TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at          TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    CONSTRAINT uq_emp_email UNIQUE (tenant_id, email)
);

CREATE INDEX IF NOT EXISTS idx_emp_tenant ON employees(tenant_id);
CREATE INDEX IF NOT EXISTS idx_emp_status ON employees(tenant_id, status);

CREATE TABLE IF NOT EXISTS contracts (
    id               UUID          NOT NULL PRIMARY KEY,
    tenant_id        UUID          NOT NULL REFERENCES tenants(id),
    employee_id      UUID          NOT NULL REFERENCES employees(id) ON DELETE CASCADE,
    type             VARCHAR(20)   NOT NULL,
    start_date       DATE          NOT NULL,
    end_date         DATE,
    salary_amount    NUMERIC(12,2) NOT NULL,
    salary_currency  VARCHAR(3)    NOT NULL DEFAULT 'XOF',
    salary_period    VARCHAR(10)   NOT NULL DEFAULT 'monthly',
    document_url     TEXT,
    status           VARCHAR(20)   NOT NULL DEFAULT 'active',
    terminated_at    TIMESTAMP WITH TIME ZONE,
    termination_reason TEXT,
    created_at       TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_contracts_employee ON contracts(employee_id);

CREATE TABLE IF NOT EXISTS timesheets (
    id             UUID        NOT NULL PRIMARY KEY,
    tenant_id      UUID        NOT NULL REFERENCES tenants(id),
    employee_id    UUID        NOT NULL REFERENCES employees(id),
    date           DATE        NOT NULL,
    check_in       TIME,
    check_out      TIME,
    source         VARCHAR(30) NOT NULL DEFAULT 'MOBILE',
    geo_lat        DECIMAL(9,6),
    geo_lng        DECIMAL(9,6),
    offline_queued BOOLEAN     NOT NULL DEFAULT FALSE,
    synced_at      TIMESTAMP WITH TIME ZONE,
    note           VARCHAR(500),
    created_by     UUID        REFERENCES employees(id),
    created_at     TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    CONSTRAINT uq_ts UNIQUE (tenant_id, employee_id, date)
);

CREATE INDEX IF NOT EXISTS idx_ts_tenant_date ON timesheets(tenant_id, date);
CREATE INDEX IF NOT EXISTS idx_ts_employee    ON timesheets(employee_id, date);

CREATE TABLE IF NOT EXISTS payrolls (
    id                     UUID          NOT NULL PRIMARY KEY,
    tenant_id              UUID          NOT NULL REFERENCES tenants(id),
    employee_id            UUID          NOT NULL REFERENCES employees(id),
    period_year            SMALLINT      NOT NULL,
    period_month           SMALLINT      NOT NULL CHECK (period_month BETWEEN 1 AND 12),
    gross_salary           DECIMAL(12,2) NOT NULL,
    net_salary             DECIMAL(12,2) NOT NULL,
    total_deductions       DECIMAL(12,2),
    total_additions        DECIMAL(12,2),
    deductions_detail      TEXT,
    additions_detail       TEXT,
    country_config_version VARCHAR(20),
    pdf_url                VARCHAR(500),
    status                 VARCHAR(20)   NOT NULL DEFAULT 'DRAFT',
    generated_at           TIMESTAMP WITH TIME ZONE,
    created_at             TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    CONSTRAINT uq_payroll UNIQUE (tenant_id, employee_id, period_year, period_month)
);

CREATE INDEX IF NOT EXISTS idx_payroll_emp ON payrolls(employee_id);

CREATE TABLE IF NOT EXISTS leave_requests (
    id           UUID        NOT NULL PRIMARY KEY,
    tenant_id    UUID        NOT NULL REFERENCES tenants(id),
    employee_id  UUID        NOT NULL REFERENCES employees(id),
    leave_type   VARCHAR(30) NOT NULL,
    start_date   DATE        NOT NULL,
    end_date     DATE        NOT NULL,
    days_count   INTEGER     NOT NULL,
    reason       VARCHAR(500),
    document_url VARCHAR(500),
    status       VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    reviewed_by  UUID        REFERENCES employees(id),
    review_note  VARCHAR(500),
    reviewed_at  TIMESTAMP WITH TIME ZONE,
    created_at   TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_leave_tenant ON leave_requests(tenant_id, status);
CREATE INDEX IF NOT EXISTS idx_leave_emp    ON leave_requests(employee_id);
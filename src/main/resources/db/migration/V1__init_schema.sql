CREATE TYPE card_status AS ENUM ('ACTIVE', 'BLOCKED', 'CANCELLED');
CREATE TYPE spend_period AS ENUM ('DAILY', 'WEEKLY', 'MONTHLY');
CREATE TYPE limit_type AS ENUM ('PER_TRANSACTION', 'DAILY', 'WEEKLY', 'MONTHLY');
CREATE TYPE transaction_status AS ENUM ('PENDING', 'APPROVED', 'DECLINED', 'REVERSED');
CREATE TYPE billing_status AS ENUM ('OPEN', 'CLOSED', 'INVOICED');
CREATE TYPE webhook_status AS ENUM ('PENDING', 'DELIVERED', 'FAILED');

CREATE TABLE cards (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    team_id     UUID NOT NULL,
    holder_id   UUID NOT NULL,
    status      card_status NOT NULL DEFAULT 'ACTIVE',
    currency    VARCHAR(3) NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE spending_limits (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    card_id     UUID NOT NULL REFERENCES cards(id),
    limit_type  limit_type NOT NULL,
    amount      NUMERIC(19,4) NOT NULL,
    currency    VARCHAR(3) NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (card_id, limit_type)
);

CREATE TABLE transactions (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    card_id             UUID NOT NULL REFERENCES cards(id),
    amount              NUMERIC(19,4) NOT NULL,
    currency            VARCHAR(3) NOT NULL,
    merchant_category   VARCHAR(64),
    status              transaction_status NOT NULL DEFAULT 'PENDING',
    decline_reason      VARCHAR(64),
    applied_markup      NUMERIC(19,4),
    idempotency_key     VARCHAR(128) NOT NULL UNIQUE,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE pricing_rules (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    merchant_category   VARCHAR(64),
    currency_code       VARCHAR(3),
    markup_percentage   NUMERIC(5,4) NOT NULL,
    effective_from      TIMESTAMPTZ NOT NULL,
    effective_to        TIMESTAMPTZ,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE feature_flags (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    flag_key                VARCHAR(128) NOT NULL UNIQUE,
    enabled                 BOOLEAN NOT NULL DEFAULT FALSE,
    rollout_percentage      INTEGER NOT NULL DEFAULT 0,
    target_entity_ids       TEXT[],
    created_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE billing_records (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    team_id         UUID NOT NULL,
    billing_period  VARCHAR(7) NOT NULL,
    total_amount    NUMERIC(19,4) NOT NULL DEFAULT 0,
    currency        VARCHAR(3) NOT NULL DEFAULT 'EUR',
    status          billing_status NOT NULL DEFAULT 'OPEN',
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (team_id, billing_period)
);

CREATE TABLE billing_line_items (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    billing_record_id   UUID NOT NULL REFERENCES billing_records(id),
    merchant_category   VARCHAR(64),
    transaction_count   INTEGER NOT NULL DEFAULT 0,
    total_amount        NUMERIC(19,4) NOT NULL DEFAULT 0,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (billing_record_id, merchant_category)
);

CREATE TABLE webhook_configurations (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    holder_id   UUID NOT NULL UNIQUE,
    url         VARCHAR(512) NOT NULL,
    secret      VARCHAR(256),
    enabled     BOOLEAN NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE webhook_deliveries (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    transaction_id  UUID NOT NULL REFERENCES transactions(id),
    webhook_url     VARCHAR(512) NOT NULL,
    event_type      VARCHAR(64) NOT NULL,
    status          webhook_status NOT NULL DEFAULT 'PENDING',
    attempt_count   INTEGER NOT NULL DEFAULT 0,
    last_attempted  TIMESTAMPTZ,
    response_status INTEGER,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_transactions_card_id ON transactions(card_id);
CREATE INDEX idx_transactions_idempotency_key ON transactions(idempotency_key);
CREATE INDEX idx_transactions_created_at ON transactions(created_at);
CREATE INDEX idx_spending_limits_card_id ON spending_limits(card_id);
CREATE INDEX idx_billing_records_team_period ON billing_records(team_id, billing_period);
CREATE INDEX idx_webhook_deliveries_transaction ON webhook_deliveries(transaction_id);
CREATE INDEX idx_pricing_rules_category ON pricing_rules(merchant_category);

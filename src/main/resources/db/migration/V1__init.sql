CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    name VARCHAR(255) NOT NULL,
    phone VARCHAR(32),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE categories (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    slug VARCHAR(64) NOT NULL UNIQUE,
    name VARCHAR(128) NOT NULL,
    icon VARCHAR(64),
    sort_order INT NOT NULL DEFAULT 0,
    is_active BOOLEAN NOT NULL DEFAULT true
);

CREATE TABLE services (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    slug VARCHAR(64) NOT NULL UNIQUE,
    name VARCHAR(128) NOT NULL,
    icon VARCHAR(64),
    sort_order INT NOT NULL DEFAULT 0,
    is_active BOOLEAN NOT NULL DEFAULT true
);

CREATE TABLE forms (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code VARCHAR(64) NOT NULL UNIQUE,
    type VARCHAR(64) NOT NULL,
    category_id UUID REFERENCES categories (id),
    service_id UUID REFERENCES services (id),
    title VARCHAR(255) NOT NULL,
    form_schema JSONB NOT NULL,
    version INT NOT NULL DEFAULT 1,
    is_active BOOLEAN NOT NULL DEFAULT true,
    CONSTRAINT forms_one_parent CHECK (
        (category_id IS NOT NULL AND service_id IS NULL)
        OR (category_id IS NULL AND service_id IS NOT NULL)
    )
);

CREATE INDEX idx_forms_category_id ON forms (category_id);
CREATE INDEX idx_forms_service_id ON forms (service_id);

CREATE TABLE ads (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users (id),
    form_id UUID NOT NULL REFERENCES forms (id),
    category_id UUID REFERENCES categories (id),
    service_id UUID REFERENCES services (id),
    title VARCHAR(255) NOT NULL,
    status VARCHAR(32) NOT NULL,
    data JSONB NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_ads_user_id ON ads (user_id);
CREATE INDEX idx_ads_category_id ON ads (category_id);
CREATE INDEX idx_ads_service_id ON ads (service_id);
CREATE INDEX idx_ads_created_at ON ads (created_at DESC);

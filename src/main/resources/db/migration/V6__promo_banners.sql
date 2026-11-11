CREATE TABLE promo_banners (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title VARCHAR(120) NOT NULL,
    description VARCHAR(280) NOT NULL,
    target_url VARCHAR(1000) NOT NULL,
    sort_order INT NOT NULL DEFAULT 0,
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_promo_banners_active_sort ON promo_banners (is_active, sort_order);

CREATE TABLE coupons (
    id UUID PRIMARY KEY,
    normalized_code VARCHAR(64) NOT NULL,
    created_at TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    max_usages INTEGER NOT NULL,
    current_usages INTEGER NOT NULL DEFAULT 0,
    country VARCHAR(2) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_coupons_normalized_code UNIQUE (normalized_code),
    CONSTRAINT ck_coupons_code_format CHECK (normalized_code ~ '^[A-Z0-9_-]{1,64}$'),
    CONSTRAINT ck_coupons_country_format CHECK (country ~ '^[A-Z]{2}$'),
    CONSTRAINT ck_coupons_max_usages CHECK (max_usages > 0),
    CONSTRAINT ck_coupons_current_usages CHECK (current_usages BETWEEN 0 AND max_usages)
);

CREATE TABLE coupon_usages (
    id UUID PRIMARY KEY,
    coupon_id UUID NOT NULL,
    used_by VARCHAR(128) NOT NULL,
    used_at TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    country VARCHAR(2) NOT NULL,
    CONSTRAINT fk_coupon_usages_coupon FOREIGN KEY (coupon_id) REFERENCES coupons (id),
    CONSTRAINT uk_coupon_usages_coupon_user UNIQUE (coupon_id, used_by),
    CONSTRAINT ck_coupon_usages_used_by_not_blank CHECK (used_by = btrim(used_by) AND used_by <> ''),
    CONSTRAINT ck_coupon_usages_country_format CHECK (country ~ '^[A-Z]{2}$')
);

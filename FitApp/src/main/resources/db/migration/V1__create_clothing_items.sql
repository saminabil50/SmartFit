CREATE TABLE IF NOT EXISTS clothing_items (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    category VARCHAR(255) NOT NULL,
    gender VARCHAR(255) NOT NULL,
    brand VARCHAR(255),
    size_system VARCHAR(255) NOT NULL DEFAULT 'INT',
    available_sizes TEXT,
    base_price DOUBLE PRECISION,
    currency VARCHAR(255),
    image_url VARCHAR(255),
    size_chart TEXT,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_clothing_items_active_created_at
    ON clothing_items (is_active, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_clothing_items_category
    ON clothing_items (category);

CREATE INDEX IF NOT EXISTS idx_clothing_items_gender
    ON clothing_items (gender);

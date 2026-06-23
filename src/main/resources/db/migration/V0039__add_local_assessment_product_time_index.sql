CREATE INDEX IF NOT EXISTS idx_local_assessment_product_time
    ON product.local_assessment (product_id, created_time DESC);

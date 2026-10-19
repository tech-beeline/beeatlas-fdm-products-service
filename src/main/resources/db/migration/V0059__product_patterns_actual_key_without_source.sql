
WITH keepers AS (
    SELECT DISTINCT ON (product_alias, product_branch, pattern_code) id
    FROM product.product_patterns
    WHERE is_actual = true
    ORDER BY product_alias, product_branch, pattern_code, created_date DESC, id DESC
)
UPDATE product.product_patterns pp
SET is_actual = false
WHERE pp.is_actual = true
  AND pp.id NOT IN (SELECT id FROM keepers);

DROP INDEX IF EXISTS product.idx_product_patterns_actual_key;

CREATE UNIQUE INDEX IF NOT EXISTS idx_product_patterns_actual_key
    ON product.product_patterns (product_alias, product_branch, pattern_code)
    WHERE is_actual = true;

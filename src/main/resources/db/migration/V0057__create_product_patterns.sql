CREATE SEQUENCE IF NOT EXISTS product.product_patterns_id_seq;

CREATE TABLE IF NOT EXISTS product.product_patterns
(
    id              INTEGER PRIMARY KEY DEFAULT nextval('product.product_patterns_id_seq'),
    pattern_code    TEXT        NOT NULL,
    is_check        BOOLEAN     NOT NULL,
    result_details  TEXT,
    product_alias   TEXT        NOT NULL,
    product_branch  TEXT        NOT NULL,
    source_type_id  INTEGER     NOT NULL,
    source_id       INTEGER,
    created_date    TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_actual       BOOLEAN     NOT NULL,

    FOREIGN KEY (source_type_id)
        REFERENCES product.enum_source_type (id)
        ON DELETE RESTRICT
);

CREATE INDEX IF NOT EXISTS idx_product_patterns_actual_key
    ON product.product_patterns (product_alias, product_branch, source_type_id, source_id, pattern_code)
    WHERE is_actual = true;

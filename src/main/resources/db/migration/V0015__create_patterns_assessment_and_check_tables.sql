CREATE SEQUENCE product.patterns_assessment_id_seq;
CREATE SEQUENCE product.patterns_check_id_seq;

CREATE TABLE product.patterns_assessment
(
    id             INTEGER PRIMARY KEY DEFAULT nextval('product.patterns_assessment_id_seq'),
    product_id     INTEGER   NOT NULL,
    source_type_id INTEGER   NOT NULL,
    source_id      INTEGER,
    create_date    TIMESTAMP NOT NULL  DEFAULT CURRENT_TIMESTAMP,

    FOREIGN KEY (product_id)
        REFERENCES product.product (id)
        ON DELETE CASCADE,

    FOREIGN KEY (source_type_id)
        REFERENCES product.enum_source_type (id)
        ON DELETE RESTRICT
);

CREATE TABLE product.patterns_check
(
    id             INTEGER PRIMARY KEY DEFAULT nextval('product.patterns_check_id_seq'),
    assessment_id  INTEGER NOT NULL,
    pattern_code   TEXT    NOT NULL,
    is_check       BOOLEAN NOT NULL,
    result_details TEXT,

    FOREIGN KEY (assessment_id)
        REFERENCES product.patterns_assessment (id)
        ON DELETE CASCADE
);

CREATE INDEX idx_patterns_assessment_product
    ON product.patterns_assessment (product_id);

CREATE INDEX idx_patterns_check_assessment
    ON product.patterns_check (assessment_id);

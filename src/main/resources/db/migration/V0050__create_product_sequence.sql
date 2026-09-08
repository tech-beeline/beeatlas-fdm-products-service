CREATE SEQUENCE IF NOT EXISTS product.sequence_id_seq;

CREATE TABLE IF NOT EXISTS product.sequence
(
    id                INTEGER NOT NULL DEFAULT nextval('product.sequence_id_seq'),
    product_branch_id INTEGER NOT NULL,
    code              TEXT    NOT NULL,
    name              TEXT    NOT NULL,
    description       TEXT,
    tc_code           TEXT,
    PRIMARY KEY (id),
    CONSTRAINT uq_sequence_branch_code UNIQUE (product_branch_id, code),
    CONSTRAINT fk_sequence_product_branch
        FOREIGN KEY (product_branch_id) REFERENCES product.product_branch (id)
);

ALTER SEQUENCE product.sequence_id_seq OWNED BY product.sequence.id;

CREATE SEQUENCE IF NOT EXISTS product.sequence_step_operation_id_seq;

CREATE TABLE IF NOT EXISTS product.sequence_step_operation
(
    id                       INTEGER NOT NULL DEFAULT nextval('product.sequence_step_operation_id_seq'),
    operation_product_alias  TEXT,
    operation_container_code TEXT,
    operation_interface_code TEXT,
    operation_name           TEXT,
    operation_type           TEXT,
    operation_id             INTEGER,
    PRIMARY KEY (id),
    CONSTRAINT fk_sequence_step_operation_operation
        FOREIGN KEY (operation_id) REFERENCES product.operation (id)
);

ALTER SEQUENCE product.sequence_step_operation_id_seq OWNED BY product.sequence_step_operation.id;

CREATE SEQUENCE IF NOT EXISTS product.seq_product_step_id_seq;

CREATE TABLE IF NOT EXISTS product.seq_product_step
(
    id                            INTEGER NOT NULL DEFAULT nextval('product.seq_product_step_id_seq'),
    seq_id                        INTEGER NOT NULL,
    "order"                       INTEGER NOT NULL,
    raw_description               TEXT,
    seq_step_operation_id         INTEGER,
    seq_step_related_operation_id INTEGER NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_seq_product_step_sequence
        FOREIGN KEY (seq_id) REFERENCES product.sequence (id) ON DELETE CASCADE,
    CONSTRAINT fk_seq_product_step_operation
        FOREIGN KEY (seq_step_operation_id) REFERENCES product.sequence_step_operation (id),
    CONSTRAINT fk_seq_product_step_related_operation
        FOREIGN KEY (seq_step_related_operation_id) REFERENCES product.sequence_step_operation (id)
);

ALTER SEQUENCE product.seq_product_step_id_seq OWNED BY product.seq_product_step.id;

CREATE INDEX IF NOT EXISTS idx_seq_product_step_seq_id
    ON product.seq_product_step (seq_id);

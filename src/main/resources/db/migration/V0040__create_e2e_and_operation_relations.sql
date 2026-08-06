CREATE SEQUENCE IF NOT EXISTS product.e2e_id_seq;

CREATE TABLE IF NOT EXISTS product.e2e
(
    id           INTEGER NOT NULL DEFAULT nextval('product.e2e_id_seq'),
    code         TEXT    NOT NULL,
    name         TEXT,
    description  TEXT,
    bi_step_code TEXT,
    PRIMARY KEY (id),
    CONSTRAINT uq_e2e_code UNIQUE (code)
);

ALTER SEQUENCE product.e2e_id_seq OWNED BY product.e2e.id;

CREATE SEQUENCE IF NOT EXISTS product.operation_relations_id_seq;

CREATE TABLE IF NOT EXISTS product.operation_relations
(
    id                     INTEGER NOT NULL DEFAULT nextval('product.operation_relations_id_seq'),
    outgoing_operation_id  INTEGER,
    incoming_operation_id  INTEGER NOT NULL,
    "order"                INTEGER NOT NULL,
    stereo_type            TEXT    NOT NULL,
    e2e_id                 INTEGER NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_operation_relations_outgoing_operation
        FOREIGN KEY (outgoing_operation_id) REFERENCES product.operation (id),
    CONSTRAINT fk_operation_relations_incoming_operation
        FOREIGN KEY (incoming_operation_id) REFERENCES product.operation (id),
    CONSTRAINT fk_operation_relations_e2e
        FOREIGN KEY (e2e_id) REFERENCES product.e2e (id)
);

ALTER SEQUENCE product.operation_relations_id_seq OWNED BY product.operation_relations.id;

CREATE INDEX IF NOT EXISTS idx_operation_relations_e2e_id
    ON product.operation_relations (e2e_id);

CREATE INDEX IF NOT EXISTS idx_operation_relations_outgoing_operation_id
    ON product.operation_relations (outgoing_operation_id);

CREATE INDEX IF NOT EXISTS idx_operation_relations_incoming_operation_id
    ON product.operation_relations (incoming_operation_id);

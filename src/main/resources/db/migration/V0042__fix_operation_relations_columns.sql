
DROP INDEX IF EXISTS product.idx_operation_relations_outgoing_operation_id;
DROP INDEX IF EXISTS product.idx_operation_relations_incoming_operation_id;

ALTER TABLE product.operation_relations
DROP CONSTRAINT IF EXISTS fk_operation_relations_outgoing_operation;

ALTER TABLE product.operation_relations
DROP CONSTRAINT IF EXISTS fk_operation_relations_incoming_operation;

ALTER TABLE product.operation_relations
    RENAME COLUMN outgoing_operation_id TO operation_id;

ALTER TABLE product.operation_relations
    RENAME COLUMN incoming_operation_id TO related_operation_id;

ALTER TABLE product.operation_relations
    ALTER COLUMN stereo_type DROP NOT NULL;

ALTER TABLE product.operation_relations
    ADD CONSTRAINT fk_operation_relations_operation
        FOREIGN KEY (operation_id) REFERENCES product.operation (id);

ALTER TABLE product.operation_relations
    ADD CONSTRAINT fk_operation_relations_related_operation
        FOREIGN KEY (related_operation_id) REFERENCES product.operation (id);

CREATE INDEX IF NOT EXISTS idx_operation_relations_operation_id
    ON product.operation_relations (operation_id);

CREATE INDEX IF NOT EXISTS idx_operation_relations_related_operation_id
    ON product.operation_relations (related_operation_id);

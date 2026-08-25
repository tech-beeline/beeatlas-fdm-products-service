ALTER TABLE product.discovered_interface
    ADD COLUMN IF NOT EXISTS source TEXT NOT NULL DEFAULT 'MAPIC';

ALTER TABLE product.discovered_interface
    ALTER COLUMN context DROP NOT NULL;

ALTER TABLE product.discovered_interface
    ALTER COLUMN external_id TYPE TEXT USING external_id::TEXT;

ALTER TABLE product.discovered_interface
    ALTER COLUMN api_id DROP NOT NULL;

ALTER TABLE product.discovered_interface
    ALTER COLUMN status DROP NOT NULL;

ALTER TABLE product.discovered_operation
    ALTER COLUMN context DROP NOT NULL;

ALTER TABLE product.discovered_operation
    ADD COLUMN IF NOT EXISTS rps NUMERIC(15, 5);

ALTER TABLE product.discovered_operation
    ADD COLUMN IF NOT EXISTS latency NUMERIC(15, 5);

ALTER TABLE product.discovered_operation
    ADD COLUMN IF NOT EXISTS error_rate NUMERIC(15, 5);

ALTER TABLE product.operation_relations
    ADD COLUMN IF NOT EXISTS entity_type_operation TEXT NOT NULL DEFAULT 'operation';

ALTER TABLE product.operation_relations
    ADD COLUMN IF NOT EXISTS entity_type_operation_relation TEXT NOT NULL DEFAULT 'operation';

ALTER TABLE product.operation_relations
    DROP CONSTRAINT IF EXISTS fk_operation_relations_operation;

ALTER TABLE product.operation_relations
    DROP CONSTRAINT IF EXISTS fk_operation_relations_related_operation;

CREATE INDEX IF NOT EXISTS idx_discovered_interface_source_product_external
    ON product.discovered_interface (source, product_id, external_id);

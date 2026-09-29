
DROP INDEX IF EXISTS product.idx_discovered_interface_project_id;

ALTER TABLE product.discovered_interface
    DROP COLUMN IF EXISTS project_id;

ALTER TABLE product.discovered_interface
    ALTER COLUMN product_id DROP NOT NULL;

ALTER TABLE product.discovered_interface
    ADD COLUMN IF NOT EXISTS product_planned TEXT;

ALTER TABLE product.discovered_operation
    ADD COLUMN IF NOT EXISTS is_planned BOOLEAN NOT NULL DEFAULT FALSE;

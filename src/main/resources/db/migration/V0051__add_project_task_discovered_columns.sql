ALTER TABLE product.discovered_interface
    ADD COLUMN IF NOT EXISTS project_id INTEGER;

ALTER TABLE product.discovered_operation
    ADD COLUMN IF NOT EXISTS tc_code TEXT;

ALTER TABLE product.discovered_operation
    ADD COLUMN IF NOT EXISTS tc_description TEXT;

ALTER TABLE product.discovered_operation
    ADD COLUMN IF NOT EXISTS description TEXT;

-- description существует с V0016 как VARCHAR(250)/VARCHAR(500): ADD COLUMN выше — no-op,
-- а описание операции из ProjectTask в этот лимит не укладывается.
ALTER TABLE product.discovered_operation
    ALTER COLUMN description TYPE TEXT;

CREATE INDEX IF NOT EXISTS idx_discovered_interface_project_id
    ON product.discovered_interface (project_id)
    WHERE project_id IS NOT NULL;

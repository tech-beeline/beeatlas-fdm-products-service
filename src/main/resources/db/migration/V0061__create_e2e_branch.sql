-- SFDM-4250: ветка e2e по образцу product_branch. Карточка e2e одна на code, дерево вызовов
-- (operation_relations) висит на ветке, как контейнеры продукта на product_branch.

CREATE TABLE product.e2e_branch
(
    id          SERIAL PRIMARY KEY,
    code        text NOT NULL REFERENCES product.e2e (code),
    branch_name text NOT NULL DEFAULT 'main'
);

-- На этот индекс опирается ON CONFLICT (code, branch_name) в E2eBranchRepository.upsert.
CREATE UNIQUE INDEX uq_e2e_branch_code_branch_name
    ON product.e2e_branch (code, branch_name);

-- Запрет на ветки, различающиеся только регистром (main / Main).
CREATE UNIQUE INDEX uq_e2e_branch_code_lower_branch_name
    ON product.e2e_branch (code, lower(branch_name));

CREATE INDEX idx_e2e_branch_code ON product.e2e_branch (code);

-- Ветка main существующей карточки получает id карточки: прежний e2e_id связей становится e2e_branch_id.
INSERT INTO product.e2e_branch (id, code, branch_name)
SELECT id, code, 'main'
FROM product.e2e;

SELECT setval(
    pg_get_serial_sequence('product.e2e_branch', 'id'),
    (SELECT COALESCE(MAX(id), 1) FROM product.e2e_branch)
);

-- Связи без карточки: для них нет ветки main с тем же id, внешний ключ не создать.
DELETE FROM product.operation_relations r
WHERE NOT EXISTS (
    SELECT 1 FROM product.e2e e WHERE e.id = r.e2e_id
);

ALTER TABLE product.operation_relations
    ADD COLUMN e2e_branch_id integer;

UPDATE product.operation_relations
SET e2e_branch_id = e2e_id;

ALTER TABLE product.operation_relations
    ALTER COLUMN e2e_branch_id SET NOT NULL;

-- Без ON DELETE CASCADE, как containers_product.product_branch_id: связи ветки снимаются в той же
-- транзакции, что и ветка.
ALTER TABLE product.operation_relations
    ADD CONSTRAINT fk_operation_relations_e2e_branch
        FOREIGN KEY (e2e_branch_id) REFERENCES product.e2e_branch (id);

CREATE INDEX IF NOT EXISTS idx_operation_relations_e2e_branch_id
    ON product.operation_relations (e2e_branch_id);

ALTER TABLE product.operation_relations
    DROP CONSTRAINT IF EXISTS fk_operation_relations_e2e;

DROP INDEX IF EXISTS product.idx_operation_relations_e2e_id;

ALTER TABLE product.operation_relations
    DROP COLUMN IF EXISTS e2e_id;

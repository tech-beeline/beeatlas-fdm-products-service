-- SFDM-4244: последовательность вызовов e2e хранится только через product.discovered_operation,
-- у карточки e2e появляется необязательный продукт-владелец.

-- Владелец e2e: product.product.id. Удаление продукта обнуляет владельца, карточку e2e не удаляет.
ALTER TABLE product.e2e
    ADD COLUMN IF NOT EXISTS product_id INTEGER NULL;

ALTER TABLE product.e2e
    DROP CONSTRAINT IF EXISTS fk_e2e_product_owner;

ALTER TABLE product.e2e
    ADD CONSTRAINT fk_e2e_product_owner
        FOREIGN KEY (product_id) REFERENCES product.product (id) ON DELETE SET NULL;

CREATE INDEX IF NOT EXISTS idx_e2e_product_id
    ON product.e2e (product_id);

-- Связи на product.operation удаляются целиком, в том числе смешанные: id на discovered_operation
-- из них не переносятся.
DELETE FROM product.operation_relations
WHERE lower(entity_type_operation) = 'operation'
   OR lower(entity_type_operation_relation) = 'operation';

-- Связи с id вне discovered_operation удаляются, иначе внешний ключ не создать.
DELETE FROM product.operation_relations r
WHERE NOT EXISTS (
        SELECT 1 FROM product.discovered_operation d WHERE d.id = r.related_operation_id
    )
   OR (
        r.operation_id IS NOT NULL
        AND NOT EXISTS (
            SELECT 1 FROM product.discovered_operation d WHERE d.id = r.operation_id
        )
    );

ALTER TABLE product.operation_relations
    DROP COLUMN IF EXISTS entity_type_operation;

ALTER TABLE product.operation_relations
    DROP COLUMN IF EXISTS entity_type_operation_relation;

ALTER TABLE product.operation_relations
    DROP CONSTRAINT IF EXISTS fk_operation_relations_operation;

ALTER TABLE product.operation_relations
    DROP CONSTRAINT IF EXISTS fk_operation_relations_related_operation;

-- operation_id остаётся NULL у корня. ON DELETE RESTRICT: обнаруженный метод, который стоит в связи,
-- не удаляется вместе со связью и не обнуляет её.
ALTER TABLE product.operation_relations
    ADD CONSTRAINT fk_operation_relations_operation
        FOREIGN KEY (operation_id) REFERENCES product.discovered_operation (id) ON DELETE RESTRICT;

ALTER TABLE product.operation_relations
    ADD CONSTRAINT fk_operation_relations_related_operation
        FOREIGN KEY (related_operation_id) REFERENCES product.discovered_operation (id) ON DELETE RESTRICT;

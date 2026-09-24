ALTER TABLE product.containers_product RENAME COLUMN product_id TO product_branch_id;

ALTER TABLE product.containers_product DROP CONSTRAINT fk_containers_product_product;

ALTER TABLE product.containers_product
    ADD CONSTRAINT fk_containers_product_branch
        FOREIGN KEY (product_branch_id) REFERENCES product.product_branch (id);

DROP INDEX IF EXISTS ixfk_containers_product_product;

CREATE INDEX idx_containers_product_branch_id ON product.containers_product (product_branch_id);

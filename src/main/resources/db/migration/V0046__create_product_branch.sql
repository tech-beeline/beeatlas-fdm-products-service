ALTER TABLE product.product
    ALTER COLUMN alias TYPE text;

CREATE TABLE product.product_branch
(
    id          SERIAL PRIMARY KEY,
    alias       text NOT NULL REFERENCES product.product (alias),
    branch_name text NOT NULL DEFAULT 'main'
);

CREATE UNIQUE INDEX uq_product_branch_alias_branch_name
    ON product.product_branch (alias, branch_name);

CREATE INDEX idx_product_branch_alias ON product.product_branch (alias);

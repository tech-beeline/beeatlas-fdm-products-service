ALTER TABLE product.product DROP CONSTRAINT IF EXISTS fk_product_product_domain;
ALTER TABLE product.product DROP COLUMN IF EXISTS domain_id;

DROP TABLE IF EXISTS product.product_domain CASCADE;

CREATE TABLE product.product_domain (
    id        int4 NOT NULL,
    "name"    varchar(250) NOT NULL,
    alias     varchar(255) NOT NULL,
    description varchar(255) NULL,
    owner_id  int4 NULL,
    CONSTRAINT pk_product_domain PRIMARY KEY (id)
);

CREATE SEQUENCE IF NOT EXISTS product.product_domain_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER TABLE product.product_domain ALTER COLUMN id SET DEFAULT nextval('product.product_domain_id_seq');

ALTER TABLE product.product ADD COLUMN IF NOT EXISTS domain_id int4 NULL;

ALTER TABLE product.product ADD CONSTRAINT fk_product_product_domain
    FOREIGN KEY (domain_id) REFERENCES product.product_domain(id) ON DELETE SET NULL;

INSERT INTO product.product_domain (name, alias, description, owner_id)
VALUES (
    'Все приложения',
    'all_product',
    'Блок по умолчанию для всех приложений',
    NULL
);

UPDATE product.product
SET domain_id = (
    SELECT id FROM product.product_domain WHERE alias = 'all_product'
)
WHERE domain_id IS NULL;
DROP TABLE if exists product.product_domain;
CREATE TABLE product.product_domain (
                                        id int4 NOT NULL,
                                        "name" varchar(250) NOT NULL,
                                        alias varchar(255) NOT NULL,
                                        description varchar(255) NULL,
                                        owner_id int4 NULL,
                                        CONSTRAINT pk_product_domain PRIMARY KEY (id)
);
ALTER TABLE product.product ADD domain_id int4 NULL;
ALTER TABLE product.product ADD CONSTRAINT fk_product_product_domain
    FOREIGN KEY (domain_id)
        REFERENCES product.product_domain(id);

INSERT INTO product.product_domain (id, name, alias, description, owner_id)
VALUES (
           1,
           'Информационные технологии',
           'info_tech',
           'Блок для информационных технологий билайна',
           null
       );

UPDATE product.product
SET domain_id = 1
WHERE domain_id IS NULL;
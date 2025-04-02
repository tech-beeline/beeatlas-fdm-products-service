-- Создание схемы product (если её ещё нет)
CREATE SCHEMA IF NOT EXISTS product;

CREATE SEQUENCE IF NOT EXISTS product.seq_infra_id;
CREATE SEQUENCE IF NOT EXISTS product.seq_relations_id;
CREATE SEQUENCE IF NOT EXISTS product.seq_properties_id;


CREATE TABLE IF NOT EXISTS product.infra (
    id INTEGER NOT NULL DEFAULT nextval('product.seq_infra_id') PRIMARY KEY,
    product_id INTEGER NOT NULL,
    name TEXT NOT NULL,
    created_date TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    deleted_date TIMESTAMP WITHOUT TIME ZONE,
    last_modified_date TIMESTAMP WITHOUT TIME ZONE,
    type TEXT,
    cmdb_id TEXT UNIQUE,
    FOREIGN KEY (product_id) REFERENCES product.product(id) ON DELETE CASCADE
    );

CREATE TABLE IF NOT EXISTS product.relations (
    id INTEGER NOT NULL DEFAULT nextval('product.seq_relations_id') PRIMARY KEY,
    parent_id TEXT NOT NULL,
    child_id TEXT NOT NULL,
    created_date TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    deleted_date TIMESTAMP WITHOUT TIME ZONE,
    last_modified_date TIMESTAMP WITHOUT TIME ZONE,
    FOREIGN KEY (parent_id) REFERENCES product.infra(cmdb_id) ON DELETE CASCADE,
    FOREIGN KEY (child_id) REFERENCES product.infra(cmdb_id) ON DELETE CASCADE,
    UNIQUE (parent_id, child_id)
    );

CREATE TABLE IF NOT EXISTS product.properties (
    id INTEGER NOT NULL DEFAULT nextval('product.seq_properties_id') PRIMARY KEY,
    infra_id INTEGER NOT NULL,
    name TEXT NOT NULL,
    value TEXT,
    created_date TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    deleted_date TIMESTAMP WITHOUT TIME ZONE,
    last_modified_date TIMESTAMP WITHOUT TIME ZONE,
    FOREIGN KEY (infra_id) REFERENCES product.infra(id) ON DELETE CASCADE,
    UNIQUE (infra_id, name)
    );

-- Опционально: индексы для ускорения поиска
CREATE INDEX IF NOT EXISTS idx_infra_product_id ON product.infra(product_id);
CREATE INDEX IF NOT EXISTS idx_infra_type ON product.infra(type);
CREATE INDEX IF NOT EXISTS idx_infra_cmdb_id ON product.infra(cmdb_id);

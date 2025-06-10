-- Удаление всех таблиц и последовательностей из предыдущей версии
DROP TABLE IF EXISTS product.properties CASCADE;
DROP TABLE IF EXISTS product.relations CASCADE;
DROP TABLE IF EXISTS product.infra_product CASCADE;
DROP TABLE IF EXISTS product.infra CASCADE;

DROP SEQUENCE IF EXISTS product.seq_infra_id;
DROP SEQUENCE IF EXISTS product.seq_relations_id;
DROP SEQUENCE IF EXISTS product.seq_properties_id;
DROP SEQUENCE IF EXISTS product.seq_infra_product_id;

CREATE SCHEMA IF NOT EXISTS product;

-- Последовательности для автоинкремента id
CREATE SEQUENCE product.seq_infra_id;
CREATE SEQUENCE product.seq_relations_id;
CREATE SEQUENCE product.seq_properties_id;
CREATE SEQUENCE product.seq_infra_product_id;

-- Таблица infra (без ссылки на product)
CREATE TABLE product.infra (
                               id INTEGER NOT NULL DEFAULT nextval('product.seq_infra_id') PRIMARY KEY,
                               name TEXT NOT NULL,
                               created_date timestamp without time zone NOT NULL,
                               deleted_date timestamp without time zone,
                               last_modified_date timestamp without time zone,
                               type TEXT,
                               cmdb_id TEXT,
                               UNIQUE (cmdb_id)
);

-- Таблица связи infra и product (многие-ко-многим)
CREATE TABLE product.infra_product (
                                       id INTEGER NOT NULL DEFAULT nextval('product.seq_infra_product_id') PRIMARY KEY,
                                       product_id INTEGER NOT NULL,
                                       infra_id INTEGER NOT NULL,
                                       created_date timestamp without time zone NOT NULL,
                                       deleted_date timestamp without time zone,
                                       last_modified_date timestamp without time zone,

    -- Внешние ключи
                                       FOREIGN KEY (product_id) REFERENCES product.product(id) ON DELETE CASCADE,
                                       FOREIGN KEY (infra_id) REFERENCES product.infra(id) ON DELETE CASCADE,

    -- Уникальное ограничение, чтобы избежать дублирования связей
                                       UNIQUE (product_id, infra_id)
);

-- Таблица relations (для иерархии infra)
CREATE TABLE product.relations (
                                   id INTEGER NOT NULL DEFAULT nextval('product.seq_relations_id') PRIMARY KEY,
                                   parent_id TEXT NOT NULL,
                                   child_id TEXT NOT NULL,
                                   created_date timestamp without time zone NOT NULL,
                                   deleted_date timestamp without time zone,
                                   last_modified_date timestamp without time zone,

    -- Внешние ключи на таблицу infra
                                   FOREIGN KEY (parent_id) REFERENCES product.infra(cmdb_id) ON DELETE CASCADE,
                                   FOREIGN KEY (child_id) REFERENCES product.infra(cmdb_id) ON DELETE CASCADE,

    -- Уникальное ограничение, чтобы избежать дублирования связей
                                   UNIQUE (parent_id, child_id)
);

-- Таблица properties
CREATE TABLE product.properties (
                                    id INTEGER NOT NULL DEFAULT nextval('product.seq_properties_id') PRIMARY KEY,
                                    infra_id INTEGER NOT NULL,
                                    name TEXT NOT NULL,
                                    value TEXT,
                                    created_date timestamp without time zone NOT NULL,
                                    deleted_date timestamp without time zone,
                                    last_modified_date timestamp without time zone,

    -- Внешний ключ на таблицу infra
                                    FOREIGN KEY (infra_id) REFERENCES product.infra(id) ON DELETE CASCADE,

    -- Уникальное ограничение: одна инфраструктура не может иметь два свойства с одинаковым именем
                                    UNIQUE (infra_id, name)
);

-- Индексы для ускорения поиска
CREATE INDEX idx_infra_type ON product.infra(type);
CREATE INDEX idx_infra_cmdb_id ON product.infra(cmdb_id);
CREATE INDEX idx_infra_product_product_id ON product.infra_product(product_id);
CREATE INDEX idx_infra_product_infra_id ON product.infra_product(infra_id);
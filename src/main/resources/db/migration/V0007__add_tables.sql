-- Создание схемы product (если её ещё нет)
CREATE SCHEMA IF NOT EXISTS product;-- Последовательности для автоинкремента id
CREATE SEQUENCE product.seq_infra_id;
CREATE SEQUENCE product.seq_relations_id;
CREATE SEQUENCE product.seq_properties_id;-- Таблица infra
CREATE TABLE product.infra (
                               id INTEGER NOT NULL DEFAULT nextval('product.seq_infra_id') PRIMARY KEY,
                               product_id INTEGER NOT NULL,  -- FK на product (если таблица product существует)
                               name TEXT NOT NULL,
                               created_date timestamp without time zone NOT NULL,
                               deleted_date timestamp without time zone,
                               last_modified_date timestamp without time zone,
                               type TEXT,
                               cmdb_id TEXT,
                               FOREIGN KEY (product_id) REFERENCES product.product(id) ON DELETE CASCADE,
                               UNIQUE (cmdb_id) );-- Таблица relations (для иерархии infra)
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
);-- Таблица properties
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
                                    UNIQUE (infra, name)
);-- Опционально: индекс для ускорения поиска по product_id в infra
CREATE INDEX idx_infra_product_id ON product.infra(product_id);-- Опционально: индекс для ускорения поиска по type в infra
CREATE INDEX idx_infra_type ON product.infra(type);
CREATE INDEX idx_infra_cmdb_id ON product.infra(cmdb_id); 
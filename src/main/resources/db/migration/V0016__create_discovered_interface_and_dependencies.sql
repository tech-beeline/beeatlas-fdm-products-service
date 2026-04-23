CREATE SCHEMA IF NOT EXISTS product;
CREATE SEQUENCE product.discovered_interface_id_seq;
CREATE TABLE product.discovered_interface
(
    id           INTEGER      NOT NULL DEFAULT nextval('product.discovered_interface_id_seq'),
    name         VARCHAR(150) NOT NULL,
    external_id  INTEGER      NOT NULL,
    api_id       INTEGER      NOT NULL,
    api_link     VARCHAR(150),
    version      VARCHAR(50),
    description  VARCHAR(250),
    status       TEXT         NOT NULL,
    context      TEXT         NOT NULL,
    product_id   INTEGER      NOT NULL,
    created_date TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    deleted_date TIMESTAMP WITHOUT TIME ZONE,
    updated_date TIMESTAMP WITHOUT TIME ZONE,
    PRIMARY KEY (id),
    CONSTRAINT fk_discovered_interface_product FOREIGN KEY (product_id) REFERENCES product.product (id)
);
ALTER SEQUENCE product.discovered_interface_id_seq OWNED BY product.discovered_interface.id;
CREATE SEQUENCE product.discovered_operation_id_seq;
CREATE TABLE product.discovered_operation
(
    id           INTEGER      NOT NULL DEFAULT nextval('product.discovered_operation_id_seq'),
    interface_id INTEGER      NOT NULL,
    name         VARCHAR(100) NOT NULL,
    context      TEXT,
    description  VARCHAR(250),
    type         VARCHAR(50),
    return_type  VARCHAR(100),
    created_date TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_date TIMESTAMP WITHOUT TIME ZONE,
    deleted_date TIMESTAMP WITHOUT TIME ZONE,
    PRIMARY KEY (id),
    CONSTRAINT fk_discovered_operation_interface FOREIGN KEY (interface_id) REFERENCES product.discovered_interface (id)
);
ALTER SEQUENCE product.discovered_operation_id_seq OWNED BY product.discovered_operation.id;
CREATE SEQUENCE product.discovered_parameter_id_seq;
CREATE TABLE product.discovered_parameter
(
    id             INTEGER      NOT NULL DEFAULT nextval('product.discovered_parameter_id_seq'),
    operation_id   INTEGER      NOT NULL,
    parameter_name VARCHAR(100) NOT NULL,
    parameter_type VARCHAR(100) NOT NULL,
    created_date   TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    deleted_date   TIMESTAMP WITHOUT TIME ZONE,
    PRIMARY KEY (id),
    CONSTRAINT fk_discovered_parameter_operation FOREIGN KEY (operation_id) REFERENCES product.discovered_operation (id)
);
ALTER SEQUENCE product.discovered_parameter_id_seq OWNED BY product.discovered_parameter.id;
CREATE INDEX idx_discovered_interface_product_id ON product.discovered_interface (product_id);
CREATE INDEX idx_discovered_operation_interface_id ON product.discovered_operation (interface_id);
CREATE INDEX idx_discovered_parameter_operation_id ON product.discovered_parameter (operation_id);
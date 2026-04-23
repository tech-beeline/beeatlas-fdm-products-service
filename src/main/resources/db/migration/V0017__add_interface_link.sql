ALTER TABLE product.discovered_interface
    ADD COLUMN connection_interface_id INTEGER;

ALTER TABLE product.discovered_interface
    ADD CONSTRAINT fk_discovered_interface_connection
        FOREIGN KEY (connection_interface_id)
            REFERENCES product.interface(id);

ALTER TABLE product.discovered_operation
    ADD COLUMN connection_operation_id INTEGER;

ALTER TABLE product.discovered_operation
    ADD CONSTRAINT fk_discovered_operation_connection
        FOREIGN KEY (connection_operation_id)
            REFERENCES product.operation(id);
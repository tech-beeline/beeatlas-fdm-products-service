-- SFDM-4318: связи discovered с архитектурой веток develop и design, по образцу V0017.
-- connection_*_id остаются связью ветки main; существующие строки не обновляются, новые колонки пустые.

ALTER TABLE product.discovered_interface
    ADD COLUMN connection_interface_dev_id INTEGER;

ALTER TABLE product.discovered_interface
    ADD CONSTRAINT fk_discovered_interface_connection_dev
        FOREIGN KEY (connection_interface_dev_id)
            REFERENCES product.interface(id);

ALTER TABLE product.discovered_interface
    ADD COLUMN connection_interface_design_id INTEGER;

ALTER TABLE product.discovered_interface
    ADD CONSTRAINT fk_discovered_interface_connection_design
        FOREIGN KEY (connection_interface_design_id)
            REFERENCES product.interface(id);

ALTER TABLE product.discovered_operation
    ADD COLUMN connection_operation_dev_id INTEGER;

ALTER TABLE product.discovered_operation
    ADD CONSTRAINT fk_discovered_operation_connection_dev
        FOREIGN KEY (connection_operation_dev_id)
            REFERENCES product.operation(id);

ALTER TABLE product.discovered_operation
    ADD COLUMN connection_operation_design_id INTEGER;

ALTER TABLE product.discovered_operation
    ADD CONSTRAINT fk_discovered_operation_connection_design
        FOREIGN KEY (connection_operation_design_id)
            REFERENCES product.operation(id);

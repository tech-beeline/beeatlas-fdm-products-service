
CREATE INDEX idx_operation_name_type ON operation(name, type) WHERE deleted_date IS NULL;

CREATE INDEX idx_discovered_operation_name_type ON discovered_operation(name, type) WHERE deleted_date IS NULL;
ALTER TABLE product.discovered_interface ADD COLUMN source_metric TEXT;

COMMENT ON COLUMN product.discovered_interface.source_metric IS
    'URL источника метрик Grafana. Пишется PUT /api/v1/source-metric?entity=discovered-interface.';

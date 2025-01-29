CREATE SEQUENCE IF NOT EXISTS product.sequence_service_id;


CREATE TABLE IF NOT EXISTS product.service
(
    id INTEGER PRIMARY KEY DEFAULT nextval('product.sequence_service_id'),
    api_key TEXT NOT NULL,
    api_secret TEXT NOT NULL
    );
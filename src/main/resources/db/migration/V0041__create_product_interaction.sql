CREATE SEQUENCE IF NOT EXISTS product.product_interaction_id_seq;

CREATE TABLE IF NOT EXISTS product.product_interaction
(
    id              INTEGER                     NOT NULL DEFAULT nextval('product.product_interaction_id_seq'),
    provider_alias  TEXT                        NOT NULL,
    consumer_alias  TEXT                        NOT NULL,
    source          TEXT                        NOT NULL,
    created_at      TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
);

ALTER SEQUENCE product.product_interaction_id_seq OWNED BY product.product_interaction.id;

-- Уникальность пары без учёта регистра (как сравнение alias в остальных методах сервиса)
CREATE UNIQUE INDEX IF NOT EXISTS uq_product_interaction_provider_consumer
    ON product.product_interaction (LOWER(provider_alias), LOWER(consumer_alias));

CREATE INDEX IF NOT EXISTS idx_product_interaction_provider_alias
    ON product.product_interaction (provider_alias);

CREATE INDEX IF NOT EXISTS idx_product_interaction_consumer_alias
    ON product.product_interaction (consumer_alias);

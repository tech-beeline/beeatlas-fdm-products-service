-- Уникальность product.alias нужна для внешнего FK из bi_dashboards.ff (сервис ff-manager),
-- ссылающегося на product.product (alias).
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'uq_product_alias'
    ) THEN
        ALTER TABLE product.product ADD CONSTRAINT uq_product_alias UNIQUE (alias);
    END IF;
END $$;

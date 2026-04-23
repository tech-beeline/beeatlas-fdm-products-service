DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'product'
        AND table_name = 'operation'
        AND column_name = 'is_deleted_tc'
    ) THEN
ALTER TABLE product.operation
    ADD COLUMN is_deleted_tc BOOLEAN;
END IF;
END $$;
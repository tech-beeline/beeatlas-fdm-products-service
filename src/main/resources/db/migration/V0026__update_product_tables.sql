ALTER TABLE product.containers_product
    ALTER COLUMN name TYPE TEXT,
    ALTER COLUMN code TYPE TEXT,
    ALTER COLUMN version TYPE TEXT;
ALTER TABLE product.interface
    ALTER COLUMN name TYPE TEXT,
    ALTER COLUMN code TYPE TEXT,
    ALTER COLUMN spec_link TYPE TEXT,
    ALTER COLUMN version TYPE TEXT,
    ALTER COLUMN description TYPE TEXT,
    ALTER COLUMN protocol TYPE TEXT;
ALTER TABLE product.operation
    ALTER COLUMN name TYPE TEXT,
    ALTER COLUMN type TYPE TEXT,
    ALTER COLUMN return_type TYPE TEXT,
    ALTER COLUMN description TYPE TEXT;
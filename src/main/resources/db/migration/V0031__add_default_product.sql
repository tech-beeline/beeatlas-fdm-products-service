DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM product.product LIMIT 1) THEN
        INSERT INTO product.product (id, name, alias)
        VALUES (0, 'default product', 'dflt');

INSERT INTO product.user_product (id, user_id, product_id)
VALUES (0, 0, 0);
END IF;
END $$;
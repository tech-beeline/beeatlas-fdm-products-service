INSERT INTO product.product_branch (id, alias, branch_name)
SELECT id, alias, 'main'
FROM product.product;

SELECT setval(
    pg_get_serial_sequence('product.product_branch', 'id'),
    (SELECT MAX(id) FROM product.product_branch)
);

INSERT INTO product.product_branch (alias, branch_name)
SELECT p.alias, 'main'
FROM product.product p
WHERE EXISTS (SELECT 1 FROM product.non_functional_requirement nfr WHERE nfr.product_id = p.id)
  AND NOT EXISTS (SELECT 1
                  FROM product.product_branch pb
                  WHERE pb.alias = p.alias
                    AND lower(pb.branch_name) = 'main');

ALTER TABLE product.non_functional_requirement
    ADD COLUMN product_branch_id int;

UPDATE product.non_functional_requirement nfr
SET product_branch_id = pb.id
FROM product.product p
         JOIN product.product_branch pb ON pb.alias = p.alias AND lower(pb.branch_name) = 'main'
WHERE p.id = nfr.product_id;

DELETE
FROM product.non_functional_requirement dup
    USING product.non_functional_requirement kept
WHERE dup.product_branch_id = kept.product_branch_id
  AND dup.nfr_id = kept.nfr_id
  AND dup.id > kept.id;

DROP INDEX IF EXISTS product.idx_non_functional_requirement_product_id;

ALTER TABLE product.non_functional_requirement
    DROP COLUMN product_id;

ALTER TABLE product.non_functional_requirement
    ADD CONSTRAINT fk_non_functional_requirement_product_branch
        FOREIGN KEY (product_branch_id) REFERENCES product.product_branch (id);

CREATE UNIQUE INDEX uq_non_functional_requirement_branch_nfr
    ON product.non_functional_requirement (product_branch_id, nfr_id);

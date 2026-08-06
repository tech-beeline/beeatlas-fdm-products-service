ALTER TABLE product.local_assessment
    ADD CONSTRAINT uk_local_assessment_source_product UNIQUE (source_id, product_id);
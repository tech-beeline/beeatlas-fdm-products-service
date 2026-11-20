ALTER TABLE product.non_functional_requirement_enum
    ADD COLUMN created_date timestamp without time zone NOT NULL DEFAULT now();

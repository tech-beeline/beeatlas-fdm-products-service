ALTER TABLE tech_product ADD COLUMN source VARCHAR(255) NULL;
ALTER TABLE tech_product ADD COLUMN created_date timestamp without time zone NULL;
ALTER TABLE tech_product ADD COLUMN deleted_date timestamp without time zone NULL;
ALTER TABLE tech_product ADD COLUMN last_modified_date timestamp without time zone NULL;

UPDATE tech_product SET source = 'git' WHERE source IS NULL;
UPDATE tech_product SET created_date = NOW() WHERE created_date IS NULL;


CREATE TABLE enum_source_type (
                                  id INTEGER PRIMARY KEY,
                                  name TEXT NOT NULL,
                                  description TEXT,
                                  identify_source BOOLEAN NOT NULL
);

INSERT INTO enum_source_type (id, name, description, identify_source)
VALUES (1, 'pipeline', 'Обновление фитнес-функций по итогам публикации архитектуры', true);

INSERT INTO enum_source_type (id, name, description, identify_source)
VALUES (2, 'script', 'Автоматическое обновление фитнес-функций', false);

ALTER TABLE product.local_assessment
    ADD COLUMN source_type_id INTEGER;

UPDATE product.local_assessment
SET source_type_id = 1
WHERE source_type_id IS NULL;

ALTER TABLE product.local_assessment
    ADD CONSTRAINT fk_local_assessment_source_type
        FOREIGN KEY (source_type_id) REFERENCES enum_source_type(id);


ALTER TABLE product.local_assessment
    ALTER COLUMN source_id DROP NOT NULL;

-- Создание сиквенса seq_assessment_check_id
CREATE SEQUENCE seq_assessment_check_id
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;-- Создание сиквенса seq_assessment_id
CREATE SEQUENCE seq_assessment_id
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;-- Создание таблицы local_fitness_function в схеме product
CREATE TABLE product.local_fitness_function (
                                                id INT PRIMARY KEY,
                                                code TEXT NOT NULL UNIQUE,
                                                description TEXT NOT NULL,
                                                status TEXT NOT NULL
);-- Создание таблицы local_assessment в схеме product
CREATE TABLE product.local_assessment (
                                          id INT PRIMARY KEY DEFAULT nextval('seq_assessment_id'),
                                          source_id INT NOT NULL,
                                          product_id INT NOT NULL REFERENCES product.product(id),
                                          created_time TIMESTAMP WITHOUT TIME ZONE
);-- Создание таблицы local_assessment_check в схеме product
CREATE TABLE product.local_assessment_check (
                                                id INT PRIMARY KEY DEFAULT nextval('seq_assessment_check_id'),
                                                lff_id INT NOT NULL REFERENCES product.local_fitness_function(id),
                                                assessment_id INT NOT NULL REFERENCES product.local_assessment(id),
                                                is_check BOOLEAN NOT NULL,
                                                result_details TEXT
);

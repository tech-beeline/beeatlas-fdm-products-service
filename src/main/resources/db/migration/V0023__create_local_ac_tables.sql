ALTER TABLE product.local_assessment_check
    ADD COLUMN assessment_description TEXT;

ALTER TABLE product.local_fitness_function
    ADD COLUMN doc_link TEXT;

CREATE SEQUENCE product.seq_local_ac_object_id
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE CACHE 1;

CREATE TABLE product.local_ac_object
(
    id     INTEGER PRIMARY KEY DEFAULT nextval('product.seq_local_ac_object_id'),
    lac_id INTEGER NOT NULL,
    is_check BOOLEAN,
    CONSTRAINT fk_local_ac_object_lac_id
        FOREIGN KEY (lac_id)
            REFERENCES product.local_assessment_check (id)
);

CREATE SEQUENCE product.seq_local_ac_object_detail_id
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE CACHE 1;

CREATE TABLE product.local_ac_object_detail
(
    id      INTEGER PRIMARY KEY DEFAULT nextval('product.seq_local_ac_object_detail_id'),
    laco_id INTEGER NOT NULL,
    key     TEXT    NOT NULL,
    value   TEXT,
    CONSTRAINT fk_local_ac_object_detail_laco_id
        FOREIGN KEY (laco_id)
            REFERENCES product.local_ac_object (id)
);

CREATE INDEX idx_local_ac_object_lac_id ON product.local_ac_object (lac_id);

CREATE INDEX idx_local_ac_object_detail_laco_id ON product.local_ac_object_detail (laco_id);

CREATE INDEX idx_local_ac_object_detail_key ON product.local_ac_object_detail (key);
CREATE SEQUENCE IF NOT EXISTS product.non_functional_requirenment_id_seq;
CREATE SEQUENCE IF NOT EXISTS product.non_functional_requirenment_enum_id_seq;
CREATE SEQUENCE IF NOT EXISTS product.non_functional_requirenment_enum_core_id_seq;
CREATE SEQUENCE IF NOT EXISTS product.chapter_id_seq;
CREATE SEQUENCE IF NOT EXISTS product.chapter_nfr_id_seq;
CREATE SEQUENCE IF NOT EXISTS product.pettern_requirement_id_seq;

CREATE TABLE IF NOT EXISTS product.non_functional_requirenment_enum_core (
                                                                             id int PRIMARY KEY DEFAULT nextval('product.non_functional_requirenment_enum_core_id_seq'),
    code text,
    source text
    );

CREATE TABLE IF NOT EXISTS product.non_functional_requirenment_enum (
                                                                        id int PRIMARY KEY DEFAULT nextval('product.non_functional_requirenment_enum_id_seq'),
    name text,
    description text,
    rule text,
    version int,
    core_id int REFERENCES product.non_functional_requirenment_enum_core(id)
    );

CREATE TABLE IF NOT EXISTS product.non_functional_requirenment (
                                                                   id int PRIMARY KEY DEFAULT nextval('product.non_functional_requirenment_id_seq'),
    product_id int REFERENCES product.product(id),
    nfr_id int REFERENCES product.non_functional_requirenment_enum(id),
    source text
    );

CREATE TABLE IF NOT EXISTS product.pattern_check (
                                                     id int PRIMARY KEY DEFAULT nextval('product.pattern_check_id_seq'),
    product_id int NOT NULL REFERENCES product.product(id),
    pattern_code text NOT NULL,
    is_check boolean NOT NULL,
    result_details text,
    create_date timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (pattern_code) REFERENCES product.pattern(code)
    );

CREATE TABLE IF NOT EXISTS product.chapter (
                                               id int PRIMARY KEY DEFAULT nextval('product.chapter_id_seq'),
    name text,
    description text,
    doc_link text
    );

CREATE TABLE IF NOT EXISTS product.chapter_nfr (
                                                   id int PRIMARY KEY DEFAULT nextval('product.chapter_nfr_id_seq'),
    chapter_id int REFERENCES product.chapter(id),
    nfr_id int REFERENCES product.non_functional_requirenment_enum(id)
    );

CREATE TABLE IF NOT EXISTS product.pettern_requirement (
                                                           id int PRIMARY KEY DEFAULT nextval('product.pettern_requirement_id_seq'),
    pattern_id int NOT NULL REFERENCES product.pattern(id),
    nfr_id int NOT NULL REFERENCES product.non_functional_requirenment_enum(id),
    CONSTRAINT unique_pattern_nfr UNIQUE (pattern_id, nfr_id)
    );

CREATE INDEX IF NOT EXISTS idx_non_functional_requirenment_product_id ON product.non_functional_requirenment(product_id);
CREATE INDEX IF NOT EXISTS idx_non_functional_requirenment_nfr_id ON product.non_functional_requirenment(nfr_id);
CREATE INDEX IF NOT EXISTS idx_non_functional_requirenment_enum_core_id ON product.non_functional_requirenment_enum(core_id);
CREATE INDEX IF NOT EXISTS idx_pattern_check_product_id ON product.pattern_check(product_id);
CREATE INDEX IF NOT EXISTS idx_pattern_check_pattern_code ON product.pattern_check(pattern_code);
CREATE INDEX IF NOT EXISTS idx_chapter_nfr_chapter_id ON product.chapter_nfr(chapter_id);
CREATE INDEX IF NOT EXISTS idx_chapter_nfr_nfr_id ON product.chapter_nfr(nfr_id);
CREATE INDEX IF NOT EXISTS idx_pettern_requirement_pattern_id ON product.pettern_requirement(pattern_id);
CREATE INDEX IF NOT EXISTS idx_pettern_requirement_nfr_id ON product.pettern_requirement(nfr_id);
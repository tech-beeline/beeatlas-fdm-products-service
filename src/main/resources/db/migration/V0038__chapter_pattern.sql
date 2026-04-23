CREATE SEQUENCE IF NOT EXISTS product.chapter_pattern_id_seq;

CREATE TABLE IF NOT EXISTS product.chapter_pattern (
    id INTEGER PRIMARY KEY DEFAULT nextval('product.chapter_pattern_id_seq'),
    chapter_id INTEGER NOT NULL,
    pattern_id INTEGER NOT NULL
);

ALTER SEQUENCE product.chapter_pattern_id_seq OWNED BY product.chapter_pattern.id;

CREATE INDEX IF NOT EXISTS idx_chapter_pattern_chapter_id ON product.chapter_pattern(chapter_id);

CREATE TABLE product.product_availability (
                                              id INTEGER PRIMARY KEY,
                                              product_id INTEGER NOT NULL,
                                              availability BOOLEAN NOT NULL,
                                              created_date TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

                                              CONSTRAINT fk_product
                                                  FOREIGN KEY (product_id)
                                                      REFERENCES product.product(id)
                                                      ON DELETE CASCADE
); 
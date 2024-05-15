/* Create Tables */

CREATE TABLE product.interface
(
    id integer NOT NULL,
    name varchar(150) NULL,
    description varchar(150) NULL,
    link varchar(150) NULL
)
;

CREATE TABLE product.product
(
    id integer NOT NULL,
    name varchar(250) NOT NULL,
    alias varchar(50) NOT NULL
)
;

CREATE TABLE product.product_interface
(
    id integer NOT NULL,
    product_id integer NOT NULL,
    interface_id integer NOT NULL
)
;

CREATE TABLE product.user_product
(
    id integer NOT NULL,
    user_id integer NOT NULL,
    product_id integer NOT NULL
)
;

/* Create Primary Keys, Indexes, Uniques, Checks */

ALTER TABLE product."interface" ADD CONSTRAINT pk_interface
    PRIMARY KEY (id)
;

ALTER TABLE product.product ADD CONSTRAINT pk_product
    PRIMARY KEY (id)
;

ALTER TABLE product.product_interface ADD CONSTRAINT "pk_Table1"
    PRIMARY KEY (id)
;

CREATE INDEX ixfk_product_interface_interface ON product.product_interface (interface_id ASC)
;

CREATE INDEX ixfk_product_interface_product ON product.product_interface (product_id ASC)
;

ALTER TABLE product.user_product ADD CONSTRAINT pk_user_product
    PRIMARY KEY (id)
;

CREATE INDEX ixfk_user_product_product ON product.user_product (product_id ASC)
;

/* Create Foreign Key Constraints */

ALTER TABLE product.product_interface ADD CONSTRAINT fk_product_interface_interface
    FOREIGN KEY (interface_id) REFERENCES product.interface (id) ON DELETE No Action ON UPDATE No Action
;

ALTER TABLE product.product_interface ADD CONSTRAINT fk_product_interface_product
    FOREIGN KEY (product_id) REFERENCES product.product (id) ON DELETE No Action ON UPDATE No Action
;

ALTER TABLE product.user_product ADD CONSTRAINT fk_user_product_product
    FOREIGN KEY (product_id) REFERENCES product.product (id) ON DELETE No Action ON UPDATE No Action
;

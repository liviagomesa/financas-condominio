CREATE TABLE supplier (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    unit_id BIGINT NULL REFERENCES unit (id),
    pix_key VARCHAR(255) NULL
);

CREATE INDEX supplier_unit_id_idx ON supplier (unit_id);

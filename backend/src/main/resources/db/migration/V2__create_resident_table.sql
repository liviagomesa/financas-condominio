CREATE TABLE resident (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    unit_id BIGINT NOT NULL REFERENCES unit (id),
    email VARCHAR(255),
    phone VARCHAR(20)
);

CREATE INDEX resident_unit_id_idx ON resident (unit_id);

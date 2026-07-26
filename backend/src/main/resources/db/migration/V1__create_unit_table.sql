CREATE TABLE unit (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    identifier VARCHAR(255) NOT NULL
);

CREATE UNIQUE INDEX unit_identifier_normalized_idx ON unit (LOWER(TRIM(identifier)));

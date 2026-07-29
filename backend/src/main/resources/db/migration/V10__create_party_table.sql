CREATE TABLE party (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    pix_key VARCHAR(255)
);

CREATE UNIQUE INDEX party_name_normalized_idx ON party (LOWER(TRIM(name)));

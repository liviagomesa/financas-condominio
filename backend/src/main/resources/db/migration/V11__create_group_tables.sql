CREATE TABLE party_group (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name VARCHAR(255) NOT NULL
);

CREATE UNIQUE INDEX party_group_name_normalized_idx ON party_group (LOWER(TRIM(name)));

CREATE TABLE party_group_member (
    group_id BIGINT NOT NULL REFERENCES party_group (id) ON DELETE CASCADE,
    party_id BIGINT NOT NULL REFERENCES party (id) ON DELETE CASCADE,
    PRIMARY KEY (group_id, party_id)
);

CREATE INDEX party_group_member_party_id_idx ON party_group_member (party_id);

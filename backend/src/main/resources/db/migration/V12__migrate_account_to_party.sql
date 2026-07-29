ALTER TABLE account DROP CONSTRAINT account_type_counterparty_check;

TRUNCATE TABLE account;

ALTER TABLE account DROP COLUMN unit_id;
ALTER TABLE account DROP COLUMN supplier_id;
ALTER TABLE account ADD COLUMN party_id BIGINT NOT NULL REFERENCES party (id);

CREATE INDEX account_party_id_idx ON account (party_id);

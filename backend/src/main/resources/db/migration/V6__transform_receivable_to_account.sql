ALTER TABLE receivable RENAME TO account;
ALTER INDEX receivable_unit_id_idx RENAME TO account_unit_id_idx;

ALTER TABLE account RENAME COLUMN target_account TO fund;
ALTER TABLE account ALTER COLUMN unit_id DROP NOT NULL;

ALTER TABLE account ADD COLUMN type VARCHAR(20) NOT NULL DEFAULT 'RECEIVABLE';
ALTER TABLE account ALTER COLUMN type DROP DEFAULT;

ALTER TABLE account ADD COLUMN supplier_id BIGINT NULL REFERENCES supplier (id);
ALTER TABLE account ADD COLUMN observations TEXT NULL;

CREATE INDEX account_supplier_id_idx ON account (supplier_id);

ALTER TABLE account ADD CONSTRAINT account_type_counterparty_check CHECK (
    (type = 'RECEIVABLE' AND unit_id IS NOT NULL AND supplier_id IS NULL)
    OR (type = 'PAYABLE' AND supplier_id IS NOT NULL AND unit_id IS NULL)
);

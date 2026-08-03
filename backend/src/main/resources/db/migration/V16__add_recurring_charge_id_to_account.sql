ALTER TABLE account ADD COLUMN recurring_charge_id BIGINT REFERENCES recurring_charge (id);

CREATE INDEX account_recurring_charge_id_idx ON account (recurring_charge_id);

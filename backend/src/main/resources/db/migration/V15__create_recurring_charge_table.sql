CREATE TABLE recurring_charge (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    type VARCHAR(20) NOT NULL,
    amount NUMERIC(10,2) NOT NULL CHECK (amount >= 0),
    due_day INTEGER NOT NULL CHECK (due_day BETWEEN 1 AND 31),
    description VARCHAR(255) NOT NULL,
    fund_id BIGINT NOT NULL REFERENCES fund (id),
    party_id BIGINT NOT NULL REFERENCES party (id),
    observations TEXT,
    deactivated_at DATE,
    last_generation_failed BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX recurring_charge_fund_id_idx ON recurring_charge (fund_id);
CREATE INDEX recurring_charge_party_id_idx ON recurring_charge (party_id);
CREATE INDEX recurring_charge_deactivated_at_idx ON recurring_charge (deactivated_at);

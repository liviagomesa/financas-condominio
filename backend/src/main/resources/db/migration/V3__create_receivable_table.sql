CREATE TABLE receivable (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    amount NUMERIC(10,2) NOT NULL,
    due_date DATE NOT NULL,
    description VARCHAR(255) NOT NULL,
    target_account VARCHAR(20) NOT NULL,
    recurring BOOLEAN NOT NULL,
    unit_id BIGINT NOT NULL REFERENCES unit (id)
);

CREATE INDEX receivable_unit_id_idx ON receivable (unit_id);

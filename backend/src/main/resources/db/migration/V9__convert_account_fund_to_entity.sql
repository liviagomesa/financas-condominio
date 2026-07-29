TRUNCATE TABLE account;

ALTER TABLE account DROP COLUMN fund;
ALTER TABLE account ADD COLUMN fund_id BIGINT NOT NULL REFERENCES fund (id);

CREATE INDEX account_fund_id_idx ON account (fund_id);

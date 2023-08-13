-- Database: external_api_db

--company
DROP TABLE IF EXISTS company;
create TABLE IF NOT EXISTS company (
    id serial PRIMARY KEY,
    symbol VARCHAR(255) not null
    );

--stock
create TABLE IF NOT EXISTS stock (
    id serial PRIMARY KEY,
    symbol VARCHAR(255) null null,
    change NUMERIC(38,2),
    latest_price NUMERIC(38,2)NOT NULL,
    delta_price numeric(5,2) default 0.00,
    previous_volume INT NOT NULL,
    volume INT,
    company_name VARCHAR(255) NOT NULL
    );

--stock_audit_log
CREATE TABLE IF NOT EXISTS stock_audit_log (
    id int8 NOT NULL,
    symbol VARCHAR(255) NOT NULL,
    old_row_data jsonb,
    new_row_data jsonb,
    dml_type dml_type NOT NULL,
    dml_timestamp timestamp NOT NULL
);
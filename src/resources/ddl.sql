CREATE TABLE IF NOT EXISTS account (
    account_id SERIAL PRIMARY KEY,
    owner_name VARCHAR(150) UNIQUE,
    balance DOUBLE PRECISION NOT NULL DEFAULT 0.0
);
CREATE TABLE IF NOT EXISTS transactions (
    transaction_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    account_id INT NOT NULL,
    type VARCHAR(50) NOT NULL,
    amount DOUBLE PRECISION NOT NULL,
    timestamp TIMESTAMP NOT NULL,  
    FOREIGN KEY(account_id) REFERENCES account(account_id)
);
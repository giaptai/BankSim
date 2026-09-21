INSERT INTO
    account (owner_name, balance)
VALUES
    ('minhthu', 10),
    ('phamduy', 10),
    ('davidlew', 10),
    ('mibiter', 10),
    ('tranthanh', 10) 
ON CONFLICT (owner_name) DO NOTHING;


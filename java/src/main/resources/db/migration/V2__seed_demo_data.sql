-- Demo data for local runs. Ids are assigned by the identity column in insert order (1..8).
-- Days cover the interest boundaries: 30/31 (grace period), 45/46 (premium start), 365/366 (one year).

insert into time_deposits (plan_type, days, balance) values
    ('basic',   30,   1000.00),
    ('basic',   31,   2500.50),
    ('student', 365,  1800.75),
    ('student', 366,  1200.00),
    ('premium', 45,   10000.00),
    ('premium', 46,   25000.25),
    ('premium', 365,  5000.00),
    ('basic',   1200, 75000.00);

insert into withdrawals (time_deposit_id, amount, date) values
    (2, 100.00,  date '2026-01-15'),
    (2, 50.25,   date '2026-03-02'),
    (6, 1500.00, date '2026-02-10'),
    (8, 300.00,  date '2026-04-20');

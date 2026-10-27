-- Run manually only after Flyway V1-V3 on an isolated PostgreSQL dev/test database.
-- Fixed high IDs avoid consuming normal generated identifiers.
DO $$
BEGIN
    IF current_database() NOT LIKE '%dev%' AND current_database() NOT LIKE '%test%' THEN
        RAISE EXCEPTION 'Billing sample data is restricted to dev/test databases';
    END IF;
END $$;

INSERT INTO rooms (room_id) VALUES (900001) ON CONFLICT (room_id) DO NOTHING;

INSERT INTO contracts (contract_id, room_id, start_date, end_date, monthly_rent, status)
VALUES (900002, 900001, '2026-01-01 00:00:00', '2026-12-31 23:59:59', 1500000, 'ACTIVE')
ON CONFLICT (contract_id) DO NOTHING;

INSERT INTO meter_readings (reading_id, room_id, reading_month,
    electricity_old, electricity_new, water_old, water_new,
    electricity_price, water_price, status, created_at)
VALUES (900003, 900001, '2026-10-01', 100, 120, 10, 13, 3500, 12000,
    'CONFIRMED', '2026-10-31 12:00:00')
ON CONFLICT (reading_id) DO NOTHING;

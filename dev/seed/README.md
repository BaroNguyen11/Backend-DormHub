# DormHub development/test seed

File: [`dormhub_dev_seed.sql`](dormhub_dev_seed.sql). **The SQL has not been run.** Review the target PostgreSQL schema and take a backup before running it in a disposable development/test database. Do not run it in production.

## Source review and scope

The repository uses PostgreSQL and Hibernate `ddl-auto: update` (`src/main/resources/application.yaml`). There is no SQL migration/Flyway/Liquibase file in this checkout. The schema below is derived from the JPA entities, enum declarations, repositories and services; the live database was not inspected. Apply the application's schema initialization first. If a deployed database has drifted from these entities, review it before executing the seed.

The seed covers all persistent entity tables. `refresh_tokens` remains empty because a usable session needs a raw token, while the table stores only a hash. The `sepay_transactions` row is synthetic and is for read-only UI/reconciliation display; it is not a real bank event. No OTP email is sent. The `email_verifications` rows are already used, and the login accounts are `ACTIVE`.

The script uses one transaction, generated identity values, reserved `seed_` usernames, `SEED-*` codes and `SEED:` markers. It inserts missing rows and does not update or delete existing rows. Unique constraints and marker checks make repeated runs idempotent. A preflight block aborts on conflicting reserved user/room data. Because the seed does not overwrite changes, rerunning after test users edit seeded rows may require a fresh disposable database. The seed dates are designed around **November 2026**: utility month October 2026, rent month November 2026; active contract dates use offsets from execution time. Run near this period for the described billing scenarios.

## Test accounts

All accounts have password **`12345678`**, stored with Spring Security `BCryptPasswordEncoder(10)`. Log in with the username or email as supported by the current login form. Email domain `seed.dormhub.test` is intentionally non-deliverable.

| Username | Email | Role | Gender | Current room | Scenario |
|---|---|---|---|---|---|
| `seed_manager` | `seed_manager@seed.dormhub.test` | MANAGER | — | — | Manage students, rooms, approvals, invoices |
| `seed_m01` | `seed_m01@seed.dormhub.test` | STUDENT | Nam | M101 | Full room; roommate M02; paid rent/deposit; maintenance pending |
| `seed_m02` | `seed_m02@seed.dormhub.test` | STUDENT | Nam | M101 | Full room; roommate M01; pending renewal |
| `seed_m03` | `seed_m03@seed.dormhub.test` | STUDENT | Nam | M102 | Partially occupied room; pending transfer to M103; unpaid rent |
| `seed_m04` | `seed_m04@seed.dormhub.test` | STUDENT | Nam | M102 | Assigned, not checked in; pending checkout; expired payment attempt |
| `seed_m05` | `seed_m05@seed.dormhub.test` | STUDENT | Nam | — | No contract/room; pending application to M103 |
| `seed_f01` | `seed_f01@seed.dormhub.test` | STUDENT | Nữ | F101 | Full room; roommate F02; paid rent and maintenance completed |
| `seed_f02` | `seed_f02@seed.dormhub.test` | STUDENT | Nữ | F101 | Full room; roommate F01 |
| `seed_f03` | `seed_f03@seed.dormhub.test` | STUDENT | Nữ | F102 | Approved transfer F104 → F102; unpaid rent |
| `seed_f04` | `seed_f04@seed.dormhub.test` | STUDENT | Nữ | F103 | Single room; approved renewal |
| `seed_f05` | `seed_f05@seed.dormhub.test` | STUDENT | Nữ | — | Historical F104 checkout, terminated contract, refunded deposit, rejected new application |

Buildings `SEED-M` and `SEED-F` restrict gender to `MALE` and `FEMALE`. Room M103 and F104 are empty, M102 and F102 have vacancies, M101/F101/F103 are full, and M104 is under maintenance. Active assignment counts match `rooms.current_occupancy`; no seeded room exceeds capacity. The F03 transfer keeps an old `CHECKED_OUT` assignment in F104 and a current `CHECKED_IN` assignment in F102. F05 has only a historical `CHECKED_OUT` assignment.

## Expected inserted rows on an empty application schema

Counts refer only to rows created by this seed. A second run should insert **zero** rows. Existing unrelated application data remains untouched.

| Table | Rows | Table | Rows |
|---|---:|---|---:|
| `users` | 11 | `students` | 10 |
| `buildings` | 2 | `rooms` | 8 |
| `dorm_applications` | 11 | `contracts` | 9 |
| `room_assignments` | 10 | `room_transfer_requests` | 2 |
| `checkout_requests` | 2 | `renewal_requests` | 2 |
| `prices` | 2 | `meter_readings` | 5 |
| `invoices` | 22 | `invoice_details` | 27 |
| `payments` | 17 | `sepay_transactions` | 1 |
| `notifications` | 4 | `notification_receivers` | 5 |
| `maintenance_requests` | 3 | `email_verifications` | 11 |
| `refresh_tokens` | 0 | | |

Invoice breakdown: 9 paid deposits (including F05's historical refunded deposit), 8 November room fees (3 paid, 5 unpaid), 5 October utility bills (1 paid, 4 unpaid). Every seeded invoice has line items whose amounts sum to its total. Payment attempts include `SUCCESS`, `PENDING` and `EXPIRED`; pending payments have a future expiry. Prices are electricity 3,500 and water 18,000 per unit.

## Execution order

1. Start the backend against an empty development/test PostgreSQL database so Hibernate creates/updates the entity schema. Stop the backend before inserting the seed to avoid schedulers changing these test scenarios.
2. Review database schema and backup. Run **only** `dormhub_dev_seed.sql` using a PostgreSQL client after explicit approval. The file already contains `BEGIN`/`COMMIT` and handles FK order internally: users → buildings/rooms → students → applications/contracts/assignments → requests → prices/readings → invoices/details/payments → notifications/maintenance/verification.
3. Restart the backend. Test login and the scenarios above. A repeat run should leave counts unchanged; it does not reset edited test records.

No explicit identity IDs or `setval` calls are used, so generated sequences advance normally. The string primary keys in `prices` are reserved seed values.

## Read-only verification after an approved run

```sql
SELECT role, status, count(*) FROM users
WHERE username LIKE 'seed_%' GROUP BY role, status ORDER BY role, status;

SELECT b.building_code, b.gender_restriction, r.room_number,
       r.capacity, r.current_occupancy, r.status,
       count(ra.assignment_id) FILTER (WHERE ra.status IN ('ASSIGNED','CHECKED_IN')) AS actual_occupancy
FROM rooms r JOIN buildings b ON b.id = r.building_id
LEFT JOIN room_assignments ra ON ra.room_id = r.id
WHERE b.building_code IN ('SEED-M','SEED-F')
GROUP BY b.building_code, b.gender_restriction, r.id
ORDER BY b.building_code, r.room_number;

SELECT u.username, s.gender, b.gender_restriction, r.room_number
FROM students s JOIN users u ON u.id = s.user_id
LEFT JOIN rooms r ON r.id = s.room_id
LEFT JOIN buildings b ON b.id = r.building_id
WHERE u.username LIKE 'seed_%' ORDER BY u.username;

SELECT i.invoice_id, i.invoice_type, i.status, i.total_amount,
       COALESCE(sum(d.amount), 0) AS detail_total
FROM invoices i JOIN contracts c ON c.contract_id = i.contract_id
LEFT JOIN invoice_details d ON d.invoice_id = i.invoice_id
WHERE c.description LIKE 'SEED:CONTRACT:%'
GROUP BY i.invoice_id ORDER BY i.invoice_id;
```

## Intentional limits

- `refresh_tokens` cannot be meaningfully seeded: the backend must issue a raw refresh token and HttpOnly cookie at login.
- OTP delivery, real SePay webhooks, QR/bank settlement and scheduler effects require a running application and external services. The seed only represents persisted outcomes.
- The source has no separate room amenity entity/table. Room type, price, meter numbers and description are seeded; there is no independent amenity relation to populate.
- The live database schema and row counts were not inspected, and this SQL was not executed. The counts above are expected counts from the script, not observed database results.

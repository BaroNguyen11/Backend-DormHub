-- DormHub development/test seed, PostgreSQL. Run only after reviewing the
-- target schema and taking a backup. No existing row is updated or deleted.
-- Hibernate ddl-auto=update is the schema source in this repository.
BEGIN;
SELECT pg_advisory_xact_lock(2026, 11012);

CREATE TEMP TABLE seed_people (
    username text PRIMARY KEY, full_name text NOT NULL, role text NOT NULL,
    gender text, student_code text, current_room text
) ON COMMIT DROP;
INSERT INTO seed_people VALUES
 ('seed_manager','Quản lý Seed','MANAGER',NULL,NULL,NULL),
 ('seed_m01','Nguyễn Minh An','STUDENT','MALE','99010001','M101'),
 ('seed_m02','Trần Quốc Bảo','STUDENT','MALE','99010002','M101'),
 ('seed_m03','Lê Hoàng Duy','STUDENT','MALE','99010003','M102'),
 ('seed_m04','Phạm Đức Huy','STUDENT','MALE','99010004','M102'),
 ('seed_m05','Võ Gia Khang','STUDENT','MALE','99010005',NULL),
 ('seed_f01','Nguyễn Thu Hà','STUDENT','FEMALE','99020001','F101'),
 ('seed_f02','Trần Ngọc Linh','STUDENT','FEMALE','99020002','F101'),
 ('seed_f03','Lê Mai Phương','STUDENT','FEMALE','99020003','F102'),
 ('seed_f04','Phạm Bảo Ngọc','STUDENT','FEMALE','99020004','F103'),
 ('seed_f05','Võ Thảo Vy','STUDENT','FEMALE','99020005',NULL);

-- Generated with Spring Security BCryptPasswordEncoder(10) for 12345678.
INSERT INTO users (username,email,password_hash,full_name,phone,role,status,created_at,updated_at)
SELECT p.username, p.username || '@seed.dormhub.test',
 '$2a$10$RWsFcFbJpNOwtPBs9/DvkOI4edWBAzcRBC1CRgmc.QILRMHPOgoSK',
 p.full_name, NULL, p.role, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM seed_people p WHERE TRUE ON CONFLICT DO NOTHING;

CREATE TEMP TABLE seed_rooms (
 building_code text, room_number text, room_type text, capacity integer,
 occupancy integer, status text, price numeric, description text,
 PRIMARY KEY (building_code,room_number)
) ON COMMIT DROP;
INSERT INTO seed_rooms VALUES
 ('SEED-M','M101','DOUBLE',2,2,'OCCUPIED',900000,'Phòng nam đủ chỗ'),
 ('SEED-M','M102','QUAD',4,2,'AVAILABLE',700000,'Phòng nam còn chỗ'),
 ('SEED-M','M103','DOUBLE',2,0,'AVAILABLE',900000,'Phòng nam trống'),
 ('SEED-M','M104','SINGLE',1,0,'MAINTENANCE',1200000,'Phòng nam bảo trì'),
 ('SEED-F','F101','DOUBLE',2,2,'OCCUPIED',900000,'Phòng nữ đủ chỗ'),
 ('SEED-F','F102','QUAD',4,1,'AVAILABLE',700000,'Phòng nữ còn chỗ'),
 ('SEED-F','F103','SINGLE',1,1,'OCCUPIED',1200000,'Phòng nữ ở một mình'),
 ('SEED-F','F104','DOUBLE',2,0,'AVAILABLE',900000,'Phòng nữ trống');

INSERT INTO buildings (building_code,name,address,total_floors,gender_restriction,description,created_at,updated_at)
VALUES
 ('SEED-M','Tòa Nam Seed','Khu kiểm thử DormHub',5,'MALE','Chỉ sinh viên Nam',CURRENT_TIMESTAMP,CURRENT_TIMESTAMP),
 ('SEED-F','Tòa Nữ Seed','Khu kiểm thử DormHub',5,'FEMALE','Chỉ sinh viên Nữ',CURRENT_TIMESTAMP,CURRENT_TIMESTAMP)
ON CONFLICT DO NOTHING;

INSERT INTO rooms (building_id,room_number,floor,room_type,capacity,current_occupancy,
 price_per_month,status,electric_meter_number,water_meter_number,description,created_at,updated_at)
SELECT b.id,r.room_number,1,r.room_type,r.capacity,r.occupancy,r.price,r.status,
 'E-SEED-'||r.room_number,'W-SEED-'||r.room_number,r.description,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP
FROM seed_rooms r JOIN buildings b ON b.building_code=r.building_code
WHERE TRUE
ON CONFLICT DO NOTHING;

INSERT INTO students (user_id,student_code,date_of_birth,gender,address,room_id,created_at,updated_at)
SELECT u.id,p.student_code,DATE '2005-05-15',p.gender,'Ký túc xá DormHub',r.id,
 CURRENT_TIMESTAMP,CURRENT_TIMESTAMP
FROM seed_people p JOIN users u ON u.username=p.username
LEFT JOIN rooms r ON r.room_number=p.current_room
 AND r.building_id=(SELECT b.id FROM buildings b WHERE b.building_code=
  CASE p.gender WHEN 'MALE' THEN 'SEED-M' ELSE 'SEED-F' END)
WHERE p.role='STUDENT'
ON CONFLICT DO NOTHING;

-- Stop atomically if any reserved natural key belongs to unrelated data.
DO $$ BEGIN
 IF EXISTS (SELECT 1 FROM seed_people p LEFT JOIN users u ON u.username=p.username
            WHERE u.id IS NULL OR u.email<>p.username||'@seed.dormhub.test'
               OR u.role<>p.role OR u.status<>'ACTIVE'
               OR u.password_hash<>'$2a$10$RWsFcFbJpNOwtPBs9/DvkOI4edWBAzcRBC1CRgmc.QILRMHPOgoSK') THEN
   RAISE EXCEPTION 'Seed user key collision or missing ACTIVE user';
 END IF;
 IF EXISTS (SELECT 1 FROM seed_people p JOIN users u ON u.username=p.username
            LEFT JOIN students s ON s.user_id=u.id
            WHERE p.role='STUDENT' AND
              (s.id IS NULL OR s.student_code<>p.student_code OR s.gender<>p.gender)) THEN
   RAISE EXCEPTION 'Seed student key collision';
 END IF;
 IF EXISTS (SELECT 1 FROM seed_rooms x LEFT JOIN buildings b ON b.building_code=x.building_code
            LEFT JOIN rooms r ON r.room_number=x.room_number AND r.building_id=b.id
            WHERE r.id IS NULL OR r.capacity<>x.capacity
               OR r.current_occupancy<>x.occupancy OR r.status<>x.status
               OR r.price_per_month<>x.price OR b.gender_restriction<>
               CASE x.building_code WHEN 'SEED-M' THEN 'MALE' ELSE 'FEMALE' END) THEN
   RAISE EXCEPTION 'Seed room/building key collision';
 END IF;
 IF (SELECT count(*) FROM rooms r JOIN buildings b ON b.id=r.building_id
     WHERE b.building_code IN ('SEED-M','SEED-F')) < 8 THEN
   RAISE EXCEPTION 'Seed rooms or buildings missing';
 END IF;
END $$;

CREATE TEMP TABLE seed_apps (
 marker text PRIMARY KEY, username text, room_number text, status text,
 processed_at timestamp, review_note text
) ON COMMIT DROP;
INSERT INTO seed_apps VALUES
 ('SEED:APP:m01','seed_m01','M101','APPROVED',CURRENT_TIMESTAMP-INTERVAL '2 months','Đã duyệt'),
 ('SEED:APP:m02','seed_m02','M101','APPROVED',CURRENT_TIMESTAMP-INTERVAL '2 months','Đã duyệt'),
 ('SEED:APP:m03','seed_m03','M102','APPROVED',CURRENT_TIMESTAMP-INTERVAL '2 months','Đã duyệt'),
 ('SEED:APP:m04','seed_m04','M102','APPROVED',CURRENT_TIMESTAMP-INTERVAL '2 months','Đã duyệt'),
 ('SEED:APP:f01','seed_f01','F101','APPROVED',CURRENT_TIMESTAMP-INTERVAL '2 months','Đã duyệt'),
 ('SEED:APP:f02','seed_f02','F101','APPROVED',CURRENT_TIMESTAMP-INTERVAL '2 months','Đã duyệt'),
 ('SEED:APP:f03','seed_f03','F104','APPROVED',CURRENT_TIMESTAMP-INTERVAL '2 months','Đã chuyển sang F102'),
 ('SEED:APP:f04','seed_f04','F103','APPROVED',CURRENT_TIMESTAMP-INTERVAL '2 months','Đã duyệt'),
 ('SEED:APP:f05-old','seed_f05','F104','APPROVED',CURRENT_TIMESTAMP-INTERVAL '10 months','Đã trả phòng'),
 ('SEED:APP:m05-pending','seed_m05','M103','PENDING',NULL,NULL),
 ('SEED:APP:f05-rejected','seed_f05','F103','REJECTED',CURRENT_TIMESTAMP-INTERVAL '1 week','Phòng đã đủ chỗ');

INSERT INTO dorm_applications (student_id,requested_room_id,request_date,status,note,review_note,
 processed_at,processed_by,created_at,updated_at)
SELECT s.id,r.id,COALESCE(a.processed_at,CURRENT_TIMESTAMP)-INTERVAL '1 day',a.status,
 a.marker,a.review_note,a.processed_at,
 CASE WHEN a.status='PENDING' THEN NULL ELSE manager.id END,
 COALESCE(a.processed_at,CURRENT_TIMESTAMP)-INTERVAL '1 day',CURRENT_TIMESTAMP
FROM seed_apps a JOIN users u ON u.username=a.username JOIN students s ON s.user_id=u.id
JOIN rooms r ON r.room_number=a.room_number
JOIN buildings b ON b.id=r.building_id AND b.building_code IN ('SEED-M','SEED-F')
JOIN users manager ON manager.username='seed_manager'
WHERE NOT EXISTS (SELECT 1 FROM dorm_applications existing WHERE existing.note=a.marker);

CREATE TEMP TABLE seed_contracts (
 username text PRIMARY KEY, current_room text, status text, deposit_status text
) ON COMMIT DROP;
INSERT INTO seed_contracts VALUES
 ('seed_m01','M101','ACTIVE','HELD'),('seed_m02','M101','ACTIVE','HELD'),
 ('seed_m03','M102','ACTIVE','HELD'),('seed_m04','M102','ACTIVE','HELD'),
 ('seed_f01','F101','ACTIVE','HELD'),('seed_f02','F101','ACTIVE','HELD'),
 ('seed_f03','F102','ACTIVE','HELD'),('seed_f04','F103','ACTIVE','HELD'),
 ('seed_f05','F104','TERMINATED','REFUNDED');

INSERT INTO contracts (dorm_application_id,room_id,student_id,start_date,end_date,monthly_rent,
 deposit_amount,deposit_status,status,description,created_at,updated_at)
SELECT a.id,r.id,s.id,
 CASE WHEN c.status='TERMINATED' THEN CURRENT_TIMESTAMP-INTERVAL '10 months'
      ELSE CURRENT_TIMESTAMP-INTERVAL '2 months' END,
 CASE WHEN c.status='TERMINATED' THEN CURRENT_TIMESTAMP-INTERVAL '3 months'
      ELSE CURRENT_TIMESTAMP+INTERVAL '10 months' END,
 r.price_per_month,r.price_per_month,c.deposit_status,c.status,
 'SEED:CONTRACT:'||c.username,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP
FROM seed_contracts c JOIN users u ON u.username=c.username JOIN students s ON s.user_id=u.id
JOIN dorm_applications a ON a.note=CASE WHEN c.username='seed_f05' THEN 'SEED:APP:f05-old'
                                      ELSE 'SEED:APP:'||substring(c.username from 6) END
JOIN rooms r ON r.room_number=c.current_room
JOIN buildings b ON b.id=r.building_id AND b.building_code IN ('SEED-M','SEED-F')
WHERE NOT EXISTS (SELECT 1 FROM contracts existing WHERE existing.dorm_application_id=a.id);

CREATE TEMP TABLE seed_assignments (
 username text, room_number text, status text, assigned_offset text,
 PRIMARY KEY(username,room_number)
) ON COMMIT DROP;
INSERT INTO seed_assignments VALUES
 ('seed_m01','M101','CHECKED_IN','2 months'),('seed_m02','M101','CHECKED_IN','2 months'),
 ('seed_m03','M102','CHECKED_IN','2 months'),('seed_m04','M102','ASSIGNED','2 months'),
 ('seed_f01','F101','CHECKED_IN','2 months'),('seed_f02','F101','CHECKED_IN','2 months'),
 ('seed_f03','F104','CHECKED_OUT','2 months'),('seed_f03','F102','CHECKED_IN','1 month'),
 ('seed_f04','F103','CHECKED_IN','2 months'),('seed_f05','F104','CHECKED_OUT','10 months');

INSERT INTO room_assignments (student_id,contract_id,room_id,assigned_by,assigned_at,
 check_in_at,check_out_at,status)
SELECT s.id,c.contract_id,r.id,m.id,CURRENT_TIMESTAMP-a.assigned_offset::interval,
 CASE WHEN a.status='ASSIGNED' THEN NULL ELSE CURRENT_TIMESTAMP-a.assigned_offset::interval+INTERVAL '1 day' END,
 CASE WHEN a.status='CHECKED_OUT' THEN
   CASE WHEN a.username='seed_f05' THEN CURRENT_TIMESTAMP-INTERVAL '3 months'
        ELSE CURRENT_TIMESTAMP-INTERVAL '1 month' END ELSE NULL END,
 a.status
FROM seed_assignments a JOIN users u ON u.username=a.username JOIN students s ON s.user_id=u.id
JOIN contracts c ON c.description='SEED:CONTRACT:'||a.username
JOIN rooms r ON r.room_number=a.room_number
JOIN buildings b ON b.id=r.building_id AND b.building_code IN ('SEED-M','SEED-F')
JOIN users m ON m.username='seed_manager'
WHERE NOT EXISTS (SELECT 1 FROM room_assignments x WHERE x.contract_id=c.contract_id AND x.room_id=r.id);

-- Requests reflect current and historical housing; approved transfer moved F03
-- from F104 to F102, while F05 completed checkout and has no current room.
INSERT INTO room_transfer_requests (student_id,current_assignment_id,preferred_room_id,reason,
 approved_room_id,status,requested_at,reviewed_by,reviewed_at,review_note)
SELECT s.id,ra.assignment_id,preferred.id,'SEED:TRANSFER:m03',NULL,'PENDING',
 CURRENT_TIMESTAMP-INTERVAL '2 days',NULL,NULL,NULL
FROM users u JOIN students s ON s.user_id=u.id AND u.username='seed_m03'
JOIN contracts c ON c.description='SEED:CONTRACT:seed_m03'
JOIN room_assignments ra ON ra.contract_id=c.contract_id
JOIN rooms current_room ON current_room.id=ra.room_id AND current_room.room_number='M102'
JOIN rooms preferred ON preferred.room_number='M103'
JOIN buildings b ON b.id=preferred.building_id AND b.building_code='SEED-M'
WHERE NOT EXISTS (SELECT 1 FROM room_transfer_requests x WHERE x.reason='SEED:TRANSFER:m03');

INSERT INTO room_transfer_requests (student_id,current_assignment_id,preferred_room_id,reason,
 approved_room_id,status,requested_at,reviewed_by,reviewed_at,review_note)
SELECT s.id,ra.assignment_id,destination.id,'SEED:TRANSFER:f03',destination.id,'APPROVED',
 CURRENT_TIMESTAMP-INTERVAL '1 month' - INTERVAL '1 day',manager.id,
 CURRENT_TIMESTAMP-INTERVAL '1 month','Đã chuyển sang F102'
FROM users u JOIN students s ON s.user_id=u.id AND u.username='seed_f03'
JOIN contracts c ON c.description='SEED:CONTRACT:seed_f03'
JOIN room_assignments ra ON ra.contract_id=c.contract_id
JOIN rooms old_room ON old_room.id=ra.room_id AND old_room.room_number='F104'
JOIN rooms destination ON destination.room_number='F102'
JOIN buildings b ON b.id=destination.building_id AND b.building_code='SEED-F'
JOIN users manager ON manager.username='seed_manager'
WHERE NOT EXISTS (SELECT 1 FROM room_transfer_requests x WHERE x.reason='SEED:TRANSFER:f03');

INSERT INTO checkout_requests (student_id,contract_id,request_date,checkout_date,status,reason,
 reviewed_by_user_id,processed_at,note)
SELECT c.student_id,c.contract_id,CURRENT_TIMESTAMP-INTERVAL '1 day',
 CURRENT_DATE+INTERVAL '2 weeks','PENDING','SEED:CHECKOUT:m04',NULL,NULL,NULL
FROM contracts c WHERE c.description='SEED:CONTRACT:seed_m04'
AND NOT EXISTS (SELECT 1 FROM checkout_requests x WHERE x.reason='SEED:CHECKOUT:m04');

INSERT INTO checkout_requests (student_id,contract_id,request_date,checkout_date,status,reason,
 reviewed_by_user_id,processed_at,note)
SELECT c.student_id,c.contract_id,CURRENT_TIMESTAMP-INTERVAL '3 months' - INTERVAL '2 days',
 (CURRENT_DATE-INTERVAL '3 months')::date,'COMPLETED','SEED:CHECKOUT:f05',manager.id,
 CURRENT_TIMESTAMP-INTERVAL '3 months','Đã bàn giao phòng'
FROM contracts c JOIN users manager ON manager.username='seed_manager'
WHERE c.description='SEED:CONTRACT:seed_f05'
AND NOT EXISTS (SELECT 1 FROM checkout_requests x WHERE x.reason='SEED:CHECKOUT:f05');

INSERT INTO renewal_requests (contract_id,requested_end_date,reason,status,requested_at,
 reviewed_by,reviewed_at,review_note)
SELECT c.contract_id,c.end_date+INTERVAL '3 months','SEED:RENEW:m02','PENDING',
 CURRENT_TIMESTAMP-INTERVAL '1 day',NULL,NULL,NULL
FROM contracts c WHERE c.description='SEED:CONTRACT:seed_m02'
AND NOT EXISTS (SELECT 1 FROM renewal_requests x WHERE x.reason='SEED:RENEW:m02');

INSERT INTO renewal_requests (contract_id,requested_end_date,reason,status,requested_at,
 reviewed_by,reviewed_at,review_note)
SELECT c.contract_id,c.end_date,'SEED:RENEW:f04','APPROVED',
 CURRENT_TIMESTAMP-INTERVAL '1 month' - INTERVAL '1 day',manager.id,
 CURRENT_TIMESTAMP-INTERVAL '1 month','Đã gia hạn'
FROM contracts c JOIN users manager ON manager.username='seed_manager'
WHERE c.description='SEED:CONTRACT:seed_f04'
AND NOT EXISTS (SELECT 1 FROM renewal_requests x WHERE x.reason='SEED:RENEW:f04');

INSERT INTO prices (prices_id,type,unit_price,effective_from,created_at,created_by,note)
VALUES
 ('SEED-E-2026','ELECTRICITY',3500,DATE '2026-01-01',CURRENT_TIMESTAMP,'seed_manager','Đơn giá kiểm thử'),
 ('SEED-W-2026','WATER',18000,DATE '2026-01-01',CURRENT_TIMESTAMP,'seed_manager','Đơn giá kiểm thử')
ON CONFLICT DO NOTHING;

CREATE TEMP TABLE seed_readings (
 room_number text PRIMARY KEY, electricity_old numeric, electricity_new numeric,
 water_old numeric, water_new numeric
) ON COMMIT DROP;
INSERT INTO seed_readings VALUES
 ('M101',100,140,20,26),('M102',50,75,10,14),('F101',80,110,18,23),
 ('F102',30,47,8,10),('F103',10,20,3,5);

INSERT INTO meter_readings (room_id,reading_month,electricity_old,electricity_new,
 water_old,water_new,electricity_price,water_price,status,created_at)
SELECT r.id,DATE '2026-10-01',sr.electricity_old,sr.electricity_new,
 sr.water_old,sr.water_new,3500,18000,'CONFIRMED',TIMESTAMP '2026-11-01 09:00:00'
FROM seed_readings sr JOIN rooms r ON r.room_number=sr.room_number
JOIN buildings b ON b.id=r.building_id AND b.building_code IN ('SEED-M','SEED-F')
WHERE TRUE
ON CONFLICT DO NOTHING;

-- Deposits are attached to contracts. Room fees use a null room_id because
-- the room/month unique key is reserved for the single utility bill per room.
INSERT INTO invoices (invoice_type,room_id,contract_id,billing_month,issue_date,
 due_date,deposit_marker,status,total_amount)
SELECT 'DEPOSIT',NULL,c.contract_id,NULL,c.created_at,c.created_at+INTERVAL '7 days',
 1,'PAID',c.deposit_amount
FROM contracts c WHERE c.description LIKE 'SEED:CONTRACT:%'
AND NOT EXISTS (SELECT 1 FROM invoices i WHERE i.invoice_type='DEPOSIT'
 AND i.contract_id=c.contract_id);

CREATE TEMP TABLE seed_rent_status (username text PRIMARY KEY,status text) ON COMMIT DROP;
INSERT INTO seed_rent_status VALUES
 ('seed_m01','PAID'),('seed_m02','PAID'),('seed_m03','UNPAID'),('seed_m04','UNPAID'),
 ('seed_f01','PAID'),('seed_f02','UNPAID'),('seed_f03','UNPAID'),('seed_f04','UNPAID');

INSERT INTO invoices (invoice_type,room_id,contract_id,billing_month,issue_date,
 due_date,deposit_marker,status,total_amount)
SELECT 'ROOM_FEE',NULL,c.contract_id,DATE '2026-11-01',
 TIMESTAMP '2026-11-01 08:00:00',TIMESTAMP '2026-11-10 23:59:59',
 NULL,sr.status,c.monthly_rent
FROM seed_rent_status sr JOIN contracts c ON c.description='SEED:CONTRACT:'||sr.username
WHERE NOT EXISTS (SELECT 1 FROM invoices i WHERE i.invoice_type='ROOM_FEE'
 AND i.contract_id=c.contract_id AND i.billing_month=DATE '2026-11-01');

INSERT INTO invoices (invoice_type,room_id,contract_id,billing_month,issue_date,
 due_date,deposit_marker,status,total_amount)
SELECT 'UTILITY',mr.room_id,owner.contract_id,DATE '2026-10-01',
 TIMESTAMP '2026-11-01 08:00:00',TIMESTAMP '2026-11-10 23:59:59',NULL,
 CASE WHEN r.room_number='M101' THEN 'PAID' ELSE 'UNPAID' END,
 (mr.electricity_new-mr.electricity_old)*mr.electricity_price+
 (mr.water_new-mr.water_old)*mr.water_price
FROM meter_readings mr JOIN rooms r ON r.id=mr.room_id
JOIN buildings b ON b.id=r.building_id AND b.building_code IN ('SEED-M','SEED-F')
JOIN LATERAL (SELECT c.contract_id FROM contracts c
 WHERE c.room_id=r.id AND c.status='ACTIVE' AND c.description LIKE 'SEED:CONTRACT:%'
 ORDER BY c.contract_id LIMIT 1) owner ON TRUE
WHERE mr.reading_month=DATE '2026-10-01'
ON CONFLICT DO NOTHING;

INSERT INTO invoice_details (invoice_id,item_type,description,quantity,unit_price,amount)
SELECT i.invoice_id,'DEPOSIT','SEED:DETAIL:DEPOSIT',1,i.total_amount,i.total_amount
FROM invoices i JOIN contracts c ON c.contract_id=i.contract_id
WHERE c.description LIKE 'SEED:CONTRACT:%' AND i.invoice_type='DEPOSIT'
AND NOT EXISTS (SELECT 1 FROM invoice_details d WHERE d.invoice_id=i.invoice_id
 AND d.description='SEED:DETAIL:DEPOSIT');

INSERT INTO invoice_details (invoice_id,item_type,description,quantity,unit_price,amount)
SELECT i.invoice_id,'ROOM','SEED:DETAIL:ROOM',1,i.total_amount,i.total_amount
FROM invoices i JOIN contracts c ON c.contract_id=i.contract_id
WHERE c.description LIKE 'SEED:CONTRACT:%' AND i.invoice_type='ROOM_FEE'
AND i.billing_month=DATE '2026-11-01'
AND NOT EXISTS (SELECT 1 FROM invoice_details d WHERE d.invoice_id=i.invoice_id
 AND d.description='SEED:DETAIL:ROOM');

INSERT INTO invoice_details (invoice_id,item_type,description,quantity,unit_price,amount)
SELECT i.invoice_id,'ELECTRICITY','SEED:DETAIL:ELECTRICITY',
 mr.electricity_new-mr.electricity_old,mr.electricity_price,
 (mr.electricity_new-mr.electricity_old)*mr.electricity_price
FROM invoices i JOIN meter_readings mr ON mr.room_id=i.room_id
 AND mr.reading_month=i.billing_month
JOIN rooms r ON r.id=i.room_id JOIN buildings b ON b.id=r.building_id
 AND b.building_code IN ('SEED-M','SEED-F')
WHERE i.invoice_type='UTILITY' AND i.billing_month=DATE '2026-10-01'
AND NOT EXISTS (SELECT 1 FROM invoice_details d WHERE d.invoice_id=i.invoice_id
 AND d.description='SEED:DETAIL:ELECTRICITY');

INSERT INTO invoice_details (invoice_id,item_type,description,quantity,unit_price,amount)
SELECT i.invoice_id,'WATER','SEED:DETAIL:WATER',
 mr.water_new-mr.water_old,mr.water_price,
 (mr.water_new-mr.water_old)*mr.water_price
FROM invoices i JOIN meter_readings mr ON mr.room_id=i.room_id
 AND mr.reading_month=i.billing_month
JOIN rooms r ON r.id=i.room_id JOIN buildings b ON b.id=r.building_id
 AND b.building_code IN ('SEED-M','SEED-F')
WHERE i.invoice_type='UTILITY' AND i.billing_month=DATE '2026-10-01'
AND NOT EXISTS (SELECT 1 FROM invoice_details d WHERE d.invoice_id=i.invoice_id
 AND d.description='SEED:DETAIL:WATER');

CREATE TEMP TABLE seed_payments (
 payment_reference text PRIMARY KEY, invoice_type text, username text,
 room_number text, payment_status text
) ON COMMIT DROP;
INSERT INTO seed_payments VALUES
 ('SEED-DEP-M01','DEPOSIT','seed_m01',NULL,'SUCCESS'),
 ('SEED-DEP-M02','DEPOSIT','seed_m02',NULL,'SUCCESS'),
 ('SEED-DEP-M03','DEPOSIT','seed_m03',NULL,'SUCCESS'),
 ('SEED-DEP-M04','DEPOSIT','seed_m04',NULL,'SUCCESS'),
 ('SEED-DEP-F01','DEPOSIT','seed_f01',NULL,'SUCCESS'),
 ('SEED-DEP-F02','DEPOSIT','seed_f02',NULL,'SUCCESS'),
 ('SEED-DEP-F03','DEPOSIT','seed_f03',NULL,'SUCCESS'),
 ('SEED-DEP-F04','DEPOSIT','seed_f04',NULL,'SUCCESS'),
 ('SEED-RENT-M01','ROOM_FEE','seed_m01',NULL,'SUCCESS'),
 ('SEED-RENT-M02','ROOM_FEE','seed_m02',NULL,'SUCCESS'),
 ('SEED-RENT-M03','ROOM_FEE','seed_m03',NULL,'PENDING'),
 ('SEED-RENT-M04','ROOM_FEE','seed_m04',NULL,'EXPIRED'),
 ('SEED-RENT-F01','ROOM_FEE','seed_f01',NULL,'SUCCESS'),
 ('SEED-RENT-F03','ROOM_FEE','seed_f03',NULL,'PENDING'),
 ('SEED-UTIL-M101','UTILITY',NULL,'M101','SUCCESS'),
 ('SEED-UTIL-F101','UTILITY',NULL,'F101','PENDING');

-- F05's historical deposit was paid and later refunded; it is included here
-- separately so every seeded deposit invoice has a matching payment.
INSERT INTO seed_payments VALUES
 ('SEED-DEP-F05','DEPOSIT','seed_f05',NULL,'SUCCESS');

INSERT INTO payments (invoice_id,payment_reference,expected_amount,status,method,
 receiving_bank_code,receiving_account_number,created_at,updated_at,expires_at,paid_at)
SELECT i.invoice_id,sp.payment_reference,i.total_amount,sp.payment_status,'BANK_TRANSFER',
 'MB','0000000000',CURRENT_TIMESTAMP-INTERVAL '2 days',CURRENT_TIMESTAMP,
 CASE WHEN sp.payment_status='PENDING' THEN CURRENT_TIMESTAMP+INTERVAL '1 day'
      ELSE CURRENT_TIMESTAMP-INTERVAL '1 day' END,
 CASE WHEN sp.payment_status='SUCCESS' THEN CURRENT_TIMESTAMP-INTERVAL '1 day'
      ELSE NULL END
FROM seed_payments sp
JOIN invoices i ON i.invoice_type=sp.invoice_type
LEFT JOIN contracts c ON c.contract_id=i.contract_id
LEFT JOIN rooms r ON r.id=i.room_id
LEFT JOIN buildings b ON b.id=r.building_id
WHERE (sp.invoice_type='DEPOSIT' AND c.description='SEED:CONTRACT:'||sp.username)
 OR (sp.invoice_type='ROOM_FEE' AND c.description='SEED:CONTRACT:'||sp.username
     AND i.billing_month=DATE '2026-11-01')
 OR (sp.invoice_type='UTILITY' AND r.room_number=sp.room_number
     AND b.building_code IN ('SEED-M','SEED-F')
     AND i.billing_month=DATE '2026-10-01')
ON CONFLICT DO NOTHING;

INSERT INTO sepay_transactions (sepay_transaction_id,payment_id,status,gateway,account_number,
 transfer_type,transfer_amount,code,content,bank_reference_code,transaction_at,received_at)
SELECT 202611120001,p.payment_id,'MATCHED','MBBank','0000000000','in',
 p.expected_amount,p.payment_reference,'SEED synthetic transfer',
 'SEED-BANK-001',p.paid_at,p.paid_at
FROM payments p WHERE p.payment_reference='SEED-DEP-M01'
ON CONFLICT DO NOTHING;

CREATE TEMP TABLE seed_notifications (
 marker text PRIMARY KEY,type text,title text,body text,username text,is_read boolean
) ON COMMIT DROP;
INSERT INTO seed_notifications VALUES
 ('SEED:NOTIFY:GENERAL','GENERAL','DormHub seed','SEED:NOTIFY:GENERAL','seed_manager',false),
 ('SEED:NOTIFY:ROOM','ROOM_ASSIGNED','Phân phòng','SEED:NOTIFY:ROOM','seed_m01',true),
 ('SEED:NOTIFY:INVOICE','INVOICE_CREATED','Hóa đơn mới','SEED:NOTIFY:INVOICE','seed_f03',false),
 ('SEED:NOTIFY:TRANSFER','TRANSFER_PROCESSED','Chuyển phòng','SEED:NOTIFY:TRANSFER','seed_f03',false);

INSERT INTO notifications (type,title,body,ref_id,created_at)
SELECT sn.type,sn.title,sn.body,NULL,CURRENT_TIMESTAMP-INTERVAL '1 day'
FROM seed_notifications sn
WHERE NOT EXISTS (SELECT 1 FROM notifications n WHERE n.body=sn.marker);

INSERT INTO notification_receivers (notification_id,user_id,is_read,read_at)
SELECT n.id,u.id,sn.is_read,
 CASE WHEN sn.is_read THEN CURRENT_TIMESTAMP ELSE NULL END
FROM seed_notifications sn JOIN notifications n ON n.body=sn.marker
JOIN users u ON u.username=sn.username
WHERE NOT EXISTS (SELECT 1 FROM notification_receivers nr
 WHERE nr.notification_id=n.id AND nr.user_id=u.id);

INSERT INTO notification_receivers (notification_id,user_id,is_read,read_at)
SELECT n.id,u.id,false,NULL
FROM notifications n JOIN users u ON u.username='seed_m02'
WHERE n.body='SEED:NOTIFY:ROOM'
AND NOT EXISTS (SELECT 1 FROM notification_receivers nr
 WHERE nr.notification_id=n.id AND nr.user_id=u.id);

CREATE TEMP TABLE seed_maintenance (
 marker text PRIMARY KEY,username text,room_number text,maintenance_status text,
 priority text,type text,rejection_reason text
) ON COMMIT DROP;
INSERT INTO seed_maintenance VALUES
 ('SEED:MAINT:M101','seed_m01','M101','PENDING','MEDIUM','FAN',NULL),
 ('SEED:MAINT:F101','seed_f01','F101','COMPLETED','LOW','LIGHTING',NULL),
 ('SEED:MAINT:M102','seed_m03','M102','REJECTED','LOW','OTHER','Không thuộc phạm vi bảo trì');

INSERT INTO maintenance_requests (title,description,maintenance_status,priority,type,
 image_url,rejection_reason,created_at,updated_at,resolved_at,student_id,room_id)
SELECT 'Yêu cầu bảo trì seed',sm.marker,sm.maintenance_status,sm.priority,sm.type,
 NULL,sm.rejection_reason,CURRENT_TIMESTAMP-INTERVAL '1 week',CURRENT_TIMESTAMP,
 CASE WHEN sm.maintenance_status='COMPLETED' THEN CURRENT_TIMESTAMP-INTERVAL '1 day'
      ELSE NULL END,s.id,r.id
FROM seed_maintenance sm JOIN users u ON u.username=sm.username
JOIN students s ON s.user_id=u.id JOIN rooms r ON r.room_number=sm.room_number
JOIN buildings b ON b.id=r.building_id AND b.building_code IN ('SEED-M','SEED-F')
WHERE NOT EXISTS (SELECT 1 FROM maintenance_requests m WHERE m.description=sm.marker);

-- Used registration OTP rows document a completed verification. They cannot
-- authenticate or trigger email delivery; raw OTP is never stored.
INSERT INTO email_verifications (otp_hash,purpose,expires_at,used_at,attempts,created_at,user_id)
SELECT '$2a$10$WThbejV/zyJn0cUAZaXVJ.ewWYYJd1WmYqR10aEfwoPX2ELHcA/3C',
 'REGISTER',CURRENT_TIMESTAMP-INTERVAL '1 day',CURRENT_TIMESTAMP-INTERVAL '2 days',
 0,CURRENT_TIMESTAMP-INTERVAL '3 days',u.id
FROM seed_people sp JOIN users u ON u.username=sp.username
WHERE NOT EXISTS (SELECT 1 FROM email_verifications ev
 WHERE ev.user_id=u.id AND ev.purpose='REGISTER' AND ev.used_at IS NOT NULL);

-- Guard against a partial seed when a pre-existing marker or schema drift
-- causes an INSERT ... SELECT to produce fewer rows than expected.
DO $$ BEGIN
 IF (SELECT count(*) FROM users WHERE username IN (SELECT username FROM seed_people))<>11
 OR (SELECT count(*) FROM students s JOIN users u ON u.id=s.user_id
     WHERE u.username IN (SELECT username FROM seed_people WHERE role='STUDENT'))<>10
 OR (SELECT count(*) FROM buildings WHERE building_code IN ('SEED-M','SEED-F'))<>2
 OR (SELECT count(*) FROM rooms r JOIN buildings b ON b.id=r.building_id
     WHERE b.building_code IN ('SEED-M','SEED-F'))<>8
 OR (SELECT count(*) FROM dorm_applications WHERE note IN (SELECT marker FROM seed_apps))<>11
 OR (SELECT count(*) FROM contracts WHERE description LIKE 'SEED:CONTRACT:%')<>9
 OR (SELECT count(*) FROM payments WHERE payment_reference IN
     (SELECT payment_reference FROM seed_payments))<>17 THEN
   RAISE EXCEPTION 'DormHub seed did not create all expected core rows';
 END IF;
END $$;

COMMIT;

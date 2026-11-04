-- Insert Buildings (only if not exists)
INSERT INTO buildings (building_code, name, address, total_floors, gender_restriction, description, created_at, updated_at)
SELECT 'A1', 'Tòa nhà A1', 'Khu nội trú A, Đại học XYZ', 5, 'MALE', 'Tòa nam sinh viên, 5 tầng, 100 phòng', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM buildings WHERE building_code = 'A1');

INSERT INTO buildings (building_code, name, address, total_floors, gender_restriction, description, created_at, updated_at)
SELECT 'A2', 'Tòa nhà A2', 'Khu nội trú A, Đại học XYZ', 5, 'FEMALE', 'Tòa nữ sinh viên, 5 tầng, 100 phòng', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM buildings WHERE building_code = 'A2');

INSERT INTO buildings (building_code, name, address, total_floors, gender_restriction, description, created_at, updated_at)
SELECT 'B1', 'Tòa nhà B1', 'Khu nội trú B, Đại học XYZ', 7, 'MALE', 'Tòa nam sinh viên cao cấp, 7 tầng', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM buildings WHERE building_code = 'B1');

-- Insert Rooms for Building A1 (Male)
-- Floor 1: QUAD rooms (101-110)
INSERT INTO rooms (room_number, floor, building_id, room_type, capacity, current_occupancy, price_per_month, status, electric_meter_number, water_meter_number, description, created_at, updated_at)
SELECT '101', 1, b.id, 'QUAD', 4, 0, 1200000.00, 'AVAILABLE', 'A1-E-101', 'A1-W-101', 'Phòng 4 người, tầng 1', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM buildings b WHERE b.building_code = 'A1' AND NOT EXISTS (SELECT 1 FROM rooms WHERE room_number = '101' AND building_id = b.id);

INSERT INTO rooms (room_number, floor, building_id, room_type, capacity, current_occupancy, price_per_month, status, electric_meter_number, water_meter_number, description, created_at, updated_at)
SELECT '102', 1, b.id, 'QUAD', 4, 2, 1200000.00, 'AVAILABLE', 'A1-E-102', 'A1-W-102', 'Phòng 4 người, tầng 1', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM buildings b WHERE b.building_code = 'A1' AND NOT EXISTS (SELECT 1 FROM rooms WHERE room_number = '102' AND building_id = b.id);

-- Floor 2: SIX_BED rooms (201-210)
INSERT INTO rooms (room_number, floor, building_id, room_type, capacity, current_occupancy, price_per_month, status, electric_meter_number, water_meter_number, description, created_at, updated_at)
SELECT '201', 2, b.id, 'SIX_BED', 6, 4, 1000000.00, 'AVAILABLE', 'A1-E-201', 'A1-W-201', 'Phòng 6 người, tầng 2', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM buildings b WHERE b.building_code = 'A1' AND NOT EXISTS (SELECT 1 FROM rooms WHERE room_number = '201' AND building_id = b.id);

INSERT INTO rooms (room_number, floor, building_id, room_type, capacity, current_occupancy, price_per_month, status, electric_meter_number, water_meter_number, description, created_at, updated_at)
SELECT '202', 2, b.id, 'SIX_BED', 6, 6, 1000000.00, 'OCCUPIED', 'A1-E-202', 'A1-W-202', 'Phòng 6 người, tầng 2, đã đầy', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM buildings b WHERE b.building_code = 'A1' AND NOT EXISTS (SELECT 1 FROM rooms WHERE room_number = '202' AND building_id = b.id);

-- Floor 3: DOUBLE rooms (301-320)
INSERT INTO rooms (room_number, floor, building_id, room_type, capacity, current_occupancy, price_per_month, status, electric_meter_number, water_meter_number, description, created_at, updated_at)
SELECT '301', 3, b.id, 'DOUBLE', 2, 0, 1500000.00, 'AVAILABLE', 'A1-E-301', 'A1-W-301', 'Phòng 2 người, tầng 3', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM buildings b WHERE b.building_code = 'A1' AND NOT EXISTS (SELECT 1 FROM rooms WHERE room_number = '301' AND building_id = b.id);

INSERT INTO rooms (room_number, floor, building_id, room_type, capacity, current_occupancy, price_per_month, status, electric_meter_number, water_meter_number, description, created_at, updated_at)
SELECT '302', 3, b.id, 'DOUBLE', 2, 1, 1500000.00, 'AVAILABLE', 'A1-E-302', 'A1-W-302', 'Phòng 2 người, tầng 3', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM buildings b WHERE b.building_code = 'A1' AND NOT EXISTS (SELECT 1 FROM rooms WHERE room_number = '302' AND building_id = b.id);

-- Insert Rooms for Building A2 (Female)
INSERT INTO rooms (room_number, floor, building_id, room_type, capacity, current_occupancy, price_per_month, status, electric_meter_number, water_meter_number, description, created_at, updated_at)
SELECT '101', 1, b.id, 'QUAD', 4, 0, 1200000.00, 'AVAILABLE', 'A2-E-101', 'A2-W-101', 'Phòng 4 người, tầng 1', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM buildings b WHERE b.building_code = 'A2' AND NOT EXISTS (SELECT 1 FROM rooms WHERE room_number = '101' AND building_id = b.id);

INSERT INTO rooms (room_number, floor, building_id, room_type, capacity, current_occupancy, price_per_month, status, electric_meter_number, water_meter_number, description, created_at, updated_at)
SELECT '102', 1, b.id, 'QUAD', 4, 3, 1200000.00, 'AVAILABLE', 'A2-E-102', 'A2-W-102', 'Phòng 4 người, tầng 1', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM buildings b WHERE b.building_code = 'A2' AND NOT EXISTS (SELECT 1 FROM rooms WHERE room_number = '102' AND building_id = b.id);

INSERT INTO rooms (room_number, floor, building_id, room_type, capacity, current_occupancy, price_per_month, status, electric_meter_number, water_meter_number, description, created_at, updated_at)
SELECT '201', 2, b.id, 'DOUBLE', 2, 0, 1500000.00, 'AVAILABLE', 'A2-E-201', 'A2-W-201', 'Phòng 2 người, tầng 2', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM buildings b WHERE b.building_code = 'A2' AND NOT EXISTS (SELECT 1 FROM rooms WHERE room_number = '201' AND building_id = b.id);

-- Insert Rooms for Building B1 (Male - Premium)
INSERT INTO rooms (room_number, floor, building_id, room_type, capacity, current_occupancy, price_per_month, status, electric_meter_number, water_meter_number, description, created_at, updated_at)
SELECT '101', 1, b.id, 'SINGLE', 1, 0, 2000000.00, 'AVAILABLE', 'B1-E-101', 'B1-W-101', 'Phòng 1 người VIP', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM buildings b WHERE b.building_code = 'B1' AND NOT EXISTS (SELECT 1 FROM rooms WHERE room_number = '101' AND building_id = b.id);

INSERT INTO rooms (room_number, floor, building_id, room_type, capacity, current_occupancy, price_per_month, status, electric_meter_number, water_meter_number, description, created_at, updated_at)
SELECT '102', 1, b.id, 'SINGLE', 1, 1, 2000000.00, 'OCCUPIED', 'B1-E-102', 'B1-W-102', 'Phòng 1 người VIP, đã có người', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM buildings b WHERE b.building_code = 'B1' AND NOT EXISTS (SELECT 1 FROM rooms WHERE room_number = '102' AND building_id = b.id);

INSERT INTO rooms (room_number, floor, building_id, room_type, capacity, current_occupancy, price_per_month, status, electric_meter_number, water_meter_number, description, created_at, updated_at)
SELECT '201', 2, b.id, 'DOUBLE', 2, 0, 1800000.00, 'AVAILABLE', 'B1-E-201', 'B1-W-201', 'Phòng 2 người cao cấp', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM buildings b WHERE b.building_code = 'B1' AND NOT EXISTS (SELECT 1 FROM rooms WHERE room_number = '201' AND building_id = b.id);

-- One room in maintenance
INSERT INTO rooms (room_number, floor, building_id, room_type, capacity, current_occupancy, price_per_month, status, electric_meter_number, water_meter_number, description, created_at, updated_at)
SELECT '301', 3, b.id, 'QUAD', 4, 0, 1400000.00, 'MAINTENANCE', 'B1-E-301', 'B1-W-301', 'Phòng đang sửa chữa điều hòa', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM buildings b WHERE b.building_code = 'B1' AND NOT EXISTS (SELECT 1 FROM rooms WHERE room_number = '301' AND building_id = b.id);

-- ==========================================
-- SAMPLE USERS AND STUDENTS FOR TESTING
-- ==========================================
-- Login credentials:
--   Manager: email=manager@dormhub.com, password=password123
--   Student 1: email=student1@dormhub.com, password=password123
--   Student 2: email=student2@dormhub.com, password=password123
--   Student 3: email=student3@dormhub.com, password=password123
-- BCrypt hash for "password123": $2a$10$slYQmyNdGzTn7ZMSnF51zuB.MbVN/Y8D8n7ViLLxE3eCp3oD.aBqG
-- ==========================================

-- Insert Manager User
INSERT INTO users (username, email, password_hash, full_name, phone, role, status, created_at, updated_at)
SELECT 'manager', 'manager@dormhub.com', '$2a$10$3x2oq4PHEyZ8AYIa1qdRjuWLgIAsA.4pdPvpeWUmeir7fE/icZKOO', 'Quản lý KTX', '0901234567', 'MANAGER', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM users WHERE username = 'manager');

-- Insert Student 1 User
INSERT INTO users (username, email, password_hash, full_name, phone, role, status, created_at, updated_at)
SELECT 'student1', 'student1@dormhub.com', '$2a$10$3x2oq4PHEyZ8AYIa1qdRjuWLgIAsA.4pdPvpeWUmeir7fE/icZKOO', 'Nguyễn Văn A', '0912345678', 'STUDENT', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM users WHERE username = 'student1');

-- Insert Student 1 Profile
INSERT INTO students (student_code, date_of_birth, gender, address, user_id, created_at, updated_at)
SELECT '20210001', '2003-01-15', 'MALE', '123 Nguyễn Huệ, Q1, TP.HCM', u.id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM users u WHERE u.username = 'student1'
AND NOT EXISTS (SELECT 1 FROM students WHERE student_code = '20210001');

-- Insert Student 2 User
INSERT INTO users (username, email, password_hash, full_name, phone, role, status, created_at, updated_at)
SELECT 'student2', 'student2@dormhub.com', '$2a$10$3x2oq4PHEyZ8AYIa1qdRjuWLgIAsA.4pdPvpeWUmeir7fE/icZKOO', 'Trần Thị B', '0923456789', 'STUDENT', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM users WHERE username = 'student2');

-- Insert Student 2 Profile
INSERT INTO students (student_code, date_of_birth, gender, address, user_id, created_at, updated_at)
SELECT '20210002', '2003-05-20', 'FEMALE', '456 Lê Lợi, Q1, TP.HCM', u.id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM users u WHERE u.username = 'student2'
AND NOT EXISTS (SELECT 1 FROM students WHERE student_code = '20210002');

-- Insert Student 3 User
INSERT INTO users (username, email, password_hash, full_name, phone, role, status, created_at, updated_at)
SELECT 'student3', 'student3@dormhub.com', '$2a$10$3x2oq4PHEyZ8AYIa1qdRjuWLgIAsA.4pdPvpeWUmeir7fE/icZKOO', 'Lê Văn C', '0934567890', 'STUDENT', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM users WHERE username = 'student3');

-- Insert Student 3 Profile
INSERT INTO students (student_code, date_of_birth, gender, address, user_id, created_at, updated_at)
SELECT '20210003', '2003-08-10', 'MALE', '789 Hai Bà Trưng, Q3, TP.HCM', u.id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM users u WHERE u.username = 'student3'
AND NOT EXISTS (SELECT 1 FROM students WHERE student_code = '20210003');

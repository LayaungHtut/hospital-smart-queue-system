-- ============================================================
-- V2__seed_data.sql
-- Hospital Smart Queue System - seed data
-- Demo password for all seeded users: 123456
-- (BCrypt hash below corresponds to "123456")
-- ============================================================

-- ---------- Departments ----------
-- ON CONFLICT keeps this idempotent: safe when Neon already holds seeded rows.
INSERT INTO department (department_code, department_name) VALUES
('CAR', 'Cardiology'),
('NEU', 'Neurology'),
('ORT', 'Orthopedics'),
('GEN', 'General Medicine'),
('PED', 'Pediatrics'),
('DER', 'Dermatology')
ON CONFLICT DO NOTHING;

-- ---------- Priorities (smart queue engine) ----------
INSERT INTO priority (priority_code, priority_name, sort_order, weight) VALUES
('EMERGENCY',   'Emergency',   1, 0.50),
('APPOINTMENT', 'Appointment', 2, 0.80),
('NORMAL',      'Normal',      3, 1.00)
ON CONFLICT DO NOTHING;

-- ---------- Doctors (password: 123456) ----------
INSERT INTO doctor (doctor_id, doctor_code, name, department_id, specialization, phone, email, password_hash,
                    max_queue_size, queue_open_time, queue_close_time, average_consultation_minutes)
VALUES
('D001', 'CAR1', 'Dr. Min Thu',     1, 'Interventional Cardiology', '0911111111', 'min.thu@hospital.com',  '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDa', 20, '07:00:00', '22:00:00', 15),
('D002', 'CAR2', 'Dr. Su Myat Noe', 1, 'Heart Failure',            '0911111112', 'su.myat@hospital.com',  '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDa', 20, '07:00:00', '22:00:00', 15),
('D003', 'NEU1', 'Dr. Kyaw Zin',    2, 'Stroke Neurology',         '0911111113', 'kyaw.zin@hospital.com', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDa', 15, '07:00:00', '22:00:00', 20),
('D004', 'ORT1', 'Dr. Nandar Lin',  3, 'Joint Replacement',        '0911111114', 'nandar.lin@hospital.com','$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDa', 20, '07:00:00', '22:00:00', 15),
('D005', 'GEN1', 'Dr. Hnin Wai',    4, 'Internal Medicine',        '0911111115', 'hnin.wai@hospital.com', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDa', 25, '07:00:00', '22:00:00', 10),
('D006', 'GEN2', 'Dr. Zaw Min',     4, 'Geriatrics',               '0911111116', 'zaw.min@hospital.com',  '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDa', 25, '07:00:00', '22:00:00', 10),
('D007', 'PED1', 'Dr. Yin Yin Htun',5, 'Pediatric Cardiology',     '0911111117', 'yin.htun@hospital.com', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDa', 20, '07:00:00', '22:00:00', 15),
('D008', 'DER1', 'Dr. Phyo Phyo',   6, 'Dermatopathology',         '0911111118', 'phyo.phyo@hospital.com', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDa', 15, '07:00:00', '22:00:00', 15)
ON CONFLICT DO NOTHING;

-- ---------- Demo patient (phone: 09123456789, password: 123456) ----------
INSERT INTO patient (patient_id, name, phone, password_hash, email, address, date_of_birth, gender, is_new_patient)
VALUES ('P202601010001', 'Aung Aung', '09123456789', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDa',
        'aung@gmail.com', 'Yangon', '1995-05-10', 'Male', FALSE)
ON CONFLICT DO NOTHING;

-- ---------- Demo staff (login: staff / 123456) ----------
INSERT INTO staff (staff_id, staff_code, name, phone, email, password_hash, role)
VALUES ('S202601010001', 'STF1', 'Staff Member One', '0922222222', 'staff@hospital.com',
        '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDa', 'STAFF')
ON CONFLICT DO NOTHING;

-- ---------- Demo admin (login: admin / 123456) ----------
INSERT INTO admin_user (admin_id, username, name, email, password_hash, role)
VALUES ('A202601010001', 'admin', 'System Administrator', 'admin@hospital.com',
        '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDa', 'ADMIN')
ON CONFLICT DO NOTHING;

-- ---------- Demo appointment (today, GEN1) ----------
INSERT INTO appointment (patient_id, doctor_id, department_id, appointment_date, appointment_time, status)
SELECT 'P202601010001', 'D005', 4, CURRENT_DATE, '09:00:00', 'SCHEDULED'
WHERE NOT EXISTS (
    SELECT 1 FROM appointment
    WHERE patient_id = 'P202601010001' AND appointment_date = CURRENT_DATE AND doctor_id = 'D005'
);

-- ---------- Demo welcome notification ----------
INSERT INTO notification (patient_id, message, is_read)
SELECT 'P202601010001', 'Welcome to Hospital Smart Queue System! Your appointment is ready for today. Please check in when you arrive.', FALSE
WHERE NOT EXISTS (SELECT 1 FROM patient WHERE patient_id = 'P202601010001');

-- ---------- System settings ----------
INSERT INTO system_setting (setting_key, setting_value, description, is_public) VALUES
('hospital_name', 'CareQueue Hospital', 'Hospital display name', TRUE),
('queue_open_time', '07:00:00', 'Default queue opening time', TRUE),
('queue_close_time', '22:00:00', 'Default queue closing time', TRUE),
('max_queue_per_doctor', '20', 'Maximum patients per doctor', TRUE),
('enable_online_registration', 'true', 'Allow online self-registration', TRUE),
('enable_appointments', 'true', 'Allow appointment booking', TRUE),
('default_consultation_minutes', '15', 'Default consultation duration', TRUE),
('queue_expiry_minutes', '30', 'Minutes before queue expires', FALSE),
('emergency_priority_enabled', 'true', 'Enable emergency priority', TRUE)
ON CONFLICT DO NOTHING;

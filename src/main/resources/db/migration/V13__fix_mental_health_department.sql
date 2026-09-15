-- ============================================================
-- V13__fix_mental_health_department.sql
-- Corrects the misspelled "Menta" department (created via the admin
-- panel) to "Mental Health", collapsing any accidental duplicates into
-- a single row, then seeds a couple of doctors into it.
-- Demo password for the new doctors: 123456
-- ============================================================

-- Collapse duplicate 'Menta' rows into the lowest department_id, then rename it.
DELETE FROM department
WHERE department_name = 'Menta'
  AND department_id <> (SELECT MIN(department_id) FROM department WHERE department_name = 'Menta');

UPDATE department
SET department_name = 'Mental Health'
WHERE department_name = 'Menta';

-- Seed a couple of doctors into Mental Health (login: doctor_code / 123456).
INSERT INTO doctor (doctor_id, doctor_code, name, department_id, specialization, phone, email, password_hash,
                    max_queue_size, queue_open_time, queue_close_time, average_consultation_minutes,
                    qualification, years_of_experience)
SELECT 'D009', 'MEN1', 'Dr. Thiri Aung', d.department_id, 'General Psychiatry', '0911111119',
       'thiri.aung@hospital.com', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDa',
       15, '07:00:00', '22:00:00', 20, 'MBBS, M.Med.Sc (Psychiatry)', 8
FROM department d
WHERE d.department_name = 'Mental Health'
ON CONFLICT DO NOTHING;

INSERT INTO doctor (doctor_id, doctor_code, name, department_id, specialization, phone, email, password_hash,
                    max_queue_size, queue_open_time, queue_close_time, average_consultation_minutes,
                    qualification, years_of_experience)
SELECT 'D010', 'MEN2', 'Dr. Kaung Htet', d.department_id, 'Clinical Psychology', '0911111120',
       'kaung.htet@hospital.com', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDa',
       15, '07:00:00', '22:00:00', 20, 'MBBS, M.Med.Sc (Psychiatry)', 5
FROM department d
WHERE d.department_name = 'Mental Health'
ON CONFLICT DO NOTHING;

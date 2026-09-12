-- ============================================================
-- V7__doctor_qualifications_and_phone_limit.sql
-- 1. Add doctor qualification and years of experience
-- 2. Allow up to 3 patients per phone number (drop strict unique constraint)
-- 3. Add doctor phone unique constraint to prevent duplicate doctors
-- 4. Update doctor working hours to 9:00 AM - 4:30 PM with standby doctors
-- ============================================================

-- Add qualification and years_of_experience to doctor
ALTER TABLE doctor ADD COLUMN IF NOT EXISTS qualification VARCHAR(100) DEFAULT 'MBBS';
ALTER TABLE doctor ADD COLUMN IF NOT EXISTS years_of_experience INT DEFAULT 5;
ALTER TABLE doctor ADD COLUMN IF NOT EXISTS specialization VARCHAR(100) DEFAULT 'General Medicine';

-- Drop strict unique constraint on patient(phone) if exists, allowing multi-profile per phone
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.table_constraints 
        WHERE table_name = 'patient' AND constraint_name = 'patient_phone_key'
    ) THEN
        ALTER TABLE patient DROP CONSTRAINT patient_phone_key;
    END IF;
END $$;

-- Ensure doctor phone has an index / unique constraint
CREATE UNIQUE INDEX IF NOT EXISTS idx_doctor_phone_unique ON doctor(phone) WHERE phone IS NOT NULL AND phone <> '';

-- Update default doctor records with qualifications, experience, and 09:00:00 - 16:30:00 hours
UPDATE doctor SET 
    qualification = CASE 
        WHEN doctor_code = 'CAR1' THEN 'MBBS, M.Med.Sc (Int. Med), MRCP (UK)'
        WHEN doctor_code = 'CAR2' THEN 'MBBS, Dr.Med.Sc (Cardiology)'
        WHEN doctor_code = 'NEU1' THEN 'MBBS, M.Med.Sc (Neurology), FRCP'
        WHEN doctor_code = 'ORT1' THEN 'MBBS, M.Med.Sc (Ortho), FRCS'
        WHEN doctor_code = 'GEN1' THEN 'MBBS, M.Med.Sc (General Medicine)'
        WHEN doctor_code = 'GEN2' THEN 'MBBS, M.Med.Sc (Geriatrics)'
        WHEN doctor_code = 'PED1' THEN 'MBBS, DCH, M.Med.Sc (Pediatrics)'
        WHEN doctor_code = 'DER1' THEN 'MBBS, Dip Dermatology, M.Med.Sc'
        ELSE COALESCE(qualification, 'MBBS, M.Med.Sc')
    END,
    specialization = CASE
        WHEN doctor_code = 'CAR1' THEN 'Interventional Cardiology'
        WHEN doctor_code = 'CAR2' THEN 'Cardiac Electrophysiology'
        WHEN doctor_code = 'NEU1' THEN 'Clinical Neurology'
        WHEN doctor_code = 'ORT1' THEN 'Orthopedic & Joint Surgery'
        WHEN doctor_code = 'GEN1' THEN 'Internal Medicine'
        WHEN doctor_code = 'GEN2' THEN 'Geriatric Medicine'
        WHEN doctor_code = 'PED1' THEN 'Child Health & Pediatrics'
        WHEN doctor_code = 'DER1' THEN 'Dermatology & Skin Care'
        ELSE COALESCE(specialization, 'General Medicine')
    END,
    years_of_experience = CASE
        WHEN doctor_code IN ('CAR1', 'NEU1', 'GEN1') THEN 12
        WHEN doctor_code IN ('CAR2', 'ORT1', 'PED1') THEN 9
        WHEN doctor_code IN ('GEN2', 'DER1') THEN 7
        ELSE COALESCE(years_of_experience, 5)
    END,
    queue_open_time = '09:00:00',
    queue_close_time = '16:30:00',
    is_available = TRUE,
    is_active = TRUE
WHERE is_active = TRUE;


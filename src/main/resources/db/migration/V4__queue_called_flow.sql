-- ============================================================
-- V4__queue_called_flow.sql
-- Adds the CALLED state to the smart queue engine:
--   WAITING -> CALLED (patient called) -> SERVING (consultation started)
-- Also seeds the waiting-expiry setting used by the queue scheduler.
-- ============================================================

-- ---------- Call next patient (WAITING -> CALLED, no consultation yet) ----------
CREATE OR REPLACE FUNCTION call_next_patient(p_doctor_id VARCHAR)
RETURNS BIGINT LANGUAGE plpgsql AS $$
DECLARE
    v_queue_id BIGINT;
BEGIN
    SELECT queue_id INTO v_queue_id
    FROM queue
    WHERE doctor_id = p_doctor_id AND status = 'WAITING'
    ORDER BY priority_sort(priority), position
    LIMIT 1;

    IF v_queue_id IS NOT NULL THEN
        UPDATE queue
        SET status = 'CALLED', called_at = CURRENT_TIMESTAMP
        WHERE queue_id = v_queue_id;

        INSERT INTO queue_history (queue_id, queue_number, patient_id, doctor_id, status)
        SELECT queue_id, queue_number, patient_id, doctor_id, 'CALLED' FROM queue WHERE queue_id = v_queue_id;
    END IF;

    RETURN v_queue_id;
END;
$$;

-- ---------- Start consultation (CALLED -> SERVING) ----------
CREATE OR REPLACE FUNCTION start_consultation(p_doctor_id VARCHAR)
RETURNS BIGINT LANGUAGE plpgsql AS $$
DECLARE
    v_queue_id BIGINT;
BEGIN
    SELECT queue_id INTO v_queue_id
    FROM queue
    WHERE doctor_id = p_doctor_id AND status = 'CALLED'
    ORDER BY called_at
    LIMIT 1;

    IF v_queue_id IS NOT NULL THEN
        UPDATE queue
        SET status = 'SERVING', started_at = CURRENT_TIMESTAMP
        WHERE queue_id = v_queue_id;

        INSERT INTO queue_history (queue_id, queue_number, patient_id, doctor_id, status)
        SELECT queue_id, queue_number, patient_id, doctor_id, 'SERVING' FROM queue WHERE queue_id = v_queue_id;
    END IF;

    RETURN v_queue_id;
END;
$$;

-- ---------- Seed: how long a waiting registration stays valid before expiry ----------
INSERT INTO system_setting (setting_key, setting_value, description, is_public)
VALUES ('waiting_expiry_minutes', '120', 'Minutes a waiting queue is kept before it expires (abandoned)', FALSE)
ON CONFLICT (setting_key) DO NOTHING;
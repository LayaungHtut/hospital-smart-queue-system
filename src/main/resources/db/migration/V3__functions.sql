-- ============================================================
-- V3__functions.sql
-- Hospital Smart Queue System - PostgreSQL functions
-- for the smart queue engine (queue numbers, priority ordering)
-- ============================================================

-- ---------- Next queue number for a doctor (per day) ----------
CREATE OR REPLACE FUNCTION generate_queue_number(p_doctor_code VARCHAR, p_date DATE DEFAULT CURRENT_DATE)
RETURNS VARCHAR LANGUAGE plpgsql AS $$
DECLARE
    v_prefix VARCHAR;
    v_last_seq INTEGER;
    v_queue_number VARCHAR;
BEGIN
    v_prefix := 'Q' || p_doctor_code || TO_CHAR(p_date, 'YYYYMMDD');
    SELECT COALESCE(MAX(CAST(SUBSTRING(queue_number FROM LENGTH(v_prefix) + 1) AS INTEGER)), 0)
    INTO v_last_seq
    FROM queue
    WHERE queue_number LIKE v_prefix || '%';
    v_queue_number := v_prefix || LPAD((v_last_seq + 1)::VARCHAR, 4, '0');
    RETURN v_queue_number;
END;
$$;

-- ---------- Priority sort value: EMERGENCY(1) > APPOINTMENT(2) > NORMAL(3) ----------
CREATE OR REPLACE FUNCTION priority_sort(p_priority VARCHAR)
RETURNS INT LANGUAGE plpgsql AS $$
BEGIN
    RETURN CASE p_priority
        WHEN 'EMERGENCY' THEN 1
        WHEN 'APPOINTMENT' THEN 2
        ELSE 3
    END;
END;
$$;

-- ---------- Next patient to call for a doctor (priority then position) ----------
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
        SET status = 'SERVING', called_at = CURRENT_TIMESTAMP, started_at = CURRENT_TIMESTAMP
        WHERE queue_id = v_queue_id;

        INSERT INTO queue_history (queue_id, queue_number, patient_id, doctor_id, status)
        SELECT queue_id, queue_number, patient_id, doctor_id, 'SERVING' FROM queue WHERE queue_id = v_queue_id;
    END IF;

    RETURN v_queue_id;
END;
$$;

-- ---------- Complete current patient of a doctor ----------
CREATE OR REPLACE FUNCTION complete_current_patient(p_doctor_id VARCHAR)
RETURNS BOOLEAN LANGUAGE plpgsql AS $$
DECLARE
    v_queue_id BIGINT;
BEGIN
    SELECT queue_id INTO v_queue_id
    FROM queue
    WHERE doctor_id = p_doctor_id AND status = 'SERVING'
    LIMIT 1;

    IF v_queue_id IS NOT NULL THEN
        UPDATE queue
        SET status = 'COMPLETED', completed_at = CURRENT_TIMESTAMP
        WHERE queue_id = v_queue_id;

        INSERT INTO queue_history (queue_id, queue_number, patient_id, doctor_id, status)
        SELECT queue_id, queue_number, patient_id, doctor_id, 'COMPLETED' FROM queue WHERE queue_id = v_queue_id;
        RETURN TRUE;
    END IF;
    RETURN FALSE;
END;
$$;

-- ---------- Queue statistics for a doctor dashboard ----------
CREATE OR REPLACE FUNCTION get_doctor_queue_stats(p_doctor_id VARCHAR)
RETURNS TABLE (waiting_count BIGINT, serving_count BIGINT, completed_today BIGINT, cancelled_today BIGINT)
LANGUAGE plpgsql AS $$
BEGIN
    RETURN QUERY
    SELECT
        COUNT(*) FILTER (WHERE status = 'WAITING'),
        COUNT(*) FILTER (WHERE status = 'SERVING'),
        COUNT(*) FILTER (WHERE status = 'COMPLETED' AND completed_at::DATE = CURRENT_DATE),
        COUNT(*) FILTER (WHERE status = 'CANCELLED' AND cancelled_at::DATE = CURRENT_DATE)
    FROM queue
    WHERE doctor_id = p_doctor_id;
END;
$$;

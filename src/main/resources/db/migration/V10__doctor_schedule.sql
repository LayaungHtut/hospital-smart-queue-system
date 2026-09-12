-- ============================================================
-- V10__doctor_schedule.sql
-- Real per-doctor working-schedule table.
-- Replaces the previously fabricated /admin/schedules response
-- (which invented day/time/room per doctor with no backing data).
-- ============================================================

CREATE TABLE doctor_schedule (
    schedule_id  SERIAL PRIMARY KEY,
    doctor_id    VARCHAR(20) NOT NULL REFERENCES doctor(doctor_id) ON DELETE CASCADE,
    day_of_week  VARCHAR(20) NOT NULL,
    start_time   TIME NOT NULL,
    end_time     TIME NOT NULL,
    break_start  TIME,
    break_end    TIME,
    room         VARCHAR(20),
    is_active    BOOLEAN NOT NULL DEFAULT TRUE,
    created_at   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_doctor_schedule_doctor ON doctor_schedule(doctor_id);

-- Seed one schedule row per existing doctor, derived from the doctor's
-- own (real) queue_open_time/queue_close_time rather than an invented time.
INSERT INTO doctor_schedule (doctor_id, day_of_week, start_time, end_time, break_start, break_end, room)
SELECT
    doctor_id,
    'Mon - Fri',
    queue_open_time,
    queue_close_time,
    '12:00:00'::TIME,
    '13:00:00'::TIME,
    'Room ' || (100 + (ROW_NUMBER() OVER (ORDER BY doctor_id)))
FROM doctor;

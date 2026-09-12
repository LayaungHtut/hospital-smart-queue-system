-- ============================================================
-- V1__create_schema.sql
-- Hospital Smart Queue System - Neon (PostgreSQL) initial schema
-- ============================================================

-- Required extensions (safe on Neon)
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- ---------- Departments ----------
CREATE TABLE department (
    department_id   SERIAL PRIMARY KEY,
    department_code VARCHAR(10)  NOT NULL UNIQUE,
    department_name VARCHAR(100) NOT NULL,
    is_active       BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ---------- Doctors ----------
CREATE TABLE doctor (
    doctor_id                     VARCHAR(20) PRIMARY KEY,
    doctor_code                   VARCHAR(10) NOT NULL UNIQUE,
    name                          VARCHAR(100) NOT NULL,
    department_id                 INT NOT NULL REFERENCES department(department_id),
    specialization                VARCHAR(100),
    phone                         VARCHAR(20),
    email                         VARCHAR(100),
    password_hash                 VARCHAR(255),
    max_queue_size                INT  NOT NULL DEFAULT 20,
    queue_open_time               TIME NOT NULL DEFAULT '07:00:00',
    queue_close_time              TIME NOT NULL DEFAULT '22:00:00',
    average_consultation_minutes  INT  NOT NULL DEFAULT 15,
    is_available                  BOOLEAN NOT NULL DEFAULT TRUE,
    is_active                     BOOLEAN NOT NULL DEFAULT TRUE,
    created_at                    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at                    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ---------- Patients ----------
CREATE TABLE patient (
    patient_id     VARCHAR(20) PRIMARY KEY,
    name           VARCHAR(100) NOT NULL,
    phone          VARCHAR(20)  NOT NULL UNIQUE,
    password_hash  VARCHAR(255) NOT NULL,
    email          VARCHAR(100),
    address        VARCHAR(255),
    date_of_birth  DATE,
    gender         VARCHAR(10),
    is_new_patient BOOLEAN NOT NULL DEFAULT TRUE,
    registered_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ---------- Staff ----------
CREATE TABLE staff (
    staff_id      VARCHAR(20) PRIMARY KEY,
    staff_code    VARCHAR(10) UNIQUE,
    name          VARCHAR(100) NOT NULL,
    phone         VARCHAR(20),
    email         VARCHAR(100),
    password_hash VARCHAR(255) NOT NULL,
    role          VARCHAR(20) NOT NULL DEFAULT 'STAFF',
    is_active     BOOLEAN NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ---------- Admin ----------
CREATE TABLE admin_user (
    admin_id      VARCHAR(20) PRIMARY KEY,
    username      VARCHAR(50) NOT NULL UNIQUE,
    name          VARCHAR(100) NOT NULL,
    email         VARCHAR(100),
    password_hash VARCHAR(255) NOT NULL,
    role          VARCHAR(20) NOT NULL DEFAULT 'ADMIN',
    is_active     BOOLEAN NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ---------- Priority (lookup used by the smart queue engine) ----------
CREATE TABLE priority (
    priority_id     SERIAL PRIMARY KEY,
    priority_code   VARCHAR(20) NOT NULL UNIQUE,
    priority_name   VARCHAR(50) NOT NULL,
    sort_order      INT NOT NULL DEFAULT 99,
    weight          NUMERIC(5,2) NOT NULL DEFAULT 1.00,
    is_active       BOOLEAN NOT NULL DEFAULT TRUE
);

-- ---------- Queue (core smart-queue-engine table) ----------
CREATE TABLE queue (
    queue_id                BIGSERIAL PRIMARY KEY,
    queue_number            VARCHAR(30) NOT NULL,
    patient_id              VARCHAR(20) NOT NULL REFERENCES patient(patient_id),
    doctor_id               VARCHAR(20) NOT NULL REFERENCES doctor(doctor_id),
    department_id           INT NOT NULL REFERENCES department(department_id),
    priority                VARCHAR(20) NOT NULL DEFAULT 'NORMAL',
    status                  VARCHAR(20) NOT NULL DEFAULT 'WAITING',
    position                INT NOT NULL,
    estimated_waiting_time  BIGINT NOT NULL DEFAULT 0,
    source                  VARCHAR(20) NOT NULL DEFAULT 'ONLINE',
    is_emergency            BOOLEAN NOT NULL DEFAULT FALSE,
    emergency_confirmed     BOOLEAN NOT NULL DEFAULT FALSE,
    called_at               TIMESTAMP,
    started_at              TIMESTAMP,
    paused_at               TIMESTAMP,
    completed_at            TIMESTAMP,
    cancelled_at            TIMESTAMP,
    cancel_reason           TEXT,
    created_at              TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    checked_in_at           TIMESTAMP
);

CREATE INDEX idx_queue_patient_status ON queue(patient_id, status);
CREATE INDEX idx_queue_doctor_status ON queue(doctor_id, status);
CREATE INDEX idx_queue_department_status ON queue(department_id, status);
CREATE INDEX idx_queue_priority_status ON queue(priority, status);

-- ---------- Queue History (optional audit of every change) ----------
CREATE TABLE queue_history (
    history_id   BIGSERIAL PRIMARY KEY,
    queue_id     BIGINT,
    queue_number VARCHAR(30),
    patient_id   VARCHAR(20),
    doctor_id    VARCHAR(20),
    status       VARCHAR(20),
    change_reason TEXT,
    created_at   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_queue_history_queue ON queue_history(queue_id);

-- ---------- Appointments ----------
CREATE TABLE appointment (
    appointment_id   BIGSERIAL PRIMARY KEY,
    patient_id       VARCHAR(20) NOT NULL REFERENCES patient(patient_id),
    doctor_id        VARCHAR(20) NOT NULL REFERENCES doctor(doctor_id),
    department_id    INT NOT NULL REFERENCES department(department_id),
    appointment_date DATE NOT NULL,
    appointment_time TIME,
    status           VARCHAR(20) NOT NULL DEFAULT 'SCHEDULED',
    queue_id         BIGINT,
    notes            TEXT,
    created_at       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_appointment_patient_date ON appointment(patient_id, appointment_date);
CREATE INDEX idx_appointment_doctor_date ON appointment(doctor_id, appointment_date);

-- ---------- Notifications ----------
CREATE TABLE notification (
    notification_id BIGSERIAL PRIMARY KEY,
    patient_id      VARCHAR(20) NOT NULL REFERENCES patient(patient_id),
    message         VARCHAR(1000) NOT NULL,
    is_read         BOOLEAN NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_notification_patient_read ON notification(patient_id, is_read);

-- ---------- System Settings (queue capacity, working hours, etc.) ----------
CREATE TABLE system_setting (
    setting_key   VARCHAR(100) PRIMARY KEY,
    setting_value TEXT NOT NULL,
    description   TEXT,
    is_public     BOOLEAN NOT NULL DEFAULT TRUE,
    updated_at    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ---------- Audit Log (admin reports) ----------
CREATE TABLE audit_log (
    audit_id    BIGSERIAL PRIMARY KEY,
    user_id     VARCHAR(50),
    user_type   VARCHAR(20),
    action      VARCHAR(100) NOT NULL,
    entity_type VARCHAR(50),
    entity_id   VARCHAR(50),
    details     TEXT,
    created_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_audit_log_created ON audit_log(created_at);

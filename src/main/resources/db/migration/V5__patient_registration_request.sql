-- ============================================================
-- V5__patient_registration_request.sql
-- Patient registration approval workflow by Administrator
-- ============================================================

CREATE TABLE IF NOT EXISTS patient_registration_request (
    request_id     BIGSERIAL PRIMARY KEY,
    name           VARCHAR(100) NOT NULL,
    phone          VARCHAR(20)  NOT NULL,
    password_hash  VARCHAR(255) NOT NULL,
    email          VARCHAR(100),
    address        VARCHAR(255),
    date_of_birth  DATE,
    gender         VARCHAR(10),
    status         VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    created_at     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    reviewed_at    TIMESTAMP,
    reviewed_by    VARCHAR(50),
    rejection_reason TEXT
);

CREATE INDEX IF NOT EXISTS idx_reg_req_phone ON patient_registration_request(phone);
CREATE INDEX IF NOT EXISTS idx_reg_req_status ON patient_registration_request(status);


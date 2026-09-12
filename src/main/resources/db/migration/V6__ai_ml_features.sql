-- ============================================================
-- V6__ai_ml_features.sql
-- Tables for AI/ML features: wait time predictions, no-show predictions,
-- triage assessments, chatbot logs, model metadata
-- ============================================================

-- Model metadata and versioning
CREATE TABLE IF NOT EXISTS ml_model (
    model_id          BIGSERIAL PRIMARY KEY,
    model_name        VARCHAR(100) NOT NULL UNIQUE,
    model_type        VARCHAR(50)  NOT NULL, -- 'wait_time', 'noshow', 'triage'
    framework         VARCHAR(50)  NOT NULL, -- 'xgboost', 'lightgbm', 'onnx'
    version           VARCHAR(20)  NOT NULL,
    file_path         VARCHAR(500) NOT NULL,
    artifacts_path    VARCHAR(500),
    metrics_json      JSONB, -- MAE, RMSE, AUC, etc.
    is_active         BOOLEAN      NOT NULL DEFAULT FALSE,
    trained_at        TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deployed_at       TIMESTAMP,
    created_by        VARCHAR(100),
    description       TEXT
);

CREATE INDEX IF NOT EXISTS idx_ml_model_type ON ml_model(model_type);
CREATE INDEX IF NOT EXISTS idx_ml_model_active ON ml_model(is_active);

-- Wait time predictions for audit/analytics
CREATE TABLE IF NOT EXISTS wait_time_prediction (
    prediction_id     BIGSERIAL PRIMARY KEY,
    queue_id          BIGINT       REFERENCES queue(queue_id) ON DELETE SET NULL,
    patient_id        VARCHAR(20)  REFERENCES patient(patient_id) ON DELETE SET NULL,
    doctor_id         VARCHAR(20)  REFERENCES doctor(doctor_id) ON DELETE SET NULL,
    department_id     INT          REFERENCES department(department_id) ON DELETE SET NULL,
    model_id          BIGINT       REFERENCES ml_model(model_id) ON DELETE SET NULL,
    
    -- Input features (JSON for flexibility)
    features_json     JSONB        NOT NULL,
    
    -- Prediction
    predicted_wait_min  NUMERIC(6,2) NOT NULL,
    actual_wait_min     NUMERIC(6,2), -- Filled after visit
    
    -- Metadata
    predicted_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    resolved_at       TIMESTAMP,
    
    -- Accuracy tracking
    error_minutes     NUMERIC(6,2) GENERATED ALWAYS AS (actual_wait_min - predicted_wait_min) STORED,
    absolute_error    NUMERIC(6,2) GENERATED ALWAYS AS (ABS(actual_wait_min - predicted_wait_min)) STORED
);

CREATE INDEX IF NOT EXISTS idx_wtp_queue ON wait_time_prediction(queue_id);
CREATE INDEX IF NOT EXISTS idx_wtp_doctor ON wait_time_prediction(doctor_id);
CREATE INDEX IF NOT EXISTS idx_wtp_predicted_at ON wait_time_prediction(predicted_at);
CREATE INDEX IF NOT EXISTS idx_wtp_model ON wait_time_prediction(model_id);

-- No-show predictions
CREATE TABLE IF NOT EXISTS noshow_prediction (
    prediction_id     BIGSERIAL PRIMARY KEY,
    appointment_id    BIGINT       REFERENCES appointment(appointment_id) ON DELETE CASCADE,
    patient_id        VARCHAR(20)  REFERENCES patient(patient_id) ON DELETE SET NULL,
    doctor_id         VARCHAR(20)  REFERENCES doctor(doctor_id) ON DELETE SET NULL,
    model_id          BIGINT       REFERENCES ml_model(model_id) ON DELETE SET NULL,
    
    features_json     JSONB        NOT NULL,
    
    predicted_probability NUMERIC(4,3) NOT NULL, -- 0.000 to 1.000
    risk_level        VARCHAR(10)  NOT NULL, -- 'LOW', 'MEDIUM', 'HIGH'
    threshold_used    NUMERIC(4,3) NOT NULL DEFAULT 0.500,
    predicted_noshow  BOOLEAN      NOT NULL,
    
    -- Outcome
    actual_noshow     BOOLEAN,
    
    predicted_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    resolved_at       TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_nsp_appointment ON noshow_prediction(appointment_id);
CREATE INDEX IF NOT EXISTS idx_nsp_patient ON noshow_prediction(patient_id);
CREATE INDEX IF NOT EXISTS idx_nsp_predicted_at ON noshow_prediction(predicted_at);
CREATE INDEX IF NOT EXISTS idx_nsp_risk ON noshow_prediction(risk_level);

-- Triage assessments (enhanced recommendations)
CREATE TABLE IF NOT EXISTS triage_assessment (
    assessment_id     BIGSERIAL PRIMARY KEY,
    patient_id        VARCHAR(20)  REFERENCES patient(patient_id) ON DELETE SET NULL,
    queue_id          BIGINT       REFERENCES queue(queue_id) ON DELETE SET NULL,
    department_id     INT          REFERENCES department(department_id) ON DELETE SET NULL,
    
    symptoms_text     TEXT         NOT NULL,
    
    -- AI Output
    recommended_dept_code VARCHAR(10),
    emergency_flag    BOOLEAN      NOT NULL DEFAULT FALSE,
    acuity_score      INT          NOT NULL CHECK (acuity_score BETWEEN 1 AND 5),
    disposition       VARCHAR(20)  NOT NULL, -- 'routine', 'urgent', 'emergency'
    recommended_tests JSONB, -- Array of test names
    recommended_labs  JSONB, -- Array of lab names
    reason            TEXT,
    ai_used           BOOLEAN      NOT NULL DEFAULT FALSE,
    model_version     VARCHAR(50),
    
    -- Staff override
    staff_acuity_score INT,
    staff_disposition  VARCHAR(20),
    staff_notes        TEXT,
    reviewed_by        VARCHAR(50),
    reviewed_at        TIMESTAMP,
    
    created_at         TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_ta_patient ON triage_assessment(patient_id);
CREATE INDEX IF NOT EXISTS idx_ta_queue ON triage_assessment(queue_id);
CREATE INDEX IF NOT EXISTS idx_ta_acuity ON triage_assessment(acuity_score);
CREATE INDEX IF NOT EXISTS idx_ta_created ON triage_assessment(created_at);

-- Chatbot conversation logs
CREATE TABLE IF NOT EXISTS chatbot_conversation (
    conversation_id   BIGSERIAL PRIMARY KEY,
    session_id        VARCHAR(100) NOT NULL,
    user_type         VARCHAR(20)  NOT NULL, -- 'patient', 'staff', 'anonymous'
    user_id           VARCHAR(50), -- patient_id, staff_id, or null
    
    message_role      VARCHAR(20)  NOT NULL, -- 'user', 'assistant', 'system'
    message_content   TEXT         NOT NULL,
    
    -- RAG context
    retrieved_chunks  JSONB, -- Array of {content, score, source}
    response_time_ms  INT,
    
    -- Feedback
    helpful           BOOLEAN,
    feedback_text     TEXT,
    
    created_at        TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_cc_session ON chatbot_conversation(session_id);
CREATE INDEX IF NOT EXISTS idx_cc_user ON chatbot_conversation(user_type, user_id);
CREATE INDEX IF NOT EXISTS idx_cc_created ON chatbot_conversation(created_at);

-- Queue acuity/escalation log (for auto-escalation tracking)
CREATE TABLE IF NOT EXISTS queue_acuity_log (
    log_id            BIGSERIAL PRIMARY KEY,
    queue_id          BIGINT       NOT NULL REFERENCES queue(queue_id) ON DELETE CASCADE,
    triage_id         BIGINT       REFERENCES triage_assessment(assessment_id) ON DELETE SET NULL,
    
    original_priority INT          NOT NULL, -- 1, 2, 3
    escalated_priority INT         NOT NULL, -- 1, 2, 3
    acuity_score      INT          NOT NULL,
    disposition       VARCHAR(20)  NOT NULL,
    
    escalation_reason VARCHAR(100), -- 'high_acuity', 'emergency_flag', 'staff_override'
    triggered_by      VARCHAR(50), -- 'ai', 'staff', 'system'
    
    created_at        TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_qal_queue ON queue_acuity_log(queue_id);
CREATE INDEX IF NOT EXISTS idx_qal_created ON queue_acuity_log(created_at);

-- Insert default active models (placeholders - updated when real models deployed)
INSERT INTO ml_model (model_name, model_type, framework, version, file_path, artifacts_path, metrics_json, is_active, trained_at, description)
VALUES 
    ('wait_time_xgboost_v1', 'wait_time', 'xgboost', '1.0.0', 
     'models/wait_time_xgboost.onnx', 'models/wait_time_xgboost_artifacts.json',
     '{"mae": 4.2, "rmse": 6.8, "r2": 0.82}', TRUE, CURRENT_TIMESTAMP,
     'XGBoost model for wait time prediction with 22 features'),
    ('noshow_xgboost_v1', 'noshow', 'xgboost', '1.0.0',
     'models/noshow_xgboost.onnx', 'models/noshow_xgboost_artifacts.json',
     '{"auc": 0.87, "accuracy": 0.82, "precision": 0.71, "recall": 0.68}', TRUE, CURRENT_TIMESTAMP,
     'XGBoost model for no-show prediction with 30 features')
ON CONFLICT (model_name) DO NOTHING;

-- Helpful view for model performance monitoring
CREATE OR REPLACE VIEW v_model_performance AS
SELECT 
    m.model_name,
    m.model_type,
    m.version,
    m.is_active,
    m.metrics_json as training_metrics,
    -- Wait time model performance
    CASE WHEN m.model_type = 'wait_time' THEN (
        SELECT jsonb_build_object(
            'count', COUNT(*),
            'mae', AVG(absolute_error),
            'rmse', SQRT(AVG(error_minutes * error_minutes)),
            'within_5min', COUNT(*) FILTER (WHERE absolute_error <= 5) * 100.0 / COUNT(*),
            'within_10min', COUNT(*) FILTER (WHERE absolute_error <= 10) * 100.0 / COUNT(*)
        )
        FROM wait_time_prediction wp
        WHERE wp.model_id = m.model_id AND wp.actual_wait_min IS NOT NULL
    ) END as wait_time_metrics,
    -- No-show model performance
    CASE WHEN m.model_type = 'noshow' THEN (
        SELECT jsonb_build_object(
            'count', COUNT(*),
            'accuracy', AVG(CASE WHEN predicted_noshow = actual_noshow THEN 1.0 ELSE 0.0 END),
            'precision', 
                COUNT(*) FILTER (WHERE predicted_noshow AND actual_noshow) * 1.0 / 
                NULLIF(COUNT(*) FILTER (WHERE predicted_noshow), 0),
            'recall',
                COUNT(*) FILTER (WHERE predicted_noshow AND actual_noshow) * 1.0 / 
                NULLIF(COUNT(*) FILTER (WHERE actual_noshow), 0)
        )
        FROM noshow_prediction np
        WHERE np.model_id = m.model_id AND np.actual_noshow IS NOT NULL
    ) END as noshow_metrics
FROM ml_model m;
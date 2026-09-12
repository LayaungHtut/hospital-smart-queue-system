-- Symptom master table
CREATE TABLE IF NOT EXISTS symptom (
    symptom_id    SERIAL PRIMARY KEY,
    symptom_name  VARCHAR(100) NOT NULL UNIQUE,
    symptom_code  VARCHAR(20)  NOT NULL UNIQUE,
    category      VARCHAR(50),
    is_active     BOOLEAN NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Junction table: queue <-> symptom (many-to-many)
CREATE TABLE IF NOT EXISTS queue_symptom (
    queue_id    BIGINT NOT NULL REFERENCES queue(queue_id) ON DELETE CASCADE,
    symptom_id  INT    NOT NULL REFERENCES symptom(symptom_id) ON DELETE CASCADE,
    PRIMARY KEY (queue_id, symptom_id)
);

CREATE INDEX IF NOT EXISTS idx_qs_queue ON queue_symptom(queue_id);
CREATE INDEX IF NOT EXISTS idx_qs_symptom ON queue_symptom(symptom_id);

-- Seed common symptoms
INSERT INTO symptom (symptom_name, symptom_code, category) VALUES
    ('Fever', 'FEVER', 'General'),
    ('Headache', 'HEADACHE', 'Neurological'),
    ('Cough', 'COUGH', 'Respiratory'),
    ('Sore Throat', 'SORE_THROAT', 'Respiratory'),
    ('Chest Pain', 'CHEST_PAIN', 'Cardiac'),
    ('Shortness of Breath', 'SOB', 'Respiratory'),
    ('Abdominal Pain', 'ABDOMINAL_PAIN', 'Gastrointestinal'),
    ('Nausea', 'NAUSEA', 'Gastrointestinal'),
    ('Vomiting', 'VOMITING', 'Gastrointestinal'),
    ('Diarrhea', 'DIARRHEA', 'Gastrointestinal'),
    ('Dizziness', 'DIZZINESS', 'Neurological'),
    ('Fatigue', 'FATIGUE', 'General'),
    ('Joint Pain', 'JOINT_PAIN', 'Musculoskeletal'),
    ('Back Pain', 'BACK_PAIN', 'Musculoskeletal'),
    ('Skin Rash', 'SKIN_RASH', 'Dermatological'),
    ('Itching', 'ITCHING', 'Dermatological'),
    ('Watery Eyes', 'WATERY_EYES', 'Ophthalmological'),
    ('Ear Pain', 'EAR_PAIN', 'ENT'),
    ('Nasal Congestion', 'NASAL_CONGESTION', 'Respiratory'),
    ('Loss of Appetite', 'LOSS_OF_APPETITE', 'General'),
    ('Weight Loss', 'WEIGHT_LOSS', 'General'),
    ('High Blood Pressure', 'HYPERTENSION', 'Cardiac'),
    ('Palpitations', 'PALPITATIONS', 'Cardiac'),
    ('Muscle Pain', 'MUSCLE_PAIN', 'Musculoskeletal'),
    ('Swelling', 'SWELLING', 'Musculoskeletal'),
    ('Difficulty Sleeping', 'INSOMNIA', 'General'),
    ('Anxiety', 'ANXIETY', 'Psychological'),
    ('Blurred Vision', 'BLURRED_VISION', 'Ophthalmological'),
    ('Numbness', 'NUMBNESS', 'Neurological'),
    ('Seizure', 'SEIZURE', 'Neurological')
ON CONFLICT (symptom_code) DO NOTHING;

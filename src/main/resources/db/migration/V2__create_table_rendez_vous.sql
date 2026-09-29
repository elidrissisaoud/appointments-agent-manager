CREATE TABLE rendez_vous (
    id BIGSERIAL PRIMARY KEY,
    patient_id BIGINT NOT NULL,
    google_event_id VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_rendez_vous_patient FOREIGN KEY (patient_id) REFERENCES patient(id) ON DELETE CASCADE
);

CREATE INDEX idx_rendez_vous_patient ON rendez_vous(patient_id);
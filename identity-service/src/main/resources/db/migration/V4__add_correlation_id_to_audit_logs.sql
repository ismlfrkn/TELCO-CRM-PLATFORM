-- V4__add_correlation_id_to_audit_logs.sql
-- Bir audit kaydini, o istegi ureten log satirlariyla (CorrelationIdFilter/MDC) eslestirebilmek icin.
ALTER TABLE audit_logs ADD COLUMN correlation_id VARCHAR(100);

CREATE INDEX idx_audit_logs_correlation_id ON audit_logs (correlation_id);

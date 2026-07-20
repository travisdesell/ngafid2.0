--liquibase formatted sql

--changeset ngafid:uploads-fleet-start-time-index labels:accounts,uploads,performance
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'uploads' AND INDEX_NAME = 'idx_uploads_fleet_start_time'
CREATE INDEX idx_uploads_fleet_start_time ON uploads (fleet_id, start_time);

--changeset ngafid:uploads-start-time-index labels:accounts,uploads,performance
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'uploads' AND INDEX_NAME = 'idx_uploads_start_time'
CREATE INDEX idx_uploads_start_time ON uploads (start_time);

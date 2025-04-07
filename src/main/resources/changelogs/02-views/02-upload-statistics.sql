--liquibase formatted sql

--changeset josh:fleet-monthly-upload-counts labels:uploads,views,materialized-views context:hourly-materialized-view runAlways:true
CREATE OR REPLACE VIEW
    v_fleet_monthly_upload_counts AS
SELECT
    fleet_id,
    YEAR(start_time) as year,
    MONTH(start_time) as month,
    SUM(status != 'DERIVED') as upload_count,
    SUM(status = 'PROCESSED_OK') as ok_count,
    SUM(status = 'PROCESSED_WARNING') as warning_count,
    SUM(status LIKE 'FAILED%') as error_count
FROM
    uploads
GROUP BY
    fleet_id,
    YEAR(start_time),
    MONTH(start_time);

CREATE TABLE IF NOT EXISTS m_fleet_monthly_upload_counts (
    fleet_id INT NOT NULL,
    year INT NOT NULL,
    month INT NOT NULL,
    upload_count INT NOT NULL,
    ok_count INT NOT NULL,
    warning_count INT NOT NULL,
    error_count INT NOT NULL,

    PRIMARY KEY (fleet_id, year, month),
    FOREIGN KEY (fleet_id) REFERENCES fleet(id)
);

TRUNCATE TABLE m_fleet_monthly_upload_counts;

INSERT INTO m_fleet_monthly_upload_counts
    SELECT fleet_id, year, month, upload_count, ok_count, warning_count, error_count FROM v_fleet_monthly_upload_counts;


--changeset josh:aggregate-upload-counts labels:uploads,views
CREATE VIEW
    v_aggregate_monthly_upload_counts AS
SELECT
    year,
    month,
    SUM(monthly.upload_count) as upload_count,
    SUM(monthly.ok_count) as ok_count,
    SUM(monthly.warning_count) as warning_count,
    SUM(monthly.error_count) as error_count
FROM
    m_fleet_monthly_upload_counts as monthly
GROUP BY
    year,
    month;


--changeset josh:fleet-yearly-upload-counts labels:uploads,views
CREATE VIEW
    v_fleet_yearly_upload_counts AS
SELECT
    fleet_id,
    year,
    SUM(monthly.upload_count) as upload_count,
    SUM(monthly.ok_count) as ok_count,
    SUM(monthly.warning_count) as warning_count,
    SUM(monthly.error_count) as error_count
FROM
    m_fleet_monthly_upload_counts as monthly
GROUP BY
    fleet_id,
    year;


--changeset josh:aggregate-yearly-upload-counts labels:uploads,views
CREATE VIEW
    v_aggregate_yearly_upload_counts AS
SELECT
    year,
    SUM(monthly.upload_count) as upload_count,
    SUM(monthly.ok_count) as ok_count,
    SUM(monthly.warning_count) as warning_count,
    SUM(monthly.error_count) as error_count
FROM
    m_fleet_monthly_upload_counts as monthly
GROUP BY
    year;


--changeset josh:fleet-upload-counts labels:uploads,views
CREATE VIEW
    v_fleet_upload_counts AS
SELECT
    fleet_id,
    SUM(monthly.upload_count) as upload_count,
    SUM(monthly.ok_count) as ok_count,
    SUM(monthly.warning_count) as warning_count,
    SUM(monthly.error_count) as error_count
FROM
    m_fleet_monthly_upload_counts as monthly
GROUP BY
    fleet_id;


--changeset josh:aggregate-upload-count labels:uploads,views
CREATE VIEW
    v_aggregate_upload_counts AS
SELECT
    SUM(monthly.upload_count) as upload_count,
    SUM(monthly.ok_count) as ok_count,
    SUM(monthly.warning_count) as warning_count,
    SUM(monthly.error_count) as error_count
FROM
    m_fleet_monthly_upload_counts as monthly;
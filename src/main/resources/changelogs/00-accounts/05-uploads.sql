--liquibase formatted sql

--changeset josh:uploads labels:accounts
CREATE TABLE uploads (
    id INT NOT NULL AUTO_INCREMENT,
    parent_id INT NULL DEFAULT NULL,
    fleet_id INT NOT NULL,
    uploader_id INT NOT NULL,
    
    filename VARCHAR(256) NOT NULL,
    identifier VARCHAR(128) NOT NULL,
    kind enum('FILE', 'AIRSYNC', 'DERIVED') DEFAULT 'FILE',
    status varchar(16),
    
    number_chunks INT NOT NULL,
    uploaded_chunks INT NOT NULL,
    chunk_status VARCHAR(8096) NOT NULL,
    md5_hash VARCHAR(32) NOT NULL,
    size_bytes BIGINT NOT NULL,
    bytes_uploaded BIGINT DEFAULT 0,
    
    start_time datetime,
    end_time datetime,

    n_valid_flights INT DEFAULT 0,
    n_warning_flights INT DEFAULT 0,
    n_error_flights INT DEFAULT 0,
    
    contains_rotorcraft TINYINT(1) NOT NULL DEFAULT 0,

    PRIMARY KEY (id),
     UNIQUE KEY (uploader_id, md5_hash),
     UNIQUE KEY (fleet_id, md5_hash),
     UNIQUE KEY (parent_id),
    FOREIGN KEY (fleet_id)      REFERENCES fleet(id),
    FOREIGN KEY (uploader_id)   REFERENCES user(id),
    FOREIGN KEY (parent_id)     REFERENCES uploads(id)
);

--changeset josh:uploads-foreign-keys
ALTER TABLE airsync_imports ADD CONSTRAINT fk_airsync_imports_upload_id
    FOREIGN KEY (upload_id) REFERENCES uploads(id);

ALTER TABLE flight_errors ADD CONSTRAINT fk_flight_errors_upload_id
    FOREIGN KEY (upload_id) REFERENCES uploads(id);

ALTER TABLE upload_errors ADD CONSTRAINT fk_upload_errors_upload_id
    FOREIGN KEY (upload_id) REFERENCES uploads(id);

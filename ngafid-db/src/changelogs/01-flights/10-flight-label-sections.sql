--liquibase formatted sql

--changeset roman:flight-label-sections labels:flights,labels
CREATE TABLE flight_label_section (
    id INT NOT NULL AUTO_INCREMENT,
    flight_id INT NOT NULL,

    start_index INT NOT NULL,
    end_index   INT NOT NULL,
    start_time  DATETIME NOT NULL,
    end_time    DATETIME NOT NULL,

    start_value DOUBLE NULL,
    end_value   DOUBLE NULL,

    label_text  VARCHAR(1024) NULL,

    created_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
                               ON UPDATE CURRENT_TIMESTAMP,

    PRIMARY KEY (id),
    INDEX idx_flight_label_section_flight_id (flight_id),
    CONSTRAINT fk_fls_flight
        FOREIGN KEY (flight_id) REFERENCES flights(id)
            ON DELETE CASCADE
);


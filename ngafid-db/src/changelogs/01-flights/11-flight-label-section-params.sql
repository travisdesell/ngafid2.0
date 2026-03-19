--liquibase formatted sql

--changeset roman:flight-label-section-params labels:flights,labels
CREATE TABLE flight_label_section_param (
    label_section_id INT NOT NULL,
    parameter_name   VARCHAR(128) NOT NULL,

    PRIMARY KEY (label_section_id, parameter_name),

    CONSTRAINT fk_flsp_label
        FOREIGN KEY (label_section_id) REFERENCES flight_label_section(id)
            ON DELETE CASCADE
);


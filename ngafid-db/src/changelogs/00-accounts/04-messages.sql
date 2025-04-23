--liquibase formatted sql

--changeset josh:flight-messages labels:messages
CREATE TABLE flight_messages (
    id INT NOT NULL AUTO_INCREMENT,
    message TEXT,

    PRIMARY KEY (id)
);

--changeset josh:flight-warnings labels:accounts,messages
CREATE TABLE flight_warnings (
    id INT NOT NULL AUTO_INCREMENT,
    flight_id INT NOT NULL,
    message_id INT NOT NULL,

    PRIMARY KEY (id),
    FOREIGN KEY (message_id) REFERENCES flight_messages(id)
);

--changeset josh:flight-errors labels:accounts,messages
CREATE TABLE flight_errors (
    id INT NOT NULL AUTO_INCREMENT,
    upload_id INT NOT NULL,
    filename VARCHAR(512) NOT NULL,
    message_id INT NOT NULL,

    PRIMARY KEY (id),
    FOREIGN KEY (message_id) REFERENCES flight_messages(id)
);

--changeset josh:upload-errorslabels:accounts,messages
CREATE TABLE upload_errors (
    id INT NOT NULL AUTO_INCREMENT,
    upload_id INT NOT NULL,
    message_id INT NOT NULL,

    PRIMARY KEY(id),
    FOREIGN KEY(message_id) REFERENCES flight_messages(id)
);

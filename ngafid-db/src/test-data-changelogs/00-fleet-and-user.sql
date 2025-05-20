--liquibase formatted sql

--changeset josh:make-dummy-fleet labels:accounts,fleet
INSERT INTO fleet
VALUES (1, 'Test Fleet with ID 1'),
       (2, 'Test Fleet with ID 2');

--changeset josh:make-dummy-users labels:accounts,users
INSERT INTO user
VALUES (1, 'test@email.com', 'aaaaaaaaaaaaaaaaaaaa', 'John', 'Doe', '123 House Road', 'CityName', 'CountryName',
        'StateName', '10001', '', '', CURRENT_DATE, 0, 0, CURRENT_DATE),
       (2, 'test1@email.com', 'aaaaaaaaaaaaaaaaaaaa', 'John Admin', 'Aggregate Doe', '123 House Road', 'CityName',
        'CountryName', 'StateName', '10001', '', '', CURRENT_DATE, 1, 1, CURRENT_DATE),
       (3, 'test2@email.com', 'aaaaaaaaaaaaaaaaaaaa', 'John Denied', 'Denied Doe', '123 House Road', 'CityName',
        'CountryName', 'StateName', '10001', '', '', CURRENT_DATE, 0, 0, CURRENT_DATE);


--changeset josh:make-dummy-fleet-access labels:accounts,fleet
INSERT INTO fleet_access
VALUES (1, 1, 'VIEW'),
       (2, 1, 'MANAGER'),
       (1, 2, 'WAITING'),
       (2, 2, 'WAITING'),
       (3, 1, 'DENIED'),
       (3, 2, 'DENIED');

--changeset josh:make-dummy-airsync-info labels:accounts,fleet
INSERT INTO airsync_fleet_info
VALUES (2, 'Airsync Name', 'API_KEY', 'API_SECRET', current_date, NULL, NULL);

--changeset josh:make-dummy-email-preferences labels:accounts,users
INSERT INTO email_preferences
VALUES (1, 'UPLOAD_PROCESS_START', 1),
       (2, 'UPLOAD_PROCESS_START', 0);

--changeset josh:make-dummy-uploads labels:uploads
INSERT INTO uploads
VALUES (1, NULL, 1, 1, 'SPOOF-FILENAME0.zip', 'SPOOF-FILENAME0.zip', 'FILE', 'PROCESSED_OK', 1, 1, '1', 'fakemd5hash',
        1, 1, CURRENT_DATE, CURRENT_DATE, 0, 0, 0, 0);

--changeset josh:make-dummy-tails labels:flights,tails
INSERT INTO tails
VALUES ('SYSTEM-ID-0', 1, 'FAKE0000', true);

--changeset josh:make-dummy-flights labels:flights
INSERT INTO flights
VALUES (1, 1, 1, 1, 'SYSTEM-ID-0', 1, CURRENT_DATE, CURRENT_DATE, 'SPOOF-FLIGHT0.csv', 'SPOOF-FLIGHT0-MD5', 100,
        'SUCCESS'),
       (2, 1, 1, 1, 'SYSTEM-ID-0', 1, CURRENT_DATE, CURRENT_DATE, 'SPOOF-FLIGHT1.csv', 'SPOOF-FLIGHT1-MD5', 100,
        'SUCCESS');
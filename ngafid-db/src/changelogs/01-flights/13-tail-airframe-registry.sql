--liquibase formatted sql

--changeset ngafid:tail-airframe-registry-airframes labels:flights,airframes,tail-airframe-registry
-- Seed rotorcraft airframe models referenced by tail_airframe_registry.csv

INSERT INTO airframes (airframe, type_id)
SELECT '407', t.id FROM airframe_types t WHERE t.name = 'Rotorcraft'
  AND NOT EXISTS (SELECT 1 FROM airframes a WHERE a.airframe = '407');

INSERT INTO airframes (airframe, type_id)
SELECT 'AS350', t.id FROM airframe_types t WHERE t.name = 'Rotorcraft'
  AND NOT EXISTS (SELECT 1 FROM airframes a WHERE a.airframe = 'AS350');

INSERT INTO airframes (airframe, type_id)
SELECT 'AW109', t.id FROM airframe_types t WHERE t.name = 'Rotorcraft'
  AND NOT EXISTS (SELECT 1 FROM airframes a WHERE a.airframe = 'AW109');

INSERT INTO airframes (airframe, type_id)
SELECT 'AW119', t.id FROM airframe_types t WHERE t.name = 'Rotorcraft'
  AND NOT EXISTS (SELECT 1 FROM airframes a WHERE a.airframe = 'AW119');

INSERT INTO airframes (airframe, type_id)
SELECT 'AW139', t.id FROM airframe_types t WHERE t.name = 'Rotorcraft'
  AND NOT EXISTS (SELECT 1 FROM airframes a WHERE a.airframe = 'AW139');

INSERT INTO airframes (airframe, type_id)
SELECT 'BK117', t.id FROM airframe_types t WHERE t.name = 'Rotorcraft'
  AND NOT EXISTS (SELECT 1 FROM airframes a WHERE a.airframe = 'BK117');

INSERT INTO airframes (airframe, type_id)
SELECT 'EC130', t.id FROM airframe_types t WHERE t.name = 'Rotorcraft'
  AND NOT EXISTS (SELECT 1 FROM airframes a WHERE a.airframe = 'EC130');

INSERT INTO airframes (airframe, type_id)
SELECT 'EC135', t.id FROM airframe_types t WHERE t.name = 'Rotorcraft'
  AND NOT EXISTS (SELECT 1 FROM airframes a WHERE a.airframe = 'EC135');

INSERT INTO airframes (airframe, type_id)
SELECT 'MH60', t.id FROM airframe_types t WHERE t.name = 'Rotorcraft'
  AND NOT EXISTS (SELECT 1 FROM airframes a WHERE a.airframe = 'MH60');

INSERT INTO airframes (airframe, type_id)
SELECT 'MH65', t.id FROM airframe_types t WHERE t.name = 'Rotorcraft'
  AND NOT EXISTS (SELECT 1 FROM airframes a WHERE a.airframe = 'MH65');

INSERT INTO airframes (airframe, type_id)
SELECT 'R44', t.id FROM airframe_types t WHERE t.name = 'Rotorcraft'
  AND NOT EXISTS (SELECT 1 FROM airframes a WHERE a.airframe = 'R44');

--changeset ngafid:tail-airframe-registry-table labels:flights,airframes,tail-airframe-registry
CREATE TABLE IF NOT EXISTS tail_airframe_registry (
    tail VARCHAR(16) NOT NULL,
    airframe_id INT NOT NULL,
    PRIMARY KEY (tail),
    CONSTRAINT tail_airframe_registry_airframe_fk
        FOREIGN KEY (airframe_id) REFERENCES airframes (id)
);

--changeset ngafid:tail-airframe-registry-data labels:flights,airframes,rotorcraft
-- Tail -> airframe mapping (445 rows); source: operator_tail_registry_dynamoDB_20260506.csv
DELETE FROM tail_airframe_registry;
INSERT INTO tail_airframe_registry (tail, airframe_id)
SELECT v.tail, a.id
FROM (
    SELECT '0095' AS tail, 'EC135' AS airframe
  UNION ALL
    SELECT '0130' AS tail, 'EC135' AS airframe
  UNION ALL
    SELECT '0131' AS tail, 'EC135' AS airframe
  UNION ALL
    SELECT '0132' AS tail, 'EC135' AS airframe
  UNION ALL
    SELECT '0133' AS tail, 'EC135' AS airframe
  UNION ALL
    SELECT '0134' AS tail, 'EC135' AS airframe
  UNION ALL
    SELECT '0143' AS tail, 'EC135' AS airframe
  UNION ALL
    SELECT '0144' AS tail, 'EC135' AS airframe
  UNION ALL
    SELECT '0154' AS tail, 'EC135' AS airframe
  UNION ALL
    SELECT '0157' AS tail, 'EC135' AS airframe
  UNION ALL
    SELECT '0161' AS tail, 'EC135' AS airframe
  UNION ALL
    SELECT '0167' AS tail, 'EC135' AS airframe
  UNION ALL
    SELECT '0179' AS tail, 'EC135' AS airframe
  UNION ALL
    SELECT '0249' AS tail, 'EC135' AS airframe
  UNION ALL
    SELECT '0330' AS tail, 'EC135' AS airframe
  UNION ALL
    SELECT '0414' AS tail, 'EC135' AS airframe
  UNION ALL
    SELECT '0417' AS tail, 'EC135' AS airframe
  UNION ALL
    SELECT '0451' AS tail, 'EC135' AS airframe
  UNION ALL
    SELECT '0485' AS tail, 'EC135' AS airframe
  UNION ALL
    SELECT '0491' AS tail, 'EC135' AS airframe
  UNION ALL
    SELECT '0511' AS tail, 'EC135' AS airframe
  UNION ALL
    SELECT '0541' AS tail, 'EC135' AS airframe
  UNION ALL
    SELECT '0559' AS tail, 'EC135' AS airframe
  UNION ALL
    SELECT '0562' AS tail, 'EC135' AS airframe
  UNION ALL
    SELECT '0591' AS tail, 'EC135' AS airframe
  UNION ALL
    SELECT '0598' AS tail, 'EC135' AS airframe
  UNION ALL
    SELECT '0613' AS tail, 'EC135' AS airframe
  UNION ALL
    SELECT '0641' AS tail, 'EC135' AS airframe
  UNION ALL
    SELECT '0662' AS tail, 'EC135' AS airframe
  UNION ALL
    SELECT '0667' AS tail, 'EC135' AS airframe
  UNION ALL
    SELECT '0707' AS tail, 'EC135' AS airframe
  UNION ALL
    SELECT '0713' AS tail, 'EC135' AS airframe
  UNION ALL
    SELECT '0735' AS tail, 'EC135' AS airframe
  UNION ALL
    SELECT '0740' AS tail, 'EC135' AS airframe
  UNION ALL
    SELECT '0758' AS tail, 'EC135' AS airframe
  UNION ALL
    SELECT '0763' AS tail, 'EC135' AS airframe
  UNION ALL
    SELECT '0764' AS tail, 'EC135' AS airframe
  UNION ALL
    SELECT '0817' AS tail, 'EC135' AS airframe
  UNION ALL
    SELECT '0834' AS tail, 'EC135' AS airframe
  UNION ALL
    SELECT '0849' AS tail, 'EC135' AS airframe
  UNION ALL
    SELECT '0858' AS tail, 'EC135' AS airframe
  UNION ALL
    SELECT '0862' AS tail, 'EC135' AS airframe
  UNION ALL
    SELECT '0890' AS tail, 'EC135' AS airframe
  UNION ALL
    SELECT '0897' AS tail, 'EC135' AS airframe
  UNION ALL
    SELECT '0913' AS tail, 'EC135' AS airframe
  UNION ALL
    SELECT '0942' AS tail, 'EC135' AS airframe
  UNION ALL
    SELECT '0946' AS tail, 'EC135' AS airframe
  UNION ALL
    SELECT '0972' AS tail, 'EC135' AS airframe
  UNION ALL
    SELECT '1069' AS tail, 'EC135' AS airframe
  UNION ALL
    SELECT '1093' AS tail, 'EC135' AS airframe
) AS v
INNER JOIN airframes a ON a.airframe = v.airframe;

INSERT INTO tail_airframe_registry (tail, airframe_id)
SELECT v.tail, a.id
FROM (
    SELECT '1096' AS tail, 'EC135' AS airframe
  UNION ALL
    SELECT '1105' AS tail, 'EC135' AS airframe
  UNION ALL
    SELECT '1233' AS tail, 'EC135' AS airframe
  UNION ALL
    SELECT '1245' AS tail, 'EC135' AS airframe
  UNION ALL
    SELECT '250TH' AS tail, 'R44' AS airframe
  UNION ALL
    SELECT '344Y' AS tail, 'R44' AS airframe
  UNION ALL
    SELECT '3837' AS tail, 'AS350' AS airframe
  UNION ALL
    SELECT '3962' AS tail, 'AS350' AS airframe
  UNION ALL
    SELECT '53482' AS tail, '407' AS airframe
  UNION ALL
    SELECT '53573' AS tail, '407' AS airframe
  UNION ALL
    SELECT '53627' AS tail, '407' AS airframe
  UNION ALL
    SELECT '53723' AS tail, '407' AS airframe
  UNION ALL
    SELECT '53790' AS tail, '407' AS airframe
  UNION ALL
    SELECT '53919' AS tail, '407' AS airframe
  UNION ALL
    SELECT '54066' AS tail, '407' AS airframe
  UNION ALL
    SELECT '54084' AS tail, '407' AS airframe
  UNION ALL
    SELECT '54305' AS tail, '407' AS airframe
  UNION ALL
    SELECT '54330' AS tail, '407' AS airframe
  UNION ALL
    SELECT '54341' AS tail, '407' AS airframe
  UNION ALL
    SELECT '54375' AS tail, '407' AS airframe
  UNION ALL
    SELECT '54376' AS tail, '407' AS airframe
  UNION ALL
    SELECT '54377' AS tail, '407' AS airframe
  UNION ALL
    SELECT '54379' AS tail, '407' AS airframe
  UNION ALL
    SELECT '54444' AS tail, '407' AS airframe
  UNION ALL
    SELECT '54446' AS tail, '407' AS airframe
  UNION ALL
    SELECT '54458' AS tail, '407' AS airframe
  UNION ALL
    SELECT '54459' AS tail, '407' AS airframe
  UNION ALL
    SELECT '54466' AS tail, '407' AS airframe
  UNION ALL
    SELECT '6001' AS tail, 'MH60' AS airframe
  UNION ALL
    SELECT '6002' AS tail, 'MH60' AS airframe
  UNION ALL
    SELECT '6003' AS tail, 'MH60' AS airframe
  UNION ALL
    SELECT '6004' AS tail, 'MH60' AS airframe
  UNION ALL
    SELECT '6005' AS tail, 'MH60' AS airframe
  UNION ALL
    SELECT '6006' AS tail, 'MH60' AS airframe
  UNION ALL
    SELECT '6007' AS tail, 'MH60' AS airframe
  UNION ALL
    SELECT '6008' AS tail, 'MH60' AS airframe
  UNION ALL
    SELECT '6009' AS tail, 'MH60' AS airframe
  UNION ALL
    SELECT '6010' AS tail, 'MH60' AS airframe
  UNION ALL
    SELECT '6011' AS tail, 'MH60' AS airframe
  UNION ALL
    SELECT '6013' AS tail, 'MH60' AS airframe
  UNION ALL
    SELECT '6014' AS tail, 'MH60' AS airframe
  UNION ALL
    SELECT '6015' AS tail, 'MH60' AS airframe
  UNION ALL
    SELECT '6016' AS tail, 'MH60' AS airframe
  UNION ALL
    SELECT '6018' AS tail, 'MH60' AS airframe
  UNION ALL
    SELECT '6019' AS tail, 'MH60' AS airframe
  UNION ALL
    SELECT '6021' AS tail, 'MH60' AS airframe
  UNION ALL
    SELECT '6022' AS tail, 'MH60' AS airframe
  UNION ALL
    SELECT '6023' AS tail, 'MH60' AS airframe
  UNION ALL
    SELECT '6024' AS tail, 'MH60' AS airframe
  UNION ALL
    SELECT '6025' AS tail, 'MH60' AS airframe
) AS v
INNER JOIN airframes a ON a.airframe = v.airframe;

INSERT INTO tail_airframe_registry (tail, airframe_id)
SELECT v.tail, a.id
FROM (
    SELECT '6026' AS tail, 'MH60' AS airframe
  UNION ALL
    SELECT '6027' AS tail, 'MH60' AS airframe
  UNION ALL
    SELECT '6029' AS tail, 'MH60' AS airframe
  UNION ALL
    SELECT '6030' AS tail, 'MH60' AS airframe
  UNION ALL
    SELECT '6031' AS tail, 'MH60' AS airframe
  UNION ALL
    SELECT '6032' AS tail, 'MH60' AS airframe
  UNION ALL
    SELECT '6033' AS tail, 'MH60' AS airframe
  UNION ALL
    SELECT '6034' AS tail, 'MH60' AS airframe
  UNION ALL
    SELECT '6035' AS tail, 'MH60' AS airframe
  UNION ALL
    SELECT '6036' AS tail, 'MH60' AS airframe
  UNION ALL
    SELECT '6037' AS tail, 'MH60' AS airframe
  UNION ALL
    SELECT '6038' AS tail, 'MH60' AS airframe
  UNION ALL
    SELECT '6039' AS tail, 'MH60' AS airframe
  UNION ALL
    SELECT '6040' AS tail, 'MH60' AS airframe
  UNION ALL
    SELECT '6041' AS tail, 'MH60' AS airframe
  UNION ALL
    SELECT '6042' AS tail, 'MH60' AS airframe
  UNION ALL
    SELECT '6043' AS tail, 'MH60' AS airframe
  UNION ALL
    SELECT '6044' AS tail, 'MH60' AS airframe
  UNION ALL
    SELECT '6045' AS tail, 'MH60' AS airframe
  UNION ALL
    SELECT '6046' AS tail, 'MH60' AS airframe
  UNION ALL
    SELECT '6047' AS tail, 'MH60' AS airframe
  UNION ALL
    SELECT '6048' AS tail, 'MH60' AS airframe
  UNION ALL
    SELECT '6049' AS tail, 'MH60' AS airframe
  UNION ALL
    SELECT '6050' AS tail, 'MH60' AS airframe
  UNION ALL
    SELECT '6051' AS tail, 'MH60' AS airframe
  UNION ALL
    SELECT '6052' AS tail, 'MH60' AS airframe
  UNION ALL
    SELECT '6053' AS tail, 'MH60' AS airframe
  UNION ALL
    SELECT '6054' AS tail, 'MH60' AS airframe
  UNION ALL
    SELECT '6501' AS tail, 'MH65' AS airframe
  UNION ALL
    SELECT '6502' AS tail, 'MH65' AS airframe
  UNION ALL
    SELECT '6503' AS tail, 'MH65' AS airframe
  UNION ALL
    SELECT '6504' AS tail, 'MH65' AS airframe
  UNION ALL
    SELECT '6506' AS tail, 'MH65' AS airframe
  UNION ALL
    SELECT '6507' AS tail, 'MH65' AS airframe
  UNION ALL
    SELECT '6508' AS tail, 'MH65' AS airframe
  UNION ALL
    SELECT '6509' AS tail, 'MH65' AS airframe
  UNION ALL
    SELECT '6510' AS tail, 'MH65' AS airframe
  UNION ALL
    SELECT '6511' AS tail, 'MH65' AS airframe
  UNION ALL
    SELECT '6512' AS tail, 'MH65' AS airframe
  UNION ALL
    SELECT '6513' AS tail, 'MH65' AS airframe
  UNION ALL
    SELECT '6514' AS tail, 'MH65' AS airframe
  UNION ALL
    SELECT '6516' AS tail, 'MH65' AS airframe
  UNION ALL
    SELECT '6517' AS tail, 'MH65' AS airframe
  UNION ALL
    SELECT '6518' AS tail, 'MH65' AS airframe
  UNION ALL
    SELECT '6519' AS tail, 'MH65' AS airframe
  UNION ALL
    SELECT '6520' AS tail, 'MH65' AS airframe
  UNION ALL
    SELECT '6521' AS tail, 'MH65' AS airframe
  UNION ALL
    SELECT '6524' AS tail, 'MH65' AS airframe
  UNION ALL
    SELECT '6525' AS tail, 'MH65' AS airframe
  UNION ALL
    SELECT '6526' AS tail, 'MH65' AS airframe
) AS v
INNER JOIN airframes a ON a.airframe = v.airframe;

INSERT INTO tail_airframe_registry (tail, airframe_id)
SELECT v.tail, a.id
FROM (
    SELECT '6527' AS tail, 'MH65' AS airframe
  UNION ALL
    SELECT '6528' AS tail, 'MH65' AS airframe
  UNION ALL
    SELECT '6529' AS tail, 'MH65' AS airframe
  UNION ALL
    SELECT '6530' AS tail, 'MH65' AS airframe
  UNION ALL
    SELECT '6531' AS tail, 'MH65' AS airframe
  UNION ALL
    SELECT '6532' AS tail, 'MH65' AS airframe
  UNION ALL
    SELECT '6533' AS tail, 'MH65' AS airframe
  UNION ALL
    SELECT '6534' AS tail, 'MH65' AS airframe
  UNION ALL
    SELECT '6536' AS tail, 'MH65' AS airframe
  UNION ALL
    SELECT '6537' AS tail, 'MH65' AS airframe
  UNION ALL
    SELECT '6538' AS tail, 'MH65' AS airframe
  UNION ALL
    SELECT '6539' AS tail, 'MH65' AS airframe
  UNION ALL
    SELECT '6540' AS tail, 'MH65' AS airframe
  UNION ALL
    SELECT '6542' AS tail, 'MH65' AS airframe
  UNION ALL
    SELECT '6543' AS tail, 'MH65' AS airframe
  UNION ALL
    SELECT '6544' AS tail, 'MH65' AS airframe
  UNION ALL
    SELECT '6545' AS tail, 'MH65' AS airframe
  UNION ALL
    SELECT '6547' AS tail, 'MH65' AS airframe
  UNION ALL
    SELECT '6548' AS tail, 'MH65' AS airframe
  UNION ALL
    SELECT '6550' AS tail, 'MH65' AS airframe
  UNION ALL
    SELECT '6551' AS tail, 'MH65' AS airframe
  UNION ALL
    SELECT '6552' AS tail, 'MH65' AS airframe
  UNION ALL
    SELECT '6553' AS tail, 'MH65' AS airframe
  UNION ALL
    SELECT '6554' AS tail, 'MH65' AS airframe
  UNION ALL
    SELECT '6555' AS tail, 'MH65' AS airframe
  UNION ALL
    SELECT '6556' AS tail, 'MH65' AS airframe
  UNION ALL
    SELECT '6557' AS tail, 'MH65' AS airframe
  UNION ALL
    SELECT '6558' AS tail, 'MH65' AS airframe
  UNION ALL
    SELECT '6559' AS tail, 'MH65' AS airframe
  UNION ALL
    SELECT '6560' AS tail, 'MH65' AS airframe
  UNION ALL
    SELECT '6561' AS tail, 'MH65' AS airframe
  UNION ALL
    SELECT '6562' AS tail, 'MH65' AS airframe
  UNION ALL
    SELECT '6563' AS tail, 'MH65' AS airframe
  UNION ALL
    SELECT '6564' AS tail, 'MH65' AS airframe
  UNION ALL
    SELECT '6565' AS tail, 'MH65' AS airframe
  UNION ALL
    SELECT '6566' AS tail, 'MH65' AS airframe
  UNION ALL
    SELECT '6567' AS tail, 'MH65' AS airframe
  UNION ALL
    SELECT '6568' AS tail, 'MH65' AS airframe
  UNION ALL
    SELECT '6569' AS tail, 'MH65' AS airframe
  UNION ALL
    SELECT '6570' AS tail, 'MH65' AS airframe
  UNION ALL
    SELECT '6571' AS tail, 'MH65' AS airframe
  UNION ALL
    SELECT '6572' AS tail, 'MH65' AS airframe
  UNION ALL
    SELECT '6573' AS tail, 'MH65' AS airframe
  UNION ALL
    SELECT '6574' AS tail, 'MH65' AS airframe
  UNION ALL
    SELECT '6575' AS tail, 'MH65' AS airframe
  UNION ALL
    SELECT '6576' AS tail, 'MH65' AS airframe
  UNION ALL
    SELECT '6577' AS tail, 'MH65' AS airframe
  UNION ALL
    SELECT '6578' AS tail, 'MH65' AS airframe
  UNION ALL
    SELECT '6579' AS tail, 'MH65' AS airframe
  UNION ALL
    SELECT '6580' AS tail, 'MH65' AS airframe
) AS v
INNER JOIN airframes a ON a.airframe = v.airframe;

INSERT INTO tail_airframe_registry (tail, airframe_id)
SELECT v.tail, a.id
FROM (
    SELECT '6581' AS tail, 'MH65' AS airframe
  UNION ALL
    SELECT '6582' AS tail, 'MH65' AS airframe
  UNION ALL
    SELECT '6583' AS tail, 'MH65' AS airframe
  UNION ALL
    SELECT '6584' AS tail, 'MH65' AS airframe
  UNION ALL
    SELECT '6585' AS tail, 'MH65' AS airframe
  UNION ALL
    SELECT '6586' AS tail, 'MH65' AS airframe
  UNION ALL
    SELECT '6587' AS tail, 'MH65' AS airframe
  UNION ALL
    SELECT '6588' AS tail, 'MH65' AS airframe
  UNION ALL
    SELECT '6589' AS tail, 'MH65' AS airframe
  UNION ALL
    SELECT '6590' AS tail, 'MH65' AS airframe
  UNION ALL
    SELECT '6591' AS tail, 'MH65' AS airframe
  UNION ALL
    SELECT '6592' AS tail, 'MH65' AS airframe
  UNION ALL
    SELECT '6593' AS tail, 'MH65' AS airframe
  UNION ALL
    SELECT '6594' AS tail, 'MH65' AS airframe
  UNION ALL
    SELECT '6595' AS tail, 'MH65' AS airframe
  UNION ALL
    SELECT '6596' AS tail, 'MH65' AS airframe
  UNION ALL
    SELECT '6597' AS tail, 'MH65' AS airframe
  UNION ALL
    SELECT '6598' AS tail, 'MH65' AS airframe
  UNION ALL
    SELECT '6601' AS tail, 'MH65' AS airframe
  UNION ALL
    SELECT '6603' AS tail, 'MH65' AS airframe
  UNION ALL
    SELECT '6604' AS tail, 'MH65' AS airframe
  UNION ALL
    SELECT '6605' AS tail, 'MH65' AS airframe
  UNION ALL
    SELECT '6606' AS tail, 'MH65' AS airframe
  UNION ALL
    SELECT '6607' AS tail, 'MH65' AS airframe
  UNION ALL
    SELECT '6608' AS tail, 'MH65' AS airframe
  UNION ALL
    SELECT '683R' AS tail, 'R44' AS airframe
  UNION ALL
    SELECT '7017' AS tail, 'EC130' AS airframe
  UNION ALL
    SELECT '7103' AS tail, 'EC130' AS airframe
  UNION ALL
    SELECT '7132' AS tail, 'EC130' AS airframe
  UNION ALL
    SELECT '7142' AS tail, 'EC130' AS airframe
  UNION ALL
    SELECT '7167' AS tail, 'EC130' AS airframe
  UNION ALL
    SELECT '7182' AS tail, 'EC130' AS airframe
  UNION ALL
    SELECT '7190' AS tail, 'EC130' AS airframe
  UNION ALL
    SELECT '7221' AS tail, 'EC130' AS airframe
  UNION ALL
    SELECT '7490' AS tail, 'EC130' AS airframe
  UNION ALL
    SELECT '7499' AS tail, 'EC130' AS airframe
  UNION ALL
    SELECT '79RH' AS tail, 'R44' AS airframe
  UNION ALL
    SELECT '9047' AS tail, 'BK117' AS airframe
  UNION ALL
    SELECT '9067' AS tail, 'BK117' AS airframe
  UNION ALL
    SELECT '9077' AS tail, 'BK117' AS airframe
  UNION ALL
    SELECT '9089' AS tail, 'BK117' AS airframe
  UNION ALL
    SELECT '9096' AS tail, 'BK117' AS airframe
  UNION ALL
    SELECT '9099' AS tail, 'BK117' AS airframe
  UNION ALL
    SELECT '9114' AS tail, 'BK117' AS airframe
  UNION ALL
    SELECT '9115' AS tail, 'BK117' AS airframe
  UNION ALL
    SELECT '9121' AS tail, 'BK117' AS airframe
  UNION ALL
    SELECT '9158' AS tail, 'BK117' AS airframe
  UNION ALL
    SELECT '9188' AS tail, 'BK117' AS airframe
  UNION ALL
    SELECT '9209' AS tail, 'BK117' AS airframe
  UNION ALL
    SELECT '9226' AS tail, 'BK117' AS airframe
) AS v
INNER JOIN airframes a ON a.airframe = v.airframe;

INSERT INTO tail_airframe_registry (tail, airframe_id)
SELECT v.tail, a.id
FROM (
    SELECT '9228' AS tail, 'BK117' AS airframe
  UNION ALL
    SELECT '9241' AS tail, 'BK117' AS airframe
  UNION ALL
    SELECT '9250' AS tail, 'BK117' AS airframe
  UNION ALL
    SELECT '9278' AS tail, 'BK117' AS airframe
  UNION ALL
    SELECT '9281' AS tail, 'BK117' AS airframe
  UNION ALL
    SELECT '9288' AS tail, 'BK117' AS airframe
  UNION ALL
    SELECT '9301' AS tail, 'BK117' AS airframe
  UNION ALL
    SELECT '9311' AS tail, 'BK117' AS airframe
  UNION ALL
    SELECT '9319' AS tail, 'BK117' AS airframe
  UNION ALL
    SELECT '9327' AS tail, 'BK117' AS airframe
  UNION ALL
    SELECT '9348' AS tail, 'BK117' AS airframe
  UNION ALL
    SELECT '9353' AS tail, 'BK117' AS airframe
  UNION ALL
    SELECT '9357' AS tail, 'BK117' AS airframe
  UNION ALL
    SELECT '9367' AS tail, 'BK117' AS airframe
  UNION ALL
    SELECT '9421' AS tail, 'BK117' AS airframe
  UNION ALL
    SELECT '9494' AS tail, 'BK117' AS airframe
  UNION ALL
    SELECT '9529' AS tail, 'BK117' AS airframe
  UNION ALL
    SELECT '9535' AS tail, 'BK117' AS airframe
  UNION ALL
    SELECT '9542' AS tail, 'BK117' AS airframe
  UNION ALL
    SELECT '9545' AS tail, 'BK117' AS airframe
  UNION ALL
    SELECT '9556' AS tail, 'BK117' AS airframe
  UNION ALL
    SELECT '9586' AS tail, 'BK117' AS airframe
  UNION ALL
    SELECT '9601' AS tail, 'BK117' AS airframe
  UNION ALL
    SELECT '9685' AS tail, 'BK117' AS airframe
  UNION ALL
    SELECT '9729' AS tail, 'BK117' AS airframe
  UNION ALL
    SELECT '9739' AS tail, 'BK117' AS airframe
  UNION ALL
    SELECT '9760' AS tail, 'BK117' AS airframe
  UNION ALL
    SELECT '9800' AS tail, 'BK117' AS airframe
  UNION ALL
    SELECT '9827' AS tail, 'BK117' AS airframe
  UNION ALL
    SELECT '9832' AS tail, 'BK117' AS airframe
  UNION ALL
    SELECT '9837' AS tail, 'BK117' AS airframe
  UNION ALL
    SELECT '9850' AS tail, 'BK117' AS airframe
  UNION ALL
    SELECT '9852' AS tail, 'BK117' AS airframe
  UNION ALL
    SELECT '9855' AS tail, 'BK117' AS airframe
  UNION ALL
    SELECT '9862' AS tail, 'BK117' AS airframe
  UNION ALL
    SELECT '9863' AS tail, 'BK117' AS airframe
  UNION ALL
    SELECT '9864' AS tail, 'BK117' AS airframe
  UNION ALL
    SELECT '9867' AS tail, 'BK117' AS airframe
  UNION ALL
    SELECT '9868' AS tail, 'BK117' AS airframe
  UNION ALL
    SELECT '9873' AS tail, 'BK117' AS airframe
  UNION ALL
    SELECT '9874' AS tail, 'BK117' AS airframe
  UNION ALL
    SELECT '9875' AS tail, 'BK117' AS airframe
  UNION ALL
    SELECT '9876' AS tail, 'BK117' AS airframe
  UNION ALL
    SELECT '9877' AS tail, 'BK117' AS airframe
  UNION ALL
    SELECT '9879' AS tail, 'BK117' AS airframe
  UNION ALL
    SELECT '9880' AS tail, 'BK117' AS airframe
  UNION ALL
    SELECT '9881' AS tail, 'BK117' AS airframe
  UNION ALL
    SELECT '9884' AS tail, 'BK117' AS airframe
  UNION ALL
    SELECT 'N106VU' AS tail, 'EC130' AS airframe
  UNION ALL
    SELECT 'N107VU' AS tail, 'EC130' AS airframe
) AS v
INNER JOIN airframes a ON a.airframe = v.airframe;

INSERT INTO tail_airframe_registry (tail, airframe_id)
SELECT v.tail, a.id
FROM (
    SELECT 'N108LN' AS tail, 'AS350' AS airframe
  UNION ALL
    SELECT 'N118LN' AS tail, 'AS350' AS airframe
  UNION ALL
    SELECT 'N11UQ' AS tail, 'EC130' AS airframe
  UNION ALL
    SELECT 'N11XQ' AS tail, 'EC130' AS airframe
  UNION ALL
    SELECT 'N11YQ' AS tail, 'EC130' AS airframe
  UNION ALL
    SELECT 'N135AM' AS tail, '407' AS airframe
  UNION ALL
    SELECT 'N141NE' AS tail, 'BK117' AS airframe
  UNION ALL
    SELECT 'N142NE' AS tail, 'BK117' AS airframe
  UNION ALL
    SELECT 'N143NE' AS tail, 'BK117' AS airframe
  UNION ALL
    SELECT 'N144NE' AS tail, 'BK117' AS airframe
  UNION ALL
    SELECT 'N145NE' AS tail, 'BK117' AS airframe
  UNION ALL
    SELECT 'N147LM' AS tail, 'AS350' AS airframe
  UNION ALL
    SELECT 'N1984S' AS tail, 'BK117' AS airframe
  UNION ALL
    SELECT 'N208AM' AS tail, '407' AS airframe
  UNION ALL
    SELECT 'N214AM' AS tail, '407' AS airframe
  UNION ALL
    SELECT 'N216SH' AS tail, 'EC130' AS airframe
  UNION ALL
    SELECT 'N218AM' AS tail, '407' AS airframe
  UNION ALL
    SELECT 'N222SH' AS tail, 'EC130' AS airframe
  UNION ALL
    SELECT 'N230TJ' AS tail, 'EC130' AS airframe
  UNION ALL
    SELECT 'N237SH' AS tail, 'EC130' AS airframe
  UNION ALL
    SELECT 'N239SH' AS tail, 'EC130' AS airframe
  UNION ALL
    SELECT 'N240SH' AS tail, 'EC130' AS airframe
  UNION ALL
    SELECT 'N242SH' AS tail, 'EC130' AS airframe
  UNION ALL
    SELECT 'N244SH' AS tail, 'EC130' AS airframe
  UNION ALL
    SELECT 'N245CC' AS tail, 'BK117' AS airframe
  UNION ALL
    SELECT 'N246AM' AS tail, '407' AS airframe
  UNION ALL
    SELECT 'N246NE' AS tail, 'BK117' AS airframe
  UNION ALL
    SELECT 'N247NE' AS tail, 'BK117' AS airframe
  UNION ALL
    SELECT 'N253LF' AS tail, 'AS350' AS airframe
  UNION ALL
    SELECT 'N255LF' AS tail, 'AS350' AS airframe
  UNION ALL
    SELECT 'N257AM' AS tail, 'AS350' AS airframe
  UNION ALL
    SELECT 'N267AM' AS tail, 'AS350' AS airframe
  UNION ALL
    SELECT 'N278AM' AS tail, '407' AS airframe
  UNION ALL
    SELECT 'N279AM' AS tail, 'AS350' AS airframe
  UNION ALL
    SELECT 'N27RT' AS tail, 'AW139' AS airframe
  UNION ALL
    SELECT 'N284AM' AS tail, '407' AS airframe
  UNION ALL
    SELECT 'N292AM' AS tail, '407' AS airframe
  UNION ALL
    SELECT 'N293AM' AS tail, '407' AS airframe
  UNION ALL
    SELECT 'N29RT' AS tail, 'AW139' AS airframe
  UNION ALL
    SELECT 'N304ME' AS tail, 'BK117' AS airframe
  UNION ALL
    SELECT 'N306LG' AS tail, 'AW119' AS airframe
  UNION ALL
    SELECT 'N307ME' AS tail, 'BK117' AS airframe
  UNION ALL
    SELECT 'N308ME' AS tail, 'EC135' AS airframe
  UNION ALL
    SELECT 'N327CH' AS tail, 'EC130' AS airframe
  UNION ALL
    SELECT 'N338AM' AS tail, '407' AS airframe
  UNION ALL
    SELECT 'N350AM' AS tail, 'AS350' AS airframe
  UNION ALL
    SELECT 'N350LF' AS tail, 'AS350' AS airframe
  UNION ALL
    SELECT 'N350MV' AS tail, 'AS350' AS airframe
  UNION ALL
    SELECT 'N351LL' AS tail, 'AW119' AS airframe
  UNION ALL
    SELECT 'N352LL' AS tail, 'AW119' AS airframe
) AS v
INNER JOIN airframes a ON a.airframe = v.airframe;

INSERT INTO tail_airframe_registry (tail, airframe_id)
SELECT v.tail, a.id
FROM (
    SELECT 'N353LL' AS tail, 'AW119' AS airframe
  UNION ALL
    SELECT 'N354CF' AS tail, 'AS350' AS airframe
  UNION ALL
    SELECT 'N354LL' AS tail, 'AW119' AS airframe
  UNION ALL
    SELECT 'N355LL' AS tail, 'AW119' AS airframe
  UNION ALL
    SELECT 'N356LL' AS tail, 'AW119' AS airframe
  UNION ALL
    SELECT 'N357AM' AS tail, '407' AS airframe
  UNION ALL
    SELECT 'N357LL' AS tail, 'AW119' AS airframe
  UNION ALL
    SELECT 'N358LL' AS tail, 'AW119' AS airframe
  UNION ALL
    SELECT 'N359LL' AS tail, 'AW119' AS airframe
  UNION ALL
    SELECT 'N360LL' AS tail, 'AW119' AS airframe
  UNION ALL
    SELECT 'N362AH' AS tail, 'EC135' AS airframe
  UNION ALL
    SELECT 'N363AH' AS tail, 'EC135' AS airframe
  UNION ALL
    SELECT 'N366AH' AS tail, 'EC135' AS airframe
  UNION ALL
    SELECT 'N370AM' AS tail, '407' AS airframe
  UNION ALL
    SELECT 'N372AM' AS tail, '407' AS airframe
  UNION ALL
    SELECT 'N3831' AS tail, 'AS350' AS airframe
  UNION ALL
    SELECT 'N383AM' AS tail, '407' AS airframe
  UNION ALL
    SELECT 'N391LG' AS tail, 'AS350' AS airframe
  UNION ALL
    SELECT 'N398AM' AS tail, '407' AS airframe
  UNION ALL
    SELECT 'N399LG' AS tail, 'AS350' AS airframe
  UNION ALL
    SELECT 'N404AM' AS tail, '407' AS airframe
  UNION ALL
    SELECT 'N407HC' AS tail, '407' AS airframe
  UNION ALL
    SELECT 'N408LM' AS tail, '407' AS airframe
  UNION ALL
    SELECT 'N408LN' AS tail, '407' AS airframe
  UNION ALL
    SELECT 'N408SH' AS tail, '407' AS airframe
  UNION ALL
    SELECT 'N409LM' AS tail, '407' AS airframe
  UNION ALL
    SELECT 'N410W' AS tail, 'EC130' AS airframe
  UNION ALL
    SELECT 'N418TY' AS tail, 'AS350' AS airframe
  UNION ALL
    SELECT 'N434AM' AS tail, '407' AS airframe
  UNION ALL
    SELECT 'N442ME' AS tail, 'EC135' AS airframe
  UNION ALL
    SELECT 'N445ME' AS tail, 'EC135' AS airframe
  UNION ALL
    SELECT 'N448ME' AS tail, 'EC135' AS airframe
  UNION ALL
    SELECT 'N4497Y' AS tail, 'AS350' AS airframe
  UNION ALL
    SELECT 'N450AM' AS tail, '407' AS airframe
  UNION ALL
    SELECT 'N458AM' AS tail, '407' AS airframe
  UNION ALL
    SELECT 'N459AM' AS tail, '407' AS airframe
  UNION ALL
    SELECT 'N470WC' AS tail, '407' AS airframe
  UNION ALL
    SELECT 'N490H' AS tail, 'AS350' AS airframe
  UNION ALL
    SELECT 'N491LG' AS tail, 'AS350' AS airframe
  UNION ALL
    SELECT 'N493LG' AS tail, 'AS350' AS airframe
  UNION ALL
    SELECT 'N494LG' AS tail, 'AS350' AS airframe
  UNION ALL
    SELECT 'N496LG' AS tail, 'AS350' AS airframe
  UNION ALL
    SELECT 'N507ME' AS tail, 'BK117' AS airframe
  UNION ALL
    SELECT 'N508AM' AS tail, 'AS350' AS airframe
  UNION ALL
    SELECT 'N520ME' AS tail, 'EC135' AS airframe
  UNION ALL
    SELECT 'N522ME' AS tail, 'BK117' AS airframe
  UNION ALL
    SELECT 'N527ME' AS tail, 'EC135' AS airframe
  UNION ALL
    SELECT 'N530ME' AS tail, 'EC135' AS airframe
  UNION ALL
    SELECT 'N536ME' AS tail, 'EC135' AS airframe
  UNION ALL
    SELECT 'N544AM' AS tail, 'AS350' AS airframe
) AS v
INNER JOIN airframes a ON a.airframe = v.airframe;

INSERT INTO tail_airframe_registry (tail, airframe_id)
SELECT v.tail, a.id
FROM (
    SELECT 'N574AM' AS tail, 'AS350' AS airframe
  UNION ALL
    SELECT 'N586TS' AS tail, 'R44' AS airframe
  UNION ALL
    SELECT 'N624EC' AS tail, 'BK117' AS airframe
  UNION ALL
    SELECT 'N625PA' AS tail, '407' AS airframe
  UNION ALL
    SELECT 'N639ME' AS tail, 'EC135' AS airframe
  UNION ALL
    SELECT 'N693AM' AS tail, '407' AS airframe
  UNION ALL
    SELECT 'N697AM' AS tail, '407' AS airframe
  UNION ALL
    SELECT 'N702SA' AS tail, 'AS350' AS airframe
  UNION ALL
    SELECT 'N710SB' AS tail, 'AW109' AS airframe
  UNION ALL
    SELECT 'N741AM' AS tail, '407' AS airframe
  UNION ALL
    SELECT 'N743AM' AS tail, '407' AS airframe
  UNION ALL
    SELECT 'N7940K' AS tail, 'EC130' AS airframe
  UNION ALL
    SELECT 'N798AC' AS tail, 'EC130' AS airframe
  UNION ALL
    SELECT 'N805LF' AS tail, 'AS350' AS airframe
  UNION ALL
    SELECT 'N807LF' AS tail, 'AS350' AS airframe
  UNION ALL
    SELECT 'N808LF' AS tail, 'AS350' AS airframe
  UNION ALL
    SELECT 'N808SA' AS tail, 'AS350' AS airframe
  UNION ALL
    SELECT 'N810LF' AS tail, 'AS350' AS airframe
  UNION ALL
    SELECT 'N811ME' AS tail, 'EC135' AS airframe
  UNION ALL
    SELECT 'N831ME' AS tail, 'EC135' AS airframe
  UNION ALL
    SELECT 'N845ME' AS tail, 'EC135' AS airframe
  UNION ALL
    SELECT 'N855CH' AS tail, 'EC130' AS airframe
  UNION ALL
    SELECT 'N855ME' AS tail, 'EC135' AS airframe
  UNION ALL
    SELECT 'N87ME' AS tail, 'BK117' AS airframe
  UNION ALL
    SELECT 'N884ME' AS tail, 'EC135' AS airframe
  UNION ALL
    SELECT 'N8CP' AS tail, 'AW139' AS airframe
  UNION ALL
    SELECT 'N901LF' AS tail, 'AW109' AS airframe
  UNION ALL
    SELECT 'N901NM' AS tail, '407' AS airframe
  UNION ALL
    SELECT 'N901WM' AS tail, 'AW109' AS airframe
  UNION ALL
    SELECT 'N901XM' AS tail, 'AW109' AS airframe
  UNION ALL
    SELECT 'N913SM' AS tail, 'AS350' AS airframe
  UNION ALL
    SELECT 'N914SM' AS tail, 'AS350' AS airframe
  UNION ALL
    SELECT 'N915ME' AS tail, 'EC135' AS airframe
  UNION ALL
    SELECT 'N925MD' AS tail, '407' AS airframe
  UNION ALL
    SELECT 'N927AM' AS tail, '407' AS airframe
  UNION ALL
    SELECT 'N930U' AS tail, 'EC130' AS airframe
  UNION ALL
    SELECT 'N932ME' AS tail, 'EC135' AS airframe
  UNION ALL
    SELECT 'N941AM' AS tail, '407' AS airframe
  UNION ALL
    SELECT 'N944ME' AS tail, 'EC135' AS airframe
  UNION ALL
    SELECT 'N947MD' AS tail, '407' AS airframe
  UNION ALL
    SELECT 'N956AM' AS tail, '407' AS airframe
  UNION ALL
    SELECT 'N963SA' AS tail, 'AS350' AS airframe
  UNION ALL
    SELECT 'N972AC' AS tail, 'BK117' AS airframe
  UNION ALL
    SELECT 'N975SC' AS tail, '407' AS airframe
  UNION ALL
    SELECT 'N9CP' AS tail, 'AW139' AS airframe
) AS v
INNER JOIN airframes a ON a.airframe = v.airframe;

--changeset ngafid:tail-airframe-registry-n272mj labels:flights,airframes,rotorcraft
INSERT INTO tail_airframe_registry (tail, airframe_id)
SELECT 'N272MJ', a.id FROM airframes a WHERE a.airframe = '407'
ON DUPLICATE KEY UPDATE airframe_id = VALUES(airframe_id);

--changeset ngafid:tail-airframe-registry-n158-n159-am labels:flights,airframes,rotorcraft
INSERT INTO tail_airframe_registry (tail, airframe_id)
SELECT v.tail, a.id
FROM (
    SELECT 'N158AM' AS tail, '407' AS airframe
  UNION ALL
    SELECT 'N159AM' AS tail, '407' AS airframe
) AS v
INNER JOIN airframes a ON a.airframe = v.airframe
ON DUPLICATE KEY UPDATE airframe_id = VALUES(airframe_id);

--changeset ngafid:tail-airframe-registry-air-methods-cf labels:flights,airframes,rotorcraft
INSERT INTO tail_airframe_registry (tail, airframe_id)
SELECT v.tail, a.id
FROM (
    SELECT 'N902CF' AS tail, '407' AS airframe
  UNION ALL
    SELECT 'N903CF' AS tail, '407' AS airframe
) AS v
INNER JOIN airframes a ON a.airframe = v.airframe
ON DUPLICATE KEY UPDATE airframe_id = VALUES(airframe_id);

--changeset ngafid:tail-airframe-registry-air-methods-batch labels:flights,airframes,rotorcraft
-- Idempotent batch for Air Methods uploads (Garmin 407 + Appareo CF tails)
INSERT INTO tail_airframe_registry (tail, airframe_id)
SELECT v.tail, a.id
FROM (
    SELECT 'N272MJ' AS tail, '407' AS airframe
  UNION ALL
    SELECT 'N158AM' AS tail, '407' AS airframe
  UNION ALL
    SELECT 'N159AM' AS tail, '407' AS airframe
  UNION ALL
    SELECT 'N404AM' AS tail, '407' AS airframe
  UNION ALL
    SELECT 'N450AM' AS tail, '407' AS airframe
  UNION ALL
    SELECT 'N902CF' AS tail, '407' AS airframe
  UNION ALL
    SELECT 'N903CF' AS tail, '407' AS airframe
) AS v
INNER JOIN airframes a ON a.airframe = v.airframe
ON DUPLICATE KEY UPDATE airframe_id = VALUES(airframe_id);

--changeset ngafid:tail-airframe-registry-lifelink-n365ll labels:flights,airframes,rotorcraft
INSERT INTO tail_airframe_registry (tail, airframe_id)
SELECT 'N365LL', a.id FROM airframes a WHERE a.airframe = 'AW119'
ON DUPLICATE KEY UPDATE airframe_id = VALUES(airframe_id);

--changeset ngafid:tail-airframe-registry-airframe-id-migrate labels:flights,airframes,tail-airframe-registry
--preconditions onFail:MARK_RAN onError:MARK_RAN
--precondition-sql-check expectedResult:1 SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tail_airframe_registry' AND COLUMN_NAME = 'airframe'
--comment Migrate legacy varchar airframe FK to airframe_id (MySQL charset/FK compatibility)
ALTER TABLE tail_airframe_registry ADD COLUMN airframe_id INT NULL;
UPDATE tail_airframe_registry r
    INNER JOIN airframes a ON a.airframe = r.airframe
SET r.airframe_id = a.id;
ALTER TABLE tail_airframe_registry DROP FOREIGN KEY tail_airframe_registry_ibfk_1;
ALTER TABLE tail_airframe_registry DROP COLUMN airframe;
ALTER TABLE tail_airframe_registry MODIFY airframe_id INT NOT NULL;
ALTER TABLE tail_airframe_registry
    ADD CONSTRAINT tail_airframe_registry_airframe_fk
        FOREIGN KEY (airframe_id) REFERENCES airframes (id);
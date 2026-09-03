-- Data_generator 및 Ai_server 표준(lux)에 맞춰 조도 단위를 lx에서 lux로 변경
UPDATE sensor_type
SET value_unit = 'lux'
WHERE type = 'LIGHT'
  AND value_unit = 'lx';

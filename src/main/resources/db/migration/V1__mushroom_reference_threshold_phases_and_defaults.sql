-- Existing threshold rows predate phase support. Preserve their current meaning as growth-phase references.
ALTER TABLE mushroom_reference_threshold
    ADD COLUMN IF NOT EXISTS threshold_type varchar(20);

UPDATE mushroom_reference_threshold
SET threshold_type = 'GROWTH'
WHERE threshold_type IS NULL;

ALTER TABLE mushroom_reference_threshold
    ALTER COLUMN threshold_type SET NOT NULL;

DO $$
DECLARE
    constraint_name text;
BEGIN
    FOR constraint_name IN
        SELECT c.conname
        FROM pg_constraint c
        WHERE c.conrelid = 'mushroom_reference_threshold'::regclass
          AND c.contype = 'u'
          AND (
              SELECT array_agg(a.attname ORDER BY a.attname)
              FROM unnest(c.conkey) AS key(attnum)
              JOIN pg_attribute a ON a.attrelid = c.conrelid AND a.attnum = key.attnum
          ) = ARRAY['mushroom_reference_id', 'sensor_type_id']::name[]
    LOOP
        EXECUTE format('ALTER TABLE mushroom_reference_threshold DROP CONSTRAINT %I', constraint_name);
    END LOOP;

    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint c
        WHERE c.conrelid = 'mushroom_reference_threshold'::regclass
          AND c.contype = 'u'
          AND (
              SELECT array_agg(a.attname ORDER BY a.attname)
              FROM unnest(c.conkey) AS key(attnum)
              JOIN pg_attribute a ON a.attrelid = c.conrelid AND a.attnum = key.attnum
          ) = ARRAY['mushroom_reference_id', 'sensor_type_id', 'threshold_type']::name[]
    ) THEN
        ALTER TABLE mushroom_reference_threshold
            ADD CONSTRAINT uk_mushroom_reference_threshold_sensor_reference_phase
                UNIQUE (sensor_type_id, mushroom_reference_id, threshold_type);
    END IF;
END $$;

INSERT INTO sensor_type (type, value_unit)
VALUES
    ('TEMPERATURE', '℃'),
    ('HUMIDITY', '%'),
    ('CO2', 'ppm'),
    ('LIGHT', 'lux')
ON CONFLICT (type, value_unit) DO NOTHING;

WITH default_mushrooms(name_ko, name_en, scientific_name, temp_growth_min, temp_growth_max, temp_harvest_min, temp_harvest_max, humidity_growth_min, humidity_growth_max, humidity_harvest_min, humidity_harvest_max, co2_growth_min, co2_growth_max, co2_harvest_min, co2_harvest_max, light_growth_min, light_growth_max, light_harvest_min, light_harvest_max) AS (
    VALUES
        ('느타리버섯', 'Oyster mushroom', 'Pleurotus ostreatus', 18.0, 24.0, 13.0, 18.0, 80.0, 90.0, 85.0, 95.0, 800.0, 1500.0, 600.0, 1000.0, 100.0, 500.0, 100.0, 500.0),
        ('양송이버섯', 'Button mushroom', 'Agaricus bisporus', 20.0, 25.0, 15.0, 18.0, 85.0, 95.0, 80.0, 90.0, 1500.0, 2500.0, 800.0, 1200.0, 0.0, 50.0, 0.0, 50.0),
        ('새송이버섯', 'King oyster mushroom', 'Pleurotus eryngii', 20.0, 24.0, 14.0, 18.0, 85.0, 90.0, 80.0, 90.0, 1200.0, 2000.0, 800.0, 1200.0, 50.0, 200.0, 100.0, 500.0),
        ('팽이버섯', 'Enoki mushroom', 'Flammulina velutipes', 12.0, 16.0, 7.0, 10.0, 80.0, 90.0, 70.0, 80.0, 2500.0, 3500.0, 2000.0, 3000.0, 50.0, 200.0, 100.0, 500.0),
        ('표고버섯', 'Shiitake', 'Lentinula edodes', 20.0, 25.0, 14.0, 20.0, 65.0, 80.0, 80.0, 90.0, 1000.0, 1500.0, 600.0, 1000.0, 50.0, 200.0, 100.0, 500.0)
), inserted_mushrooms AS (
    INSERT INTO mushroom_reference (mushroom_name_ko, mushroom_name_en, mushroom_scientific_name)
    SELECT name_ko, name_en, scientific_name FROM default_mushrooms
    ON CONFLICT (mushroom_scientific_name) DO NOTHING
    RETURNING id, mushroom_scientific_name
), default_mushroom_references AS (
    SELECT id, mushroom_scientific_name FROM inserted_mushrooms
    UNION ALL
    SELECT mr.id, mr.mushroom_scientific_name
    FROM mushroom_reference mr
    JOIN default_mushrooms dm ON dm.scientific_name = mr.mushroom_scientific_name
)
INSERT INTO mushroom_reference_threshold (sensor_type_id, mushroom_reference_id, threshold_type, threshold_min, threshold_max)
SELECT st.id, mr.id, phase.threshold_type, phase.threshold_min, phase.threshold_max
FROM default_mushrooms dm
JOIN default_mushroom_references mr ON mr.mushroom_scientific_name = dm.scientific_name
CROSS JOIN LATERAL (VALUES
    ('TEMPERATURE', '℃', 'GROWTH', dm.temp_growth_min, dm.temp_growth_max),
    ('TEMPERATURE', '℃', 'HARVEST', dm.temp_harvest_min, dm.temp_harvest_max),
    ('HUMIDITY', '%', 'GROWTH', dm.humidity_growth_min, dm.humidity_growth_max),
    ('HUMIDITY', '%', 'HARVEST', dm.humidity_harvest_min, dm.humidity_harvest_max),
    ('CO2', 'ppm', 'GROWTH', dm.co2_growth_min, dm.co2_growth_max),
    ('CO2', 'ppm', 'HARVEST', dm.co2_harvest_min, dm.co2_harvest_max),
    ('LIGHT', 'lux', 'GROWTH', dm.light_growth_min, dm.light_growth_max),
    ('LIGHT', 'lux', 'HARVEST', dm.light_harvest_min, dm.light_harvest_max)
) AS phase(sensor_type, value_unit, threshold_type, threshold_min, threshold_max)
JOIN sensor_type st ON st.type = phase.sensor_type AND st.value_unit = phase.value_unit
ON CONFLICT (sensor_type_id, mushroom_reference_id, threshold_type) DO NOTHING;

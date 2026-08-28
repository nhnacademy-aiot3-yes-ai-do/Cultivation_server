ALTER TABLE mushroom_reference
    ALTER COLUMN created_at SET DEFAULT now();

ALTER TABLE mushroom_reference_threshold
    ALTER COLUMN created_at SET DEFAULT now();

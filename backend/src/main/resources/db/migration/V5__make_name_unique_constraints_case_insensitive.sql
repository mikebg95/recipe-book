-- Recipe name
ALTER TABLE recipes DROP CONSTRAINT recipes_name_key;
CREATE UNIQUE INDEX uq_recipes_name_ci ON recipes (LOWER(name));

-- Ingredient name (unique within a recipe) — generated column keeps it DEFERRABLE (see V4)
ALTER TABLE ingredients DROP CONSTRAINT uq_name_recipe;
ALTER TABLE ingredients ADD COLUMN name_normalized VARCHAR(100)
    GENERATED ALWAYS AS (LOWER(name)) STORED;
ALTER TABLE ingredients ADD CONSTRAINT uq_name_recipe
    UNIQUE (recipe_id, name_normalized) DEFERRABLE INITIALLY DEFERRED;
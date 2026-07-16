ALTER TABLE steps DROP CONSTRAINT uq_step_number_recipe;
ALTER TABLE steps ADD CONSTRAINT uq_step_number_recipe
    UNIQUE (recipe_id, step_number) DEFERRABLE INITIALLY DEFERRED;

ALTER TABLE ingredients DROP CONSTRAINT uq_name_recipe;
ALTER TABLE ingredients ADD CONSTRAINT uq_name_recipe
    UNIQUE (recipe_id, name) DEFERRABLE INITIALLY DEFERRED ;
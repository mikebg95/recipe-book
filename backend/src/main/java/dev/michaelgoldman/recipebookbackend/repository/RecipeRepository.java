package dev.michaelgoldman.recipebookbackend.repository;

import dev.michaelgoldman.recipebookbackend.entity.Recipe;

public interface RecipeRepository {
    Recipe save(Recipe recipe);
    boolean existsByName(String recipeName);
}

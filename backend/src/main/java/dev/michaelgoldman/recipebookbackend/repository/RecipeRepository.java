package dev.michaelgoldman.recipebookbackend.repository;

import dev.michaelgoldman.recipebookbackend.entity.Recipe;

import java.util.List;

public interface RecipeRepository {
    Recipe save(Recipe recipe);
    boolean existsByName(String recipeName);
    List<Recipe> findAll();
}

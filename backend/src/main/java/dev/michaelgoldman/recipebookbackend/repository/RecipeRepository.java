package dev.michaelgoldman.recipebookbackend.repository;

import dev.michaelgoldman.recipebookbackend.entity.Recipe;

import java.util.List;
import java.util.Optional;

public interface RecipeRepository {
    Recipe save(Recipe recipe);
    boolean existsByName(String recipeName);
    List<Recipe> findAll();
    Optional<Recipe> findById(Long id);
}

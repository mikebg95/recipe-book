package dev.michaelgoldman.recipebookbackend.repository;

import dev.michaelgoldman.recipebookbackend.entity.Recipe;
import dev.michaelgoldman.recipebookbackend.repository.projection.RecipeSummary;

import java.util.List;
import java.util.Optional;

public interface RecipeRepository {
    Recipe save(Recipe recipe);
    boolean existsByName(String recipeName);
    boolean existsByNameExcludingId(String recipeName, Long id);
    List<RecipeSummary> findAll();
    Optional<Recipe> findById(Long id);
    boolean deleteById(Long id);
}

package dev.michaelgoldman.recipebookbackend.repository.projection;

public record RecipeSummary(
        Long id,
        String name,
        String description,
        int ingredientCount,
        int stepCount) {}

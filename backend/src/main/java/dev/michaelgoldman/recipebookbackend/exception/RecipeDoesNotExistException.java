package dev.michaelgoldman.recipebookbackend.exception;

public class RecipeDoesNotExistException extends RuntimeException {
    public RecipeDoesNotExistException(Long recipeId) {
        super("No recipe found with id + " + recipeId);
    }
}

package dev.michaelgoldman.recipebookbackend.exception;

public class RecipeDoesNotExistException extends RuntimeException {
    public RecipeDoesNotExistException(Long recipeId) {
        super("Recipe with id " + recipeId + " does not exist.");
    }
}

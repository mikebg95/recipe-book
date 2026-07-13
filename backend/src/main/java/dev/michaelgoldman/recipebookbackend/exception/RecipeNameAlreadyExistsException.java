package dev.michaelgoldman.recipebookbackend.exception;

public class RecipeNameAlreadyExistsException extends RuntimeException {
    public RecipeNameAlreadyExistsException(String name) {
        super("Recipe with name " + name + " already exists.");
    }
}

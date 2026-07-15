package dev.michaelgoldman.recipebookbackend.service;

import dev.michaelgoldman.recipebookbackend.api.model.RecipeRequest;
import dev.michaelgoldman.recipebookbackend.api.model.RecipeResponse;
import dev.michaelgoldman.recipebookbackend.entity.Recipe;
import dev.michaelgoldman.recipebookbackend.exception.RecipeNameAlreadyExistsException;
import dev.michaelgoldman.recipebookbackend.mapper.RecipeMapper;
import dev.michaelgoldman.recipebookbackend.repository.RecipeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class RecipeService {
    private final RecipeRepository recipeRepository;
    private final RecipeMapper recipeMapper;

    public RecipeService(RecipeRepository recipeRepository, RecipeMapper recipeMapper) {
        this.recipeRepository = recipeRepository;
        this.recipeMapper = recipeMapper;
    }

    @Transactional
    public RecipeResponse createRecipe(RecipeRequest request) {
        if (recipeRepository.existsByName(request.getName())) {
            throw new RecipeNameAlreadyExistsException(request.getName());
        }

        Recipe recipe = recipeMapper.toEntity(request);
        Recipe saved = recipeRepository.save(recipe);
        return recipeMapper.toResponse(saved);
    }

    public List<RecipeResponse> getAll() {
        return recipeMapper.toResponseList(recipeRepository.findAll());
    }
}

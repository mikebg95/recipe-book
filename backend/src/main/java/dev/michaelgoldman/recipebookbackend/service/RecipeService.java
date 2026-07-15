package dev.michaelgoldman.recipebookbackend.service;

import dev.michaelgoldman.recipebookbackend.api.model.RecipeRequest;
import dev.michaelgoldman.recipebookbackend.api.model.RecipeResponse;
import dev.michaelgoldman.recipebookbackend.api.model.RecipeSummaryResponse;
import dev.michaelgoldman.recipebookbackend.entity.Recipe;
import dev.michaelgoldman.recipebookbackend.exception.RecipeDoesNotExistException;
import dev.michaelgoldman.recipebookbackend.exception.RecipeNameAlreadyExistsException;
import dev.michaelgoldman.recipebookbackend.mapper.RecipeMapper;
import dev.michaelgoldman.recipebookbackend.repository.RecipeRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class RecipeService {
    private final RecipeRepository recipeRepository;
    private final RecipeMapper recipeMapper;

    public RecipeService(RecipeRepository recipeRepository, RecipeMapper recipeMapper) {
        this.recipeRepository = recipeRepository;
        this.recipeMapper = recipeMapper;
    }

    // TODO: @Transactional
    public RecipeResponse createRecipe(RecipeRequest request) {
        if (recipeRepository.existsByName(request.getName())) {
            throw new RecipeNameAlreadyExistsException(request.getName());
        }

        Recipe recipe = recipeMapper.toEntity(request);
        Recipe saved = recipeRepository.save(recipe);
        return recipeMapper.toResponse(saved);
    }

    public List<RecipeSummaryResponse> getAll() {
        return recipeMapper.toResponseList(recipeRepository.findAll());
    }

    // TODO: @Transactional
    public RecipeResponse getById(Long id) {
        Optional<Recipe> recipeOptional = recipeRepository.findById(id);
        if (recipeOptional.isEmpty()) {
            throw new RecipeDoesNotExistException(id);
        }
        return recipeMapper.toResponse(recipeOptional.get());
    }

    // TODO: @Transactional
    public void deleteById(Long id) {
        boolean isDeleted = recipeRepository.deleteById(id);
        if (!isDeleted) {
            throw new RecipeDoesNotExistException(id);
        }
    }
}

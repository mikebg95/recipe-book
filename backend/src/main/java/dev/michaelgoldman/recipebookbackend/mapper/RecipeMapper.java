package dev.michaelgoldman.recipebookbackend.mapper;

import dev.michaelgoldman.recipebookbackend.api.model.RecipeRequest;
import dev.michaelgoldman.recipebookbackend.api.model.RecipeResponse;
import dev.michaelgoldman.recipebookbackend.api.model.RecipeSummaryResponse;
import dev.michaelgoldman.recipebookbackend.api.model.StepRequest;
import dev.michaelgoldman.recipebookbackend.api.model.StepResponse;
import dev.michaelgoldman.recipebookbackend.entity.Ingredient;
import dev.michaelgoldman.recipebookbackend.entity.Recipe;
import dev.michaelgoldman.recipebookbackend.entity.Step;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class RecipeMapper {
    public List<RecipeSummaryResponse> toResponseList(List<Recipe> entities) {
        List<RecipeSummaryResponse> responseList = new ArrayList<>();
        for (Recipe entity : entities) {
            responseList.add(toSummaryResponse(entity));
        }
        return responseList;
    }

    public Recipe toEntity(RecipeRequest request) {
        String description = (request.getDescription() == null || request.getDescription().isBlank())
                ? null
                : request.getDescription().strip();
        Recipe recipe = new Recipe(request.getName(), description);

        for (dev.michaelgoldman.recipebookbackend.api.model.Ingredient ingredient : request.getIngredients()) {
            recipe.addIngredient(new Ingredient(ingredient.getName(), ingredient.getUnit(), ingredient.getQuantity()));
        }
        for (StepRequest step : request.getSteps()) {
            recipe.addStep(new Step(step.getDescription()));
        }
        return recipe;
    }

    public RecipeResponse toResponse(Recipe entity) {
       return new RecipeResponse()
                .id(entity.getId())
                .name(entity.getName())
                .description(entity.getDescription())
                .ingredients(entity.getIngredients().stream().map(this::toIngredientResponse).toList())
                .steps(entity.getSteps().stream().map(this::toStepResponse).toList());
    }

    public RecipeSummaryResponse toSummaryResponse(Recipe entity) {
        return new RecipeSummaryResponse()
                .id(entity.getId())
                .name(entity.getName())
                .description(entity.getDescription())
                .numberOfIngredients(entity.getIngredients().size())
                .numberOfSteps(entity.getSteps().size());
    }

    private dev.michaelgoldman.recipebookbackend.api.model.Ingredient toIngredientResponse(Ingredient e) {
        return new dev.michaelgoldman.recipebookbackend.api.model.Ingredient(e.getName(), e.getUnit(), e.getQuantity());
    }

    private StepResponse toStepResponse(Step e) {
        return new StepResponse(e.getDescription(), e.getStepNumber());
    }

}

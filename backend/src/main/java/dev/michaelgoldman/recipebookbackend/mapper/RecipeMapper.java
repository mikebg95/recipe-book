package dev.michaelgoldman.recipebookbackend.mapper;

import dev.michaelgoldman.recipebookbackend.api.model.RecipeRequest;
import dev.michaelgoldman.recipebookbackend.api.model.RecipeResponse;
import dev.michaelgoldman.recipebookbackend.api.model.RecipeSummaryResponse;
import dev.michaelgoldman.recipebookbackend.api.model.StepRequest;
import dev.michaelgoldman.recipebookbackend.api.model.StepResponse;
import dev.michaelgoldman.recipebookbackend.entity.Ingredient;
import dev.michaelgoldman.recipebookbackend.entity.Recipe;
import dev.michaelgoldman.recipebookbackend.entity.Step;
import dev.michaelgoldman.recipebookbackend.repository.projection.RecipeSummary;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class RecipeMapper {
    public List<RecipeSummaryResponse> toResponseSummaryList(List<RecipeSummary> entities) {
        List<RecipeSummaryResponse> responseList = new ArrayList<>();
        for (RecipeSummary entity : entities) {
            responseList.add(toSummaryResponse(entity));
        }
        return responseList;
    }

    public Recipe toEntity(RecipeRequest request) {
        Recipe recipe = new Recipe(request.getName(), request.getDescription());

        for (dev.michaelgoldman.recipebookbackend.api.model.Ingredient ingredient : request.getIngredients()) {
            recipe.addIngredient(new Ingredient(ingredient.getName(), ingredient.getUnit(), ingredient.getQuantity()));
        }
        for (StepRequest step : request.getSteps()) {
            recipe.addStep(new Step(step.getDescription()));
        }
        return recipe;
    }

    public void updateEntity(Recipe recipe, RecipeRequest request) {
        recipe.setName(request.getName());
        recipe.setDescription(request.getDescription());
        recipe.replaceIngredients(toIngredientEntities(request.getIngredients()));
        recipe.replaceSteps(toStepEntities(request.getSteps()));
    }

    private List<Ingredient> toIngredientEntities(List<dev.michaelgoldman.recipebookbackend.api.model.Ingredient> ingredients) {
        List<Ingredient> result = new ArrayList<>();
        for (dev.michaelgoldman.recipebookbackend.api.model.Ingredient ingredient : ingredients) {
            result.add(new Ingredient(ingredient.getName(), ingredient.getUnit(), ingredient.getQuantity()));
        }
        return result;
    }

    private List<Step> toStepEntities(List<StepRequest> steps) {
        List<Step> result = new ArrayList<>();
        for (dev.michaelgoldman.recipebookbackend.api.model.StepRequest step : steps) {
            result.add(new Step(step.getDescription()));
        }
        return result;
    }

    public RecipeResponse toResponse(Recipe entity) {
       return new RecipeResponse()
                .id(entity.getId())
                .name(entity.getName())
                .description(entity.getDescription())
                .ingredients(entity.getIngredients().stream().map(this::toIngredientResponse).toList())
                .steps(entity.getSteps().stream().map(this::toStepResponse).toList());
    }

    public RecipeSummaryResponse toSummaryResponse(RecipeSummary entity) {
        return new RecipeSummaryResponse()
                .id(entity.id())
                .name(entity.name())
                .description(entity.description())
                .numberOfIngredients(entity.ingredientCount())
                .numberOfSteps(entity.stepCount());
    }

    private dev.michaelgoldman.recipebookbackend.api.model.Ingredient toIngredientResponse(Ingredient e) {
        return new dev.michaelgoldman.recipebookbackend.api.model.Ingredient(e.getName(), e.getUnit(), e.getQuantity());
    }

    private StepResponse toStepResponse(Step e) {
        return new StepResponse(e.getDescription(), e.getStepNumber());
    }

}

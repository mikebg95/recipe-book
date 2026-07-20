package dev.michaelgoldman.recipebookbackend.api.model;

import java.util.List;

import static dev.michaelgoldman.recipebookbackend.api.model.IngredientTestBuilder.anIngredientDto;

public class RecipeRequestTestBuilder {
    private String name = "Valid Recipe Name";
    private String description = "A valid recipe description.";
    private List<Ingredient> ingredients = List.of(anIngredientDto().build());
    private List<StepRequest> steps = List.of(new StepRequest("Grind the salt until it looks like powder."));

    public static RecipeRequestTestBuilder aRecipeRequest() {
        return new RecipeRequestTestBuilder();
    }

    public RecipeRequestTestBuilder withName(String name) {
        this.name = name;
        return this;
    }

    public RecipeRequestTestBuilder withDescription(String description) {
        this.description = description;
        return this;
    }

    public RecipeRequestTestBuilder withIngredients(Ingredient... ingredients) {
        this.ingredients = List.of(ingredients);
        return this;
    }

    public RecipeRequestTestBuilder withStepDescriptions(List<String> descriptions) {
        this.steps = descriptions.stream().map(StepRequest::new).toList();
        return this;
    }

    public RecipeRequest build() {
        return new RecipeRequest()
                .name(name)
                .description(description)
                .ingredients(ingredients)
                .steps(steps);
    }
}

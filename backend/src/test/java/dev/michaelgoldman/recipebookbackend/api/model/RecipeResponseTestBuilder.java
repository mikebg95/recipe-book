package dev.michaelgoldman.recipebookbackend.api.model;

import java.util.List;
import java.util.stream.IntStream;

import static dev.michaelgoldman.recipebookbackend.api.model.IngredientTestBuilder.anIngredientDto;
import static dev.michaelgoldman.recipebookbackend.api.model.StepResponseTestBuilder.aStepResponse;

public class RecipeResponseTestBuilder {
    private Long id = 1L;
    private String name = "Valid Recipe Name";
    private String description = "A valid recipe description.";
    private List<Ingredient> ingredients = List.of(anIngredientDto().build());
    private List<StepResponse> steps = List.of(aStepResponse().build());

    public static RecipeResponseTestBuilder aRecipeResponse() {
        return new RecipeResponseTestBuilder();
    }

    public RecipeResponseTestBuilder withId(Long id) {
        this.id = id;
        return this;
    }

    public RecipeResponseTestBuilder withName(String name) {
        this.name = name;
        return this;
    }

    public RecipeResponseTestBuilder withDescription(String description) {
        this.description = description;
        return this;
    }

    public RecipeResponseTestBuilder withIngredients(Ingredient... ingredients) {
        this.ingredients = List.of(ingredients);
        return this;
    }

    public RecipeResponseTestBuilder withStepDescriptions(List<String> descriptions) {
        this.steps = IntStream.range(0, descriptions.size())
                .mapToObj(i -> new StepResponse(descriptions.get(i), i + 1))
                .toList();
        return this;
    }

    public RecipeResponse build() {
        return new RecipeResponse()
                .id(id)
                .name(name)
                .description(description)
                .ingredients(ingredients)
                .steps(steps);
    }
}

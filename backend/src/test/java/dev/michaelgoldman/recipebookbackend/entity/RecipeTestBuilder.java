package dev.michaelgoldman.recipebookbackend.entity;

import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;

public class RecipeTestBuilder {
    private Long id;
    private String name = "Valid recipe name";
    private String description = "A valid recipe description.";
    private List<Ingredient> ingredients = List.of(
            new Ingredient("Salt", "grams", new BigDecimal("1")));
    private List<Step> steps = List.of(
            new Step("A valid step."));

    public static RecipeTestBuilder aRecipe() {
        return new RecipeTestBuilder();
    }

    public RecipeTestBuilder withId(Long id) {
        this.id = id;
        return this;
    }

    public RecipeTestBuilder withName(String name) {
        this.name = name;
        return this;
    }

    public RecipeTestBuilder withDescription(String description) {
        this.description = description;
        return this;
    }

    public RecipeTestBuilder withIngredients(Ingredient... ingredients) {
        this.ingredients = List.of(ingredients);
        return this;
    }

    public RecipeTestBuilder withStepDescriptions(List<String> descriptions) {
        this.steps = descriptions.stream().map(Step::new).toList();
        return this;
    }

    public Recipe build() {
        Recipe recipe = new Recipe(name, description);
        ingredients.forEach(recipe::addIngredient);
        steps.forEach(recipe::addStep);
        if (id != null) {
            ReflectionTestUtils.setField(recipe, "id", id);
        }
        return recipe;
    }
}

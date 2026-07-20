package dev.michaelgoldman.recipebookbackend.entity;

import dev.michaelgoldman.recipebookbackend.repository.projection.RecipeSummary;

public class RecipeSummaryTestBuilder {
    private Long id = 1L;
    private String name = "Valid Recipe Name";
    private String description = "A valid recipe description.";
    private int ingredientCount = 3;
    private int stepCount = 4;

    public static RecipeSummaryTestBuilder aRecipeSummary() {
        return new RecipeSummaryTestBuilder();
    }

    public RecipeSummaryTestBuilder withId(Long id) {
        this.id = id;
        return this;
    }

    public RecipeSummaryTestBuilder withName(String name) {
        this.name = name;
        return this;
    }

    public RecipeSummaryTestBuilder withDescription(String description) {
        this.description = description;
        return this;
    }

    public RecipeSummaryTestBuilder withIngredientCount(int ingredientCount) {
        this.ingredientCount = ingredientCount;
        return this;
    }

    public RecipeSummaryTestBuilder withStepCount(int stepCount) {
        this.stepCount = stepCount;
        return this;
    }

    public RecipeSummary build() {
        return new RecipeSummary(id, name, description, ingredientCount, stepCount);
    }
}

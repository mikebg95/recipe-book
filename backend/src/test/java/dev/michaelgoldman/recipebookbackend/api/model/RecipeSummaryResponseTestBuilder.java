package dev.michaelgoldman.recipebookbackend.api.model;

public class RecipeSummaryResponseTestBuilder {
    private Long id = 1L;
    private String name = "Valid recipe name";
    private String description = "A valid recipe description.";
    private Integer numberOfIngredients = 1;
    private Integer numberOfSteps = 1;

    public static RecipeSummaryResponseTestBuilder aRecipeSummaryResponse() {
        return new RecipeSummaryResponseTestBuilder();
    }

    public RecipeSummaryResponseTestBuilder withId(Long id) {
        this.id = id;
        return this;
    }

    public RecipeSummaryResponseTestBuilder withName(String name) {
        this.name = name;
        return this;
    }

    public RecipeSummaryResponseTestBuilder withDescription(String description) {
        this.description = description;
        return this;
    }

    public RecipeSummaryResponseTestBuilder withNumberOfIngredients(Integer numberOfIngredients) {
        this.numberOfIngredients = numberOfIngredients;
        return this;
    }

    public RecipeSummaryResponseTestBuilder withNumberOfSteps(Integer numberOfSteps) {
        this.numberOfSteps = numberOfSteps;
        return this;
    }

    public RecipeSummaryResponse build() {
        return new RecipeSummaryResponse()
                .id(id)
                .name(name)
                .description(description)
                .numberOfIngredients(numberOfIngredients)
                .numberOfSteps(numberOfSteps);
    }
}

package dev.michaelgoldman.recipebookbackend.api.model;

public class StepResponseTestBuilder {
    private String description = "Grind the salt until it looks like powder.";
    private Integer stepNumber = 1;

    public static StepResponseTestBuilder aStepResponse() {
        return new StepResponseTestBuilder();
    }

    public StepResponseTestBuilder withDescription(String description) {
        this.description = description;
        return this;
    }

    public StepResponseTestBuilder withStepNumber(Integer stepNumber) {
        this.stepNumber = stepNumber;
        return this;
    }

    public StepResponse build() {
        return new StepResponse().description(description).stepNumber(stepNumber);
    }
}

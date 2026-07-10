package dev.michaelgoldman.recipebookbackend.api.model;

import java.math.BigDecimal;

public class IngredientTestBuilder {
    private String name = "Salt";
    private String unit = "grams";
    private BigDecimal quantity = new BigDecimal("1");

    public static IngredientTestBuilder anIngredient() {
        return new IngredientTestBuilder();
    }

    public IngredientTestBuilder withName(String name) {
        this.name = name;
        return this;
    }

    public IngredientTestBuilder withUnit(String unit) {
        this.unit = unit;
        return this;
    }

    public IngredientTestBuilder withQuantity(BigDecimal quantity) {
        this.quantity = quantity;
        return this;
    }

    public Ingredient build() {
        return new Ingredient().name(name).unit(unit).quantity(quantity);
    }
}

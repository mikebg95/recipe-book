package dev.michaelgoldman.recipebookbackend.fixtures;

import dev.michaelgoldman.recipebookbackend.api.model.Ingredient;
import dev.michaelgoldman.recipebookbackend.api.model.RecipeRequestTestBuilder;
import dev.michaelgoldman.recipebookbackend.api.model.RecipeResponseTestBuilder;
import dev.michaelgoldman.recipebookbackend.entity.RecipeTestBuilder;

import java.math.BigDecimal;
import java.util.List;

import static dev.michaelgoldman.recipebookbackend.api.model.RecipeRequestTestBuilder.aRecipeRequest;
import static dev.michaelgoldman.recipebookbackend.api.model.RecipeResponseTestBuilder.aRecipeResponse;
import static dev.michaelgoldman.recipebookbackend.entity.RecipeTestBuilder.aRecipe;

public final class RecipeFixtures {
    private RecipeFixtures() {}

    public static final Long NON_EXISTING_ID = 999L;

    public static final List<String> CARBONARA_STEPS_DESCRIPTIONS = List.of(
            "Crisp the diced guanciale in a pan over medium heat; set the rendered fat and meat aside.",
            "Whisk egg yolks, Pecorino Romano, and plenty of cracked black pepper in a bowl to form a thick paste.",
            "Boil spaghetti in salted water until al dente, drain (reserve 1 cup of pasta water), and immediately toss the hot pasta into the guanciale pan.",
            "Remove pan from heat, stir in the egg-cheese paste and a splash of pasta water, tossing vigorously until a smooth, creamy sauce forms.",
            "Enjoy the beautiful tasty dish!"
    );

    private static final List<String> CACIO_E_PEPE_STEPS_DESCRIPTIONS = List.of(
            "Toast the freshly cracked black pepper in a dry pan over medium heat until fragrant.",
            "Boil the tonnarelli or spaghetti in salted water until very al dente; transfer directly to the pan with the pepper, reserving the pasta water.",
            "Add a ladle of hot pasta water to the pan and toss to create a starchy base, then remove the pan from the heat.",
            "Sprinkle in the Pecorino Romano while stirring and tossing vigorously, adding a bit more pasta water as needed to create a creamy sauce."
    );

    private static final String CARBONARA_NAME = "Spaghetti alla Carbonara";
    private static final String CARBONARA_DESCRIPTION = "An Italian classic.";
    private static final List<IngredientSpec> CARBONARA_INGREDIENTS = List.of(
            new IngredientSpec("Guanciale", "grams", "150"),
            new IngredientSpec("Pecorino Romano", "grams", "100"),
            new IngredientSpec("Spaghetti", "grams", "350"),
            new IngredientSpec("Large egg", "yolk", "6"),
            new IngredientSpec("Black pepper", "to taste", "1"));

    private static final String CACIO_E_PEPE_NAME = "Cacio e Pepe";
    private static final String CACIO_E_PEPE_DESCRIPTION = "A minimalist Roman masterpiece.";
    private static final List<IngredientSpec> CACIO_E_PEPE_INGREDIENTS = List.of(
            new IngredientSpec("Tonnarelli or Spaghetti", "grams", "350"),
            new IngredientSpec("Pecorino Romano", "grams", "120"),
            new IngredientSpec("Black pepper", "tablespoons", "1.5"),
            new IngredientSpec("Salt", "to taste", "1"));

    public static RecipeRequestTestBuilder aCarbonaraRequest() {
        return aRecipeRequest()
                .withName(CARBONARA_NAME)
                .withDescription(CARBONARA_DESCRIPTION)
                .withIngredients(toDtos(CARBONARA_INGREDIENTS))
                .withStepDescriptions(CARBONARA_STEPS_DESCRIPTIONS);
    }

    public static RecipeResponseTestBuilder aCarbonaraResponse() {
        return aRecipeResponse()
                .withName(CARBONARA_NAME)
                .withDescription(CARBONARA_DESCRIPTION)
                .withIngredients(toDtos(CARBONARA_INGREDIENTS))
                .withStepDescriptions(CARBONARA_STEPS_DESCRIPTIONS);
    }

    public static RecipeTestBuilder aCarbonara() {
        return aRecipe()
                .withName(CARBONARA_NAME)
                .withDescription(CARBONARA_DESCRIPTION)
                .withIngredients(toEntities(CARBONARA_INGREDIENTS))
                .withStepDescriptions(CARBONARA_STEPS_DESCRIPTIONS);
    }

    public static RecipeTestBuilder aCacioEPepe() {
        return aRecipe()
                .withName(CACIO_E_PEPE_NAME)
                .withDescription(CACIO_E_PEPE_DESCRIPTION)
                .withIngredients(toEntities(CACIO_E_PEPE_INGREDIENTS))
                .withStepDescriptions(CACIO_E_PEPE_STEPS_DESCRIPTIONS);
    }

    private static Ingredient[] toDtos(List<IngredientSpec> specs) {
        return specs.stream()
                .map(s -> new Ingredient(s.name(), s.unit(), new BigDecimal(s.quantity())))
                .toArray(Ingredient[]::new);
    }

    private static dev.michaelgoldman.recipebookbackend.entity.Ingredient[] toEntities(List<IngredientSpec> specs) {
        return specs.stream()
                .map(s -> new dev.michaelgoldman.recipebookbackend.entity.Ingredient(s.name(), s.unit(), new BigDecimal(s.quantity())))
                .toArray(dev.michaelgoldman.recipebookbackend.entity.Ingredient[]::new);
    }

    private record IngredientSpec(String name, String unit, String quantity) {}
}

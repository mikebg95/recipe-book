package dev.michaelgoldman.recipebookbackend.mapper;

import dev.michaelgoldman.recipebookbackend.api.model.Ingredient;
import dev.michaelgoldman.recipebookbackend.api.model.RecipeRequest;
import dev.michaelgoldman.recipebookbackend.api.model.RecipeResponse;
import dev.michaelgoldman.recipebookbackend.api.model.StepResponse;
import dev.michaelgoldman.recipebookbackend.entity.Recipe;
import dev.michaelgoldman.recipebookbackend.entity.Step;
import org.assertj.core.groups.Tuple;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static dev.michaelgoldman.recipebookbackend.api.model.RecipeRequestTestBuilder.aRecipeRequest;
import static dev.michaelgoldman.recipebookbackend.entity.RecipeTestBuilder.aRecipe;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

class RecipeMapperTest {
    private static final List<String> CARBONARA_STEPS_DESCRIPTIONS = List.of(
            "Crisp the diced guanciale in a pan over medium heat; set the rendered fat and meat aside.",
            "Whisk egg yolks, Pecorino Romano, and plenty of cracked black pepper in a bowl to form a thick paste.",
            "Boil spaghetti in salted water until al dente, drain (reserve 1 cup of pasta water), and immediately toss the hot pasta into the guanciale pan.",
            "Remove pan from heat, stir in the egg-cheese paste and a splash of pasta water, tossing vigorously until a smooth, creamy sauce forms."
    );

    private final RecipeMapper recipeMapper = new RecipeMapper();

    @Nested
    @DisplayName("toEntity")
    class ToEntity {
        @Test
        void whenFullyPopulatedRequest_shouldMapNameDescriptionIngredientsAndSteps() {
            // Arrange
            RecipeRequest request = fullyPopulatedRequest();

            // Act
            Recipe entity = recipeMapper.toEntity(request);

            // Assert
            assertThat(entity.getName()).isEqualTo(request.getName());
            assertThat(entity.getDescription()).isEqualTo(request.getDescription());
            assertThat(entity.getIngredients())
                    .extracting(
                            dev.michaelgoldman.recipebookbackend.entity.Ingredient::getName,
                            dev.michaelgoldman.recipebookbackend.entity.Ingredient::getUnit,
                            dev.michaelgoldman.recipebookbackend.entity.Ingredient::getQuantity)
                    .containsExactly(
                            request.getIngredients().stream()
                                    .map(i -> tuple(i.getName(), i.getUnit(), i.getQuantity()))
                                    .toArray(Tuple[]::new));

            assertThat(entity.getSteps())
                    .usingRecursiveComparison()
                    .comparingOnlyFields("description")
                    .isEqualTo(request.getSteps());

            assertThat(entity.getSteps())
                    .extracting(Step::getStepNumber)
                    .containsExactly(1, 2, 3, 4);
        }

        @Test
        void whenFullyPopulatedRequest_shouldSetParentBackReferenceOnIngredientsAndSteps() {
            // Arrange
            RecipeRequest request = fullyPopulatedRequest();

            // Act
            Recipe entity = recipeMapper.toEntity(request);

            // Assert
            assertThat(entity.getIngredients())
                    .allSatisfy(i -> assertThat(i.getRecipe()).isSameAs(entity));

            assertThat(entity.getSteps())
                    .allSatisfy(s -> assertThat(s.getRecipe()).isSameAs(entity));
        }

        @Test
        void whenDescriptionNull_shouldMapDescriptionToNull() {
            // Arrange
            RecipeRequest request = fullyPopulatedRequest();
            request.setDescription(null);

            // Act
            Recipe entity = recipeMapper.toEntity(request);

            // Assert
            assertThat(entity.getDescription()).isNull();
        }
    }

    @Nested
    @DisplayName("toResponse")
    class ToResponse {
        @Test
        void whenFullyPopulatedEntity_shouldMapNameDescriptionIngredientsAndSteps() {
            // Arrange
            Recipe entity = fullyPopulatedEntity();

            // Act
            RecipeResponse response = recipeMapper.toResponse(entity);

            // Assert
            assertThat(response.getId()).isEqualTo(entity.getId());
            assertThat(response.getName()).isEqualTo(entity.getName());
            assertThat(response.getDescription()).isEqualTo(entity.getDescription());
            assertThat(response.getIngredients())
                    .extracting(Ingredient::getName, Ingredient::getUnit, Ingredient::getQuantity)
                    .containsExactly(
                            entity.getIngredients().stream()
                                    .map(i -> tuple(i.getName(), i.getUnit(), i.getQuantity()))
                                    .toArray(Tuple[]::new));

            assertThat(response.getSteps())
                    .usingRecursiveComparison()
                    .comparingOnlyFields("description")
                    .isEqualTo(entity.getSteps());

            assertThat(response.getSteps())
                    .extracting(StepResponse::getStepNumber)
                    .containsExactly(1, 2, 3, 4);
        }

        @Test
        void whenDescriptionNull_shouldMapDescriptionToNull() {
            // Arrange
            Recipe entity = fullyPopulatedEntity();
            entity.setDescription(null);

            // Act
            RecipeResponse response = recipeMapper.toResponse(entity);

            // Assert
            assertThat(response.getDescription()).isNull();
        }
    }

    private RecipeRequest fullyPopulatedRequest() {
        return aRecipeRequest()
                .withName("Spaghetti alla Carbonara")
                .withDescription("An Italian classic.")
                .withIngredients(
                        new Ingredient("Guanciale", "grams", new BigDecimal("150")),
                        new Ingredient("Pecorino Romano", "grams", new BigDecimal("100")),
                        new Ingredient("Spaghetti", "grams", new BigDecimal("350")),
                        new Ingredient("Large egg", "yolk", new BigDecimal("6")),
                        new Ingredient("Black pepper", "to taste", new BigDecimal("1")))
                .withStepDescriptions(CARBONARA_STEPS_DESCRIPTIONS)
                .build();
    }

    private Recipe fullyPopulatedEntity() {
        return aRecipe()
                .withId(1L)
                .withName("Spaghetti alla Carbonara")
                .withDescription("An Italian classic.")
                .withIngredients(
                        new dev.michaelgoldman.recipebookbackend.entity.Ingredient("Guanciale", "grams", new BigDecimal("150")),
                        new dev.michaelgoldman.recipebookbackend.entity.Ingredient("Pecorino Romano", "grams", new BigDecimal("100")),
                        new dev.michaelgoldman.recipebookbackend.entity.Ingredient("Spaghetti", "grams", new BigDecimal("350")),
                        new dev.michaelgoldman.recipebookbackend.entity.Ingredient("Large egg", "yolk", new BigDecimal("6")),
                        new dev.michaelgoldman.recipebookbackend.entity.Ingredient("Black pepper", "to taste", new BigDecimal("1")))
                .withStepDescriptions(CARBONARA_STEPS_DESCRIPTIONS)
                .build();
    }
}

package dev.michaelgoldman.recipebookbackend.mapper;

import dev.michaelgoldman.recipebookbackend.api.model.Ingredient;
import dev.michaelgoldman.recipebookbackend.api.model.RecipeRequest;
import dev.michaelgoldman.recipebookbackend.api.model.RecipeRequestTestBuilder;
import dev.michaelgoldman.recipebookbackend.api.model.RecipeResponse;
import dev.michaelgoldman.recipebookbackend.api.model.RecipeSummaryResponse;
import dev.michaelgoldman.recipebookbackend.api.model.StepRequest;
import dev.michaelgoldman.recipebookbackend.api.model.StepResponse;
import dev.michaelgoldman.recipebookbackend.entity.Recipe;
import dev.michaelgoldman.recipebookbackend.entity.Step;
import dev.michaelgoldman.recipebookbackend.repository.projection.RecipeSummary;
import org.assertj.core.groups.Tuple;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static dev.michaelgoldman.recipebookbackend.api.model.RecipeRequestTestBuilder.aRecipeRequest;
import static dev.michaelgoldman.recipebookbackend.entity.RecipeSummaryTestBuilder.aRecipeSummary;
import static dev.michaelgoldman.recipebookbackend.entity.RecipeTestBuilder.aRecipe;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

class RecipeMapperTest {
    private static final List<String> CARBONARA_STEPS_DESCRIPTIONS = List.of(
            "Crisp the diced guanciale in a pan over medium heat; set the rendered fat and meat aside.",
            "Whisk egg yolks, Pecorino Romano, and plenty of cracked black pepper in a bowl to form a thick paste.",
            "Boil spaghetti in salted water until al dente, drain (reserve 1 cup of pasta water), and immediately toss the hot pasta into the guanciale pan.",
            "Remove pan from heat, stir in the egg-cheese paste and a splash of pasta water, tossing vigorously until a smooth, creamy sauce forms.",
            "Enjoy the beautiful tasty dish!."
    );

    private static final List<String> CACIO_E_PEPE_STEPS_DESCRIPTIONS = List.of(
            "Toast the freshly cracked black pepper in a dry pan over medium heat until fragrant.",
            "Boil the tonnarelli or spaghetti in salted water until very al dente; transfer directly to the pan with the pepper, reserving the pasta water.",
            "Add a ladle of hot pasta water to the pan and toss to create a starchy base, then remove the pan from the heat.",
            "Sprinkle in the Pecorino Romano while stirring and tossing vigorously, adding a bit more pasta water as needed to create a creamy sauce."
    );

    private final RecipeMapper recipeMapper = new RecipeMapper();

    @Nested
    @DisplayName("toEntity")
    class ToEntity {
        @Test
        void whenFullyPopulatedRequest_shouldMapNameDescriptionIngredientsAndSteps() {
            // Arrange
            RecipeRequest request = carbonaraRequest().build();

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
                    .containsExactly(1, 2, 3, 4, 5);
        }

        @Test
        void whenFullyPopulatedRequest_shouldSetParentBackReferenceOnIngredientsAndSteps() {
            // Arrange
            RecipeRequest request = carbonaraRequest().build();

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
            RecipeRequest request = carbonaraRequest().build();
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
            Recipe entity = cacioEPepeEntity();

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
            Recipe entity = cacioEPepeEntity();
            entity.setDescription(null);

            // Act
            RecipeResponse response = recipeMapper.toResponse(entity);

            // Assert
            assertThat(response.getDescription()).isNull();
        }
    }

    @Nested
    @DisplayName("toResponseSummaryList")
    class ToResponseSummaryList {
        @Test
        void whenEntityList_shouldMapToListOfResponseSummaries() {
            // Arrange
            List<RecipeSummary> entities = List.of(
                    aRecipeSummary().withId(1L).withName("Steak").build(),
                    aRecipeSummary().withId(2L).withName("Pizza").build(),
                    aRecipeSummary().withId(3L).withName("Pasta").build()
            );

            // Act
            List<RecipeSummaryResponse> responses = recipeMapper.toResponseSummaryList(entities);



            // Assert
            assertThat(responses)
                    .extracting(RecipeSummaryResponse::getId)
                    .containsExactly(1L, 2L, 3L);
            assertThat(responses)
                    .extracting(RecipeSummaryResponse::getName)
                    .containsExactly("Steak", "Pizza", "Pasta");
            assertThat(responses)
                    .extracting(RecipeSummaryResponse::getNumberOfIngredients)
                    .containsExactly(3, 3, 3);
            assertThat(responses)
                    .extracting(RecipeSummaryResponse::getNumberOfSteps)
                    .containsExactly(4, 4, 4);
        }

        @Test
        void whenEmptyList_shouldMapToEmptyList() {
            // Arrange
            List<RecipeSummary> emptyList = new ArrayList<>();

            // Act
            List<RecipeSummaryResponse> result = recipeMapper.toResponseSummaryList(emptyList);

            // Assert
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("UpdateEntity")
    class UpdateEntity {
        @Test
        void updateEntity_whenPopulatedEntityAndRequestPassed_shouldUpdateEntityValues() {
            // Arrange
            Recipe entity = cacioEPepeEntity();
            RecipeRequest request = carbonaraRequest().build();
            Long entityId = entity.getId();

            // Act
            recipeMapper.updateEntity(entity, request);

            // Assert
            assertThat(entity.getId()).isEqualTo(entityId);
            assertThat(entity.getName()).isEqualTo(request.getName());
            assertThat(entity.getDescription()).isEqualTo(request.getDescription());

            // ingredient names
            List<String> expectedIngredientNames = request.getIngredients()
                    .stream()
                    .map(Ingredient::getName)
                    .toList();
            assertThat(entity.getIngredients())
                    .extracting(dev.michaelgoldman.recipebookbackend.entity.Ingredient::getName)
                    .containsExactlyInAnyOrderElementsOf(expectedIngredientNames);

            // ingredient units
            List<String> expectedIngredientUnits = request.getIngredients()
                    .stream()
                    .map(Ingredient::getUnit)
                    .toList();
            assertThat(entity.getIngredients())
                    .extracting(dev.michaelgoldman.recipebookbackend.entity.Ingredient::getUnit)
                    .containsExactlyInAnyOrderElementsOf(expectedIngredientUnits);

            // ingredient quantities
            List<BigDecimal> expectedIngredientQuantities = request.getIngredients()
                    .stream()
                    .map(Ingredient::getQuantity)
                    .toList();
            assertThat(entity.getIngredients())
                    .extracting(dev.michaelgoldman.recipebookbackend.entity.Ingredient::getQuantity)
                    .usingElementComparator(BigDecimal::compareTo)
                    .containsExactlyInAnyOrderElementsOf(expectedIngredientQuantities);

            // steps
            List<String> expectedStepDescriptions = request.getSteps()
                    .stream()
                    .map(StepRequest::getDescription)
                    .toList();
            assertThat(entity.getSteps())
                    .extracting(dev.michaelgoldman.recipebookbackend.entity.Step::getDescription)
                    .containsExactlyElementsOf(expectedStepDescriptions);
            assertThat(entity.getSteps())
                    .extracting(dev.michaelgoldman.recipebookbackend.entity.Step::getStepNumber)
                    .containsExactly(1, 2, 3, 4, 5);

            // back references
            assertThat(entity.getIngredients()).allMatch(i -> i.getRecipe() == entity);
            assertThat(entity.getSteps()).allMatch(s -> s.getRecipe() == entity);
        }

        @Test
        void updateEntity_whenDescriptionNullInRequest_shouldUpdateEntityCorrectly() {
            // Arrange
            Recipe entity = cacioEPepeEntity();
            RecipeRequest request = carbonaraRequest().withDescription(null).build();

            // Act
            recipeMapper.updateEntity(entity, request);

            // Assert
            assertThat(entity.getDescription()).isNull();
        }
    }

    private RecipeRequestTestBuilder carbonaraRequest() {
        return aRecipeRequest()
                .withName("Spaghetti alla Carbonara")
                .withDescription("An Italian classic.")
                .withIngredients(
                        new Ingredient("Guanciale", "grams", new BigDecimal("150")),
                        new Ingredient("Pecorino Romano", "grams", new BigDecimal("100")),
                        new Ingredient("Spaghetti", "grams", new BigDecimal("350")),
                        new Ingredient("Large egg", "yolk", new BigDecimal("6")),
                        new Ingredient("Black pepper", "to taste", new BigDecimal("1")))
                .withStepDescriptions(CARBONARA_STEPS_DESCRIPTIONS);
    }

    private Recipe cacioEPepeEntity() {
        return aRecipe()
                .withId(5L)
                .withName("Cacio e Pepe")
                .withDescription("A minimalist Roman masterpiece.")
                .withIngredients(
                        new dev.michaelgoldman.recipebookbackend.entity.Ingredient("Tonnarelli or Spaghetti", "grams", new BigDecimal("350")),
                        new dev.michaelgoldman.recipebookbackend.entity.Ingredient("Pecorino Romano", "grams", new BigDecimal("120")),
                        new dev.michaelgoldman.recipebookbackend.entity.Ingredient("Black pepper", "tablespoons", new BigDecimal("1.5")),
                        new dev.michaelgoldman.recipebookbackend.entity.Ingredient("Salt", "to taste", new BigDecimal("1")))
                .withStepDescriptions(CACIO_E_PEPE_STEPS_DESCRIPTIONS)
                .build();
    }
}

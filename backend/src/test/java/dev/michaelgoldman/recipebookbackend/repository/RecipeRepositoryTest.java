package dev.michaelgoldman.recipebookbackend.repository;

import dev.michaelgoldman.recipebookbackend.TestcontainersConfiguration;
import dev.michaelgoldman.recipebookbackend.entity.Ingredient;
import dev.michaelgoldman.recipebookbackend.entity.Recipe;
import dev.michaelgoldman.recipebookbackend.entity.RecipeTestBuilder;
import dev.michaelgoldman.recipebookbackend.entity.Step;
import jakarta.persistence.EntityExistsException;
import org.hibernate.exception.DataException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static dev.michaelgoldman.recipebookbackend.entity.IngredientTestBuilder.anIngredient;
import static dev.michaelgoldman.recipebookbackend.entity.RecipeTestBuilder.aRecipe;
import static org.assertj.core.api.Assertions.as;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.assertj.core.api.InstanceOfAssertFactories.BIG_DECIMAL;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase.Replace.NONE;

@DataJpaTest
@AutoConfigureTestDatabase(replace = NONE)
@Import({TestcontainersConfiguration.class, RecipeRepositoryImpl.class})
class RecipeRepositoryTest {
    private static final List<String> CARBONARA_STEPS_DESCRIPTIONS = List.of(
            "Crisp the diced guanciale in a pan over medium heat; set the rendered fat and meat aside.",
            "Whisk egg yolks, Pecorino Romano, and plenty of cracked black pepper in a bowl to form a thick paste.",
            "Boil spaghetti in salted water until al dente, drain (reserve 1 cup of pasta water), and immediately toss the hot pasta into the guanciale pan.",
            "Remove pan from heat, stir in the egg-cheese paste and a splash of pasta water, tossing vigorously until a smooth, creamy sauce forms."
    );

    private static final List<String> CACIO_E_PEPE_STEPS_DESCRIPTIONS = List.of(
            "Toast the freshly cracked black pepper in a dry pan over medium heat until fragrant.",
            "Boil the tonnarelli or spaghetti in salted water until very al dente; transfer directly to the pan with the pepper, reserving the pasta water.",
            "Add a ladle of hot pasta water to the pan and toss to create a starchy base, then remove the pan from the heat.",
            "Sprinkle in the Pecorino Romano while stirring and tossing vigorously, adding a bit more pasta water as needed to create a creamy sauce."
    );

    @Autowired
    private RecipeRepository recipeRepository;

    @Autowired
    private TestEntityManager testEntityManager;

    @Nested
    @DisplayName("SaveRecipe")
    class SaveRecipe {
        @Test
        void whenValidRecipeDetailsProvided_shouldReturnPersistedRecipeWithGeneratedId() {
            // Arrange
            Recipe carbonara = aCarbonara().build();

            // Act
            Recipe fetched = saveAndReload(carbonara);

            // Assert
            assertThat(fetched).isNotNull();
            assertThat(fetched.getId()).isNotNull();
            assertThat(fetched.getVersion()).isNotNull();
            assertThat(fetched.getName()).isEqualTo(carbonara.getName());
            assertThat(fetched.getDescription()).isEqualTo(carbonara.getDescription());
        }

        @Test
        void whenValidRecipeDetailsProvided_shouldReturnPersistedRecipeWithAllIngredients() {
            // Arrange
            Recipe carbonara = aCarbonara().build();

            // Act
            Recipe fetched = saveAndReload(carbonara);

            // Assert
            assertThat(fetched).isNotNull();
            assertThat(fetched.getIngredients())
                    .extracting(Ingredient::getName)
                    .containsExactlyInAnyOrder("Guanciale", "Pecorino Romano", "Spaghetti", "Large egg", "Black pepper");
        }

        @Test
        void whenValidRecipeDetailsProvided_shouldReturnAllStepsInCorrectOrder() {
            // Arrange
            Recipe carbonara = aCarbonara().build();

            // Act
            Recipe fetched = saveAndReload(carbonara);

            // Assert
            assertThat(fetched).isNotNull();
            assertThat(fetched.getSteps())
                    .extracting(Step::getDescription)
                    .containsExactlyElementsOf(CARBONARA_STEPS_DESCRIPTIONS);
            assertThat(fetched.getSteps())
                    .extracting(Step::getStepNumber)
                    .containsExactly(1, 2, 3, 4);
        }

        @Test
        void whenRecipeDescriptionIsNull_shouldPersistSuccessfully() {
            // Arrange
            Recipe recipe = aRecipe().withDescription(null).build();

            // Act
            Recipe fetched = saveAndReload(recipe);

            // Assert
            assertThat(fetched).isNotNull();
            assertThat(fetched.getDescription()).isNull();
        }

        @Test
        void whenIngredientQuantityLessThanOne_shouldPersistSuccessfully() {
            // Arrange
            BigDecimal quantity = new BigDecimal("0.1");
            Recipe recipe = aRecipe().withIngredients(anIngredient().withQuantity(quantity).build()).build();

            // Act
            Recipe fetched = saveAndReload(recipe);

            // Assert
            assertThat(fetched).isNotNull();
            assertThat(fetched.getIngredients())
                    .singleElement()
                    .extracting(Ingredient::getQuantity, as(BIG_DECIMAL))
                    .isEqualByComparingTo(quantity);
        }

        @Test
        void whenIngredientQuantityTooManyDecimals_shouldPersistSuccessfullyAndRoundToThreeDecimals() {
            // Arrange
            Recipe recipe = aRecipe().withIngredients(anIngredient().withQuantity(new BigDecimal("123.1234")).build()).build();

            // Act
            Recipe fetched = saveAndReload(recipe);

            // Assert
            assertThat(fetched).isNotNull();
            assertThat(fetched.getIngredients())
                    .singleElement()
                    .extracting(Ingredient::getQuantity, as(BIG_DECIMAL))
                    .isEqualByComparingTo("123.123");
        }

        @Test
        void whenSameIngredientNameInDifferentRecipes_shouldPersistBothSuccessfully() {
            // Arrange
            Recipe carbonara = aCarbonara().build();
            Recipe cacioEPepe = aCacioEPepe().build();

            // Act
            recipeRepository.save(carbonara);
            recipeRepository.save(cacioEPepe);
            testEntityManager.flush();
            testEntityManager.clear();

            Recipe fetchedCarbonara = testEntityManager.find(Recipe.class, carbonara.getId());
            Recipe fetchedCacioEPepe = testEntityManager.find(Recipe.class, cacioEPepe.getId());

            // Assert
            assertThat(fetchedCarbonara).isNotNull();
            assertThat(fetchedCarbonara.getIngredients())
                    .extracting(Ingredient::getName)
                            .contains("Pecorino Romano");

            assertThat(fetchedCacioEPepe).isNotNull();
            assertThat(fetchedCacioEPepe.getIngredients())
                    .extracting(Ingredient::getName)
                    .contains("Pecorino Romano");
        }

        @ParameterizedTest(name = "{0} at limit -> Persist successfully")
        @MethodSource("limitValues")
        void whenValuesAtLimit_shouldPersistSuccessfully(String field, Recipe recipe) {
            assertThatNoException().isThrownBy(() -> saveAndReload(recipe));
        }

        @ParameterizedTest(name = "{0} too long -> DataException")
        @MethodSource("tooLongCases")
        void whenFieldTooLong_shouldThrowDataException(String field, Recipe recipe) {
            assertSaveFailsWith(recipe, DataException.class);
        }

        @ParameterizedTest(name = "{0} is null -> ConstraintViolationException")
        @MethodSource("nullValues")
        void whenContainingNotAllowedNullValue_shouldThrowConstraintViolationException(String field, Recipe recipe) {
            assertSaveFailsWith(recipe, ConstraintViolationException.class);
        }

        @ParameterizedTest
        @ValueSource(strings = {"0", "-1"})
        void whenIngredientQuantityZeroOrNegative_shouldThrowConstraintViolationException(String quantity) {
            // Arrange
            Recipe recipe = aRecipe().withIngredients(anIngredient().withQuantity(new BigDecimal(quantity)).build()).build();

            // Act & Assert
            assertSaveFailsWith(recipe, ConstraintViolationException.class);
        }

        @ParameterizedTest
        @ValueSource(ints = {0, -1})
        void whenStepNumberZeroOrNegative_shouldThrowConstraintViolationException(Integer stepNumber) {
            // Arrange
            Recipe recipe = recipeWithStepNumber(stepNumber);

            // Act & Assert
            assertSaveFailsWith(recipe, ConstraintViolationException.class);
        }

        @Test
        void whenDuplicateRecipe_shouldThrowEntityExistsException() {
            // Arrange
            Recipe recipe = aRecipe().build();
            saveAndReload(recipe);

            // Act & Assert
            assertSaveFailsWith(recipe, EntityExistsException.class);
        }

        @Test
        void whenRecipeNameIsDuplicate_shouldThrowConstraintViolationException() {
            // Arrange
            Recipe recipe = new Recipe("recipe", "foo");
            Recipe duplicateName = new Recipe("recipe", "bar");

            saveAndReload(recipe);

            // Act & Assert
            assertSaveFailsWith(duplicateName, ConstraintViolationException.class);
        }

        @Test
        void whenIngredientNameIsDuplicate_shouldThrowConstraintViolationException() {
            // Arrange
            Recipe recipe = aRecipe()
                    .withIngredients(
                            anIngredient().withName("Salt").build(),
                            anIngredient().withName("Salt").build()
                    ).build();

            // Act & Assert
            assertSaveFailsWith(recipe, ConstraintViolationException.class);
        }

        @Test
        void whenIngredientQuantityTooManyIntegers_shouldThrowDataException() {
            // Arrange
            Recipe recipe = aRecipe().withIngredients(anIngredient().withQuantity(new BigDecimal("12345678")).build()).build();

            // Act & Assert
            assertSaveFailsWith(recipe, DataException.class);
        }

        @Test
        void whenStepNumberIsDuplicate_shouldThrowConstraintViolationException() {
            // Arrange
            Recipe recipe = aRecipe().build();
            recipe.addStep(new Step("Step 2"));
            recipe.getSteps().getFirst().setStepNumber(2);

            // Act & Assert
            assertSaveFailsWith(recipe, ConstraintViolationException.class);
        }

        static Stream<Arguments> limitValues() {
            return Stream.of(
                    arguments(
                            "recipe name",
                            aRecipe().withName("a".repeat(100)).build()
                    ),
                    arguments(
                            "recipe description",
                            aRecipe().withDescription("a".repeat(500)).build()
                    ),
                    arguments(
                            "ingredient name",
                            aRecipe().withIngredients(anIngredient().withName("a".repeat(100)).build()).build()
                    ),
                    arguments(
                            "ingredient unit",
                            aRecipe().withIngredients(anIngredient().withUnit("a".repeat(50)).build()).build()
                    ),
                    arguments(
                            "ingredient quantity",
                            aRecipe().withIngredients(anIngredient().withQuantity(new BigDecimal("9999999.999")).build()).build()
                    ),
                    arguments(
                            "step description",
                            recipeWithStepDescription("a".repeat(500))
                    )
            );
        }

        static Stream<Arguments> nullValues() {
            return Stream.of(
                    arguments("recipe name", aRecipe().withName(null).build()),
                    arguments("ingredient name", aRecipe().withIngredients(anIngredient().withName(null).build()).build()),
                    arguments("ingredient unit", aRecipe().withIngredients(anIngredient().withUnit(null).build()).build()),
                    arguments("ingredient quantity", aRecipe().withIngredients(anIngredient().withQuantity(null).build()).build()),
                    arguments("step description", recipeWithStepDescription(null)),
                    arguments("step number", recipeWithStepNumber(null))
            );
        }

        static Stream<Arguments> tooLongCases() {
            return Stream.of(
                    arguments(
                            "recipe name",
                            aRecipe().withName("a".repeat(101)).build()
                    ),
                    arguments(
                            "recipe description",
                            aRecipe().withDescription("a".repeat(501)).build()
                    ),
                    arguments(
                            "ingredient name",
                            aRecipe().withIngredients(anIngredient().withName("a".repeat(101)).build()).build()
                    ),
                    arguments(
                            "ingredient unit",
                            aRecipe().withIngredients(anIngredient().withUnit("a".repeat(101)).build()).build()
                    ),
                    arguments(
                            "step description",
                            recipeWithStepDescription("a".repeat(501))
                    )
            );
        }

        private static Recipe recipeWithStepNumber(Integer stepNumber) {
            Recipe recipe = aRecipe().build();
            recipe.getSteps().getFirst().setStepNumber(stepNumber);
            return recipe;
        }

        private static Recipe recipeWithStepDescription(String description) {
            Recipe recipe = aRecipe().build();
            recipe.getSteps().getFirst().setDescription(description);
            return recipe;
        }
    }

    @Nested
    @DisplayName("ExistsByName")
    class ExistsByName {
        @Test
        void whenExists_shouldReturnTrue() {
            // Arrange
            String name = "Feijoada";
            Recipe recipe = aRecipe().withName(name).build();
            testEntityManager.persistAndFlush(recipe);
            testEntityManager.clear();

            // Act
            boolean exists = recipeRepository.existsByName(name);

            // Assert
            assertThat(exists).isTrue();
        }

        @Test
        void whenDoesNotExist_shouldReturnFalse() {
            // Arrange
            String name = "Feijoada";

            // Act
            boolean exists = recipeRepository.existsByName(name);

            // Assert
            assertThat(exists).isFalse();
        }

        @Test
        void whenDifferentNameExists_shouldReturnFalse() {
            // Arrange
            Recipe differentNameRecipe = aRecipe().withName("Pad Thai").build();
            testEntityManager.persistAndFlush(differentNameRecipe);
            testEntityManager.clear();

            // Act
            boolean exists = recipeRepository.existsByName("Feijoada");

            // Assert
            assertThat(exists).isFalse();
        }
    }

    @Nested
    @DisplayName("FindAllRecipes")
    class FindAllRecipes {
        @Test
        void whenRecipesExist_shouldReturnListOfRecipes() {
            // Arrange
            Recipe carbonara = aCarbonara().build();
            Recipe cacioEPepe = aCacioEPepe().build();
            Recipe recipe = aRecipe().build();

            testEntityManager.persistAndFlush(carbonara);
            testEntityManager.persistAndFlush(cacioEPepe);
            testEntityManager.persistAndFlush(recipe);
            testEntityManager.clear();

            // Act
            List<Recipe> fetched = recipeRepository.findAll();

            // Assert
            assertThat(fetched)
                    .extracting(Recipe::getName)
                            .containsExactlyInAnyOrder(carbonara.getName(), cacioEPepe.getName(), recipe.getName());
            assertThat(fetched)
                    .extracting(Recipe::getName, r -> r.getIngredients().size())
                    .containsExactlyInAnyOrder(
                            tuple(carbonara.getName(), carbonara.getIngredients().size()),
                            tuple(cacioEPepe.getName(), cacioEPepe.getIngredients().size()),
                            tuple(recipe.getName(), recipe.getIngredients().size())
                    );
        }

        @Test
        void whenNoRecipesExist_shouldReturnEmptyList() {
            // Arrange

            // Act
            List<Recipe> fetched = recipeRepository.findAll();

            // Assert
            assertThat(fetched).isEmpty();
        }
    }

    @Nested
    @DisplayName("FindRecipeById")
    class FindRecipeById {
        @Test
        void whenRecipeExists_shouldReturnFullyPersistedRecipeWithGeneratedId() {
            // Arrange
            String name = "Pizza";
            Recipe recipe = aRecipe().withName(name).build();
            Long savedId = saveAndReload(recipe).getId();

            // Act
            Optional<Recipe> fetched = recipeRepository.findById(savedId);

            // Assert
            List<String> expectedIngredientNames = recipe.getIngredients().stream().map(Ingredient::getName).toList();
            List<String> expectedIngredientUnits = recipe.getIngredients().stream().map(Ingredient::getUnit).toList();
            List<BigDecimal> expectedIngredientQuantities = recipe.getIngredients().stream().map(Ingredient::getQuantity).toList();
            List<String> expectedStepDescriptions = recipe.getSteps().stream().map(Step::getDescription).toList();
            List<Integer> expectedStepNumbers = recipe.getSteps().stream().map(Step::getStepNumber).toList();

            assertThat(fetched).isNotEmpty();
            assertThat(fetched.get().getId()).isEqualTo(savedId);
            assertThat(fetched.get().getName()).isEqualTo(recipe.getName());
            assertThat(fetched.get().getDescription()).isEqualTo(recipe.getDescription());
            assertThat(fetched.get().getIngredients())
                    .extracting(Ingredient::getName)
                    .containsExactlyInAnyOrderElementsOf(expectedIngredientNames);
            assertThat(fetched.get().getIngredients())
                    .extracting(Ingredient::getUnit)
                    .containsExactlyInAnyOrderElementsOf(expectedIngredientUnits);
            assertThat(fetched.get().getIngredients())
                    .extracting(Ingredient::getQuantity)
                    .usingElementComparator(BigDecimal::compareTo)
                    .containsExactlyInAnyOrderElementsOf(expectedIngredientQuantities);
            assertThat(fetched.get().getSteps())
                    .extracting(Step::getDescription)
                    .containsExactlyElementsOf(expectedStepDescriptions);
            assertThat(fetched.get().getSteps())
                    .extracting(Step::getStepNumber)
                    .containsExactlyElementsOf(expectedStepNumbers);
        }

        @Test
        void whenNonExistingIdProvided_shouldReturnEmptyOptional() {
            // Act
            Optional<Recipe> fetched = recipeRepository.findById(99L);

            // Assert
            assertThat(fetched).isEmpty();
        }
    }

    @Nested
    @DisplayName("DeleteRecipeById")
    class DeleteRecipeById {
        @Test
        void whenRecipeExists_shouldDeleteSuccessfullyAndReturnTrue() {
            // Arrange
            Recipe recipe1 = aRecipe().withName("Steak").build();
            Recipe recipe2 = aRecipe().withName("Pizza").build();
            Recipe recipe3 = aRecipe().withName("Pasta").build();
            Long savedId1 = saveAndReload(recipe1).getId();
            Long savedId2 = saveAndReload(recipe2).getId();
            Long savedId3 = saveAndReload(recipe3).getId();

            // Act
            boolean isDeleted = recipeRepository.deleteById(savedId2);
            testEntityManager.flush();
            testEntityManager.clear();

            // Assert
            assertThat(isDeleted).isTrue();
            assertThat(testEntityManager.find(Recipe.class, savedId2)).isNull();

            String jpqlRecipesCount = "SELECT COUNT(r) FROM Recipe r";
            Long recipesCount = testEntityManager.getEntityManager()
                    .createQuery(jpqlRecipesCount, Long.class)
                    .getSingleResult();
            assertThat(recipesCount).isEqualTo(2);

            String jpqlIngredients = "SELECT i FROM Ingredient i";
            List<Ingredient> ingredients = testEntityManager.getEntityManager()
                    .createQuery(jpqlIngredients, Ingredient.class)
                    .getResultList();

            assertThat(ingredients)
                    .extracting(Ingredient::getRecipe)
                    .extracting(Recipe::getId)
                    .containsExactlyInAnyOrder(savedId1, savedId3);

            String jpqlSteps = "SELECT s FROM Step s";
            List<Step> steps = testEntityManager.getEntityManager()
                    .createQuery(jpqlSteps, Step.class)
                    .getResultList();

            assertThat(steps)
                    .extracting(Step::getRecipe)
                    .extracting(Recipe::getId)
                    .containsExactlyInAnyOrder(savedId1, savedId3);
        }

        @Test
        void whenNonExistingIdProvided_shouldReturnFalse() {
            // Act
            boolean isDeleted = recipeRepository.deleteById(999L);

            // Assert
            assertThat(isDeleted).isFalse();
        }
    }

    private Recipe saveAndReload(Recipe recipe) {
        recipeRepository.save(recipe);
        testEntityManager.flush();
        testEntityManager.clear();
        return testEntityManager.find(Recipe.class, recipe.getId());
    }

    private void assertSaveFailsWith(Recipe recipe, Class<? extends Throwable> exception) {
        assertThatThrownBy(() -> {
            recipeRepository.save(recipe);
            testEntityManager.flush();
        }).isInstanceOf(exception);
    }

    private static RecipeTestBuilder aCarbonara() {
        return aRecipe()
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

    private static RecipeTestBuilder aCacioEPepe() {
        return aRecipe()
                .withName("Cacio e Pepe")
                .withDescription("A minimalist Roman masterpiece.")
                .withIngredients(
                        new Ingredient("Tonnarelli or Spaghetti", "grams", new BigDecimal("350")),
                        new Ingredient("Pecorino Romano", "grams", new BigDecimal("120")),
                        new Ingredient("Black pepper", "tablespoons", new BigDecimal("1.5")),
                        new Ingredient("Salt", "to taste", new BigDecimal("1")))
                .withStepDescriptions(CACIO_E_PEPE_STEPS_DESCRIPTIONS);
    }
}
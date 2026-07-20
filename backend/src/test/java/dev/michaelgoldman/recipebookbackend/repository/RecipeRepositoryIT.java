package dev.michaelgoldman.recipebookbackend.repository;

import dev.michaelgoldman.recipebookbackend.TestcontainersConfiguration;
import dev.michaelgoldman.recipebookbackend.entity.Ingredient;
import dev.michaelgoldman.recipebookbackend.entity.Recipe;
import dev.michaelgoldman.recipebookbackend.entity.Step;
import dev.michaelgoldman.recipebookbackend.repository.projection.RecipeSummary;
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
import static dev.michaelgoldman.recipebookbackend.fixtures.RecipeFixtures.CARBONARA_STEPS_DESCRIPTIONS;
import static dev.michaelgoldman.recipebookbackend.fixtures.RecipeFixtures.NON_EXISTING_ID;
import static dev.michaelgoldman.recipebookbackend.fixtures.RecipeFixtures.aCacioEPepe;
import static dev.michaelgoldman.recipebookbackend.fixtures.RecipeFixtures.aCarbonara;
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
class RecipeRepositoryIT {

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
                    .containsExactly(1, 2, 3, 4, 5);
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
        void whenRecipeNameIsDuplicateWithDifferentCasing_shouldThrowConstraintViolationException() {
            // Arrange
            Recipe recipe = new Recipe("recipe", "foo");
            Recipe duplicateName = new Recipe("RECIPE", "bar");

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
            assertThatThrownBy(() -> saveAndForceConstraintCheck(recipe)).isInstanceOf(ConstraintViolationException.class);
        }

        @Test
        void whenIngredientNameIsDuplicateWithDifferentCasing_shouldThrowConstraintViolationException() {
            // Arrange
            Recipe recipe = aRecipe()
                    .withIngredients(
                            anIngredient().withName("Salt").build(),
                            anIngredient().withName("SALT").build()
                    ).build();

            // Act & Assert
            assertThatThrownBy(() -> saveAndForceConstraintCheck(recipe)).isInstanceOf(ConstraintViolationException.class);
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
            assertThatThrownBy(() -> saveAndForceConstraintCheck(recipe)).isInstanceOf(ConstraintViolationException.class);
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

        @Test
        void whenNameExistsWithDifferentCasing_shouldReturnTrue() {
            // Arrange
            Recipe recipe = aRecipe().withName("Feijoada").build();
            testEntityManager.persistAndFlush(recipe);
            testEntityManager.clear();

            // Act & Assert
            assertThat(recipeRepository.existsByName("FEIJOADA")).isTrue();
        }
    }

    @Nested
    @DisplayName("ExistsByNameExcludingId")
    class ExistsByNameExcludingId {
        @Test
        void whenNameExistsOnDifferentId_shouldReturnTrue() {
            // Arrange
            Long savedId = testEntityManager.persistAndFlush(aRecipe().withName("Feijoada").build()).getId();
            testEntityManager.clear();

            // Act & Assert
            assertThat(recipeRepository.existsByNameExcludingId("Feijoada", savedId + 1)).isTrue();
        }

        @Test
        void whenNameExistsOnlyOnSameId_shouldReturnFalse() {
            // Arrange
            Long savedId = testEntityManager.persistAndFlush(aRecipe().withName("Feijoada").build()).getId();
            testEntityManager.clear();

            // Act & Assert
            assertThat(recipeRepository.existsByNameExcludingId("Feijoada", savedId)).isFalse();
        }

        @Test
        void whenNameExistsOnDifferentIdWithDifferentCasing_shouldReturnTrue() {
            // Arrange
            Long savedId = testEntityManager.persistAndFlush(aRecipe().withName("Feijoada").build()).getId();
            testEntityManager.clear();

            // Act & Assert
            assertThat(recipeRepository.existsByNameExcludingId("FEIJOADA", savedId + 1)).isTrue();
        }

        @Test
        void whenNameDoesNotExist_shouldReturnFalse() {
            // Act & Assert
            assertThat(recipeRepository.existsByNameExcludingId("Feijoada", NON_EXISTING_ID)).isFalse();
        }
    }

    @Nested
    @DisplayName("FindAllRecipes")
    class FindAllRecipes {
        @Test
        void whenRecipesExist_shouldReturnListOfRecipeSummaries() {
            // Arrange
            Recipe carbonara = aCarbonara().build();
            Recipe cacioEPepe = aCacioEPepe().build();
            Recipe recipe = aRecipe().build();

            testEntityManager.persistAndFlush(carbonara);
            testEntityManager.persistAndFlush(cacioEPepe);
            testEntityManager.persistAndFlush(recipe);
            testEntityManager.clear();

            // Act
            List<RecipeSummary> fetched = recipeRepository.findAll();

            // Assert
            assertThat(fetched)
                    .extracting(RecipeSummary::name)
                            .containsExactlyInAnyOrder(carbonara.getName(), cacioEPepe.getName(), recipe.getName());
            assertThat(fetched)
                    .extracting(RecipeSummary::name, RecipeSummary::ingredientCount, RecipeSummary::stepCount)
                    .containsExactlyInAnyOrder(
                            tuple(carbonara.getName(), carbonara.getIngredients().size(), carbonara.getSteps().size()),
                            tuple(cacioEPepe.getName(), cacioEPepe.getIngredients().size(), cacioEPepe.getSteps().size()),
                            tuple(recipe.getName(), recipe.getIngredients().size(), recipe.getSteps().size())
                    );
        }

        @Test
        void whenNoRecipesExist_shouldReturnEmptyList() {
            // Act
            List<RecipeSummary> fetched = recipeRepository.findAll();

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
            Long savedId = testEntityManager.persistAndFlush(recipe).getId();
            testEntityManager.clear();

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
            Optional<Recipe> fetched = recipeRepository.findById(NON_EXISTING_ID);

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
            Long savedId1 = testEntityManager.persistAndFlush(recipe1).getId();
            Long savedId2 = testEntityManager.persistAndFlush(recipe2).getId();
            Long savedId3 = testEntityManager.persistAndFlush(recipe3).getId();
            testEntityManager.clear();

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
            boolean isDeleted = recipeRepository.deleteById(NON_EXISTING_ID);

            // Assert
            assertThat(isDeleted).isFalse();
        }
    }

    @Nested
    @DisplayName("OrphanRemovalOnUpdate")
    class OrphanRemovalOnUpdate {
        @Test
        void whenIngredientsAndStepsReplaced_shouldDeleteOrphanedIngredientAndStepsRows() {
            // Arrange
            Long savedId = testEntityManager.persistAndFlush(aCarbonara().build()).getId();
            testEntityManager.clear();
            String psqlIngredients = "SELECT i FROM Ingredient i WHERE i.recipe.id = :savedId";
            String psqlSteps = "SELECT s FROM Step s WHERE s.recipe.id = :savedId";

            List<Ingredient> oldIngredients = testEntityManager.getEntityManager()
                    .createQuery(psqlIngredients, Ingredient.class)
                    .setParameter("savedId", savedId)
                    .getResultList();

            List<Step> oldSteps = testEntityManager.getEntityManager()
                    .createQuery(psqlSteps, Step.class)
                    .setParameter("savedId", savedId)
                    .getResultList();

            // Act
            Recipe managed = testEntityManager.find(Recipe.class, savedId);
            assertThat(managed).isNotNull();
            managed.replaceIngredients(List.of(
                    new Ingredient("Salt", "grams", new BigDecimal("150")),
                    new Ingredient("Water", "ml", new BigDecimal("500")),
                    new Ingredient("Bread", "kg", new BigDecimal("1.5"))));
            managed.replaceSteps(List.of(
                    new Step("Mix the salt and water for 5 minutes."),
                    new Step("Soak the bread in the saltwater mix until it dissolves.")
            ));
            testEntityManager.flush();
            testEntityManager.clear();

            List<Ingredient> newIngredients = testEntityManager.getEntityManager()
                    .createQuery(psqlIngredients, Ingredient.class)
                    .setParameter("savedId", savedId)
                    .getResultList();

            List<Step> newSteps = testEntityManager.getEntityManager()
                    .createQuery(psqlSteps, Step.class)
                    .setParameter("savedId", savedId)
                    .getResultList();

            Long totalIngredientCount = testEntityManager.getEntityManager()
                    .createQuery("SELECT COUNT(i) FROM Ingredient i", Long.class)
                    .getSingleResult();

            Long totalStepCount = testEntityManager.getEntityManager()
                    .createQuery("SELECT COUNT(s) FROM Step s", Long.class)
                    .getSingleResult();

            // Assert
            assertThat(oldIngredients).hasSize(5);
            assertThat(oldSteps).hasSize(5);
            assertThat(newIngredients).hasSize(3);
            assertThat(newSteps).hasSize(2);
            assertThat(totalIngredientCount).isEqualTo(3);
            assertThat(totalStepCount).isEqualTo(2);
            assertThat(newIngredients)
                    .extracting(Ingredient::getName)
                    .containsExactlyInAnyOrder("Salt", "Water", "Bread");
            assertThat(newIngredients)
                    .extracting(Ingredient::getUnit)
                    .containsExactlyInAnyOrder("grams", "ml", "kg");
            assertThat(newIngredients)
                    .extracting(Ingredient::getQuantity)
                    .usingElementComparator(BigDecimal::compareTo)
                    .containsExactlyInAnyOrder(new BigDecimal("150"), new BigDecimal("500"), new BigDecimal("1.5"));
            assertThat(newSteps)
                    .extracting(Step::getDescription)
                    .containsExactly("Mix the salt and water for 5 minutes.", "Soak the bread in the saltwater mix until it dissolves.");
            assertThat(newSteps)
                    .extracting(Step::getStepNumber)
                    .containsExactly(1, 2);
        }
    }

    private Recipe saveAndReload(Recipe recipe) {
        recipeRepository.save(recipe);
        testEntityManager.flush();
        testEntityManager.clear();
        return testEntityManager.find(Recipe.class, recipe.getId());
    }

    private void saveAndForceConstraintCheck(Recipe recipe) {
        recipeRepository.save(recipe);
        testEntityManager.flush();
        testEntityManager.getEntityManager()
                .createNativeQuery("SET CONSTRAINTS ALL IMMEDIATE")
                .executeUpdate();
    }

    private void assertSaveFailsWith(Recipe recipe, Class<? extends Throwable> exception) {
        assertThatThrownBy(() -> {
            recipeRepository.save(recipe);
            testEntityManager.flush();
        }).isInstanceOf(exception);
    }
}
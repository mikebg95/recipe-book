package dev.michaelgoldman.recipebookbackend.service;

import dev.michaelgoldman.recipebookbackend.api.model.Ingredient;
import dev.michaelgoldman.recipebookbackend.api.model.RecipeRequest;
import dev.michaelgoldman.recipebookbackend.api.model.RecipeRequestTestBuilder;
import dev.michaelgoldman.recipebookbackend.api.model.RecipeResponse;
import dev.michaelgoldman.recipebookbackend.api.model.RecipeResponseTestBuilder;
import dev.michaelgoldman.recipebookbackend.api.model.RecipeSummaryResponse;
import dev.michaelgoldman.recipebookbackend.entity.Recipe;
import dev.michaelgoldman.recipebookbackend.entity.RecipeTestBuilder;
import dev.michaelgoldman.recipebookbackend.exception.RecipeDoesNotExistException;
import dev.michaelgoldman.recipebookbackend.exception.RecipeNameAlreadyExistsException;
import dev.michaelgoldman.recipebookbackend.mapper.RecipeMapper;
import dev.michaelgoldman.recipebookbackend.repository.RecipeRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static dev.michaelgoldman.recipebookbackend.api.model.RecipeRequestTestBuilder.aRecipeRequest;
import static dev.michaelgoldman.recipebookbackend.api.model.RecipeResponseTestBuilder.aRecipeResponse;
import static dev.michaelgoldman.recipebookbackend.api.model.RecipeSummaryResponseTestBuilder.aRecipeSummaryResponse;
import static dev.michaelgoldman.recipebookbackend.entity.RecipeTestBuilder.aRecipe;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecipeServiceTest {
    private final static Long NON_EXISTING_ID = 999L;

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

    @InjectMocks
    RecipeService recipeService;

    @Mock
    RecipeRepository recipeRepository;

    @Mock
    RecipeMapper recipeMapper;

    @Nested
    @DisplayName("createRecipe")
    class CreateRecipe {
        @Test
        void whenValidRecipeRequestProvided_shouldReturnMappedResponse() {
            // Arrange
            RecipeRequest request = aRecipeRequest().build();
            RecipeResponse expectedResponse = aRecipeResponse().build();
            Recipe mappedEntity = aRecipe().build();
            Recipe savedEntity = aRecipe().build();
            when(recipeMapper.toEntity(request)).thenReturn(mappedEntity);
            when(recipeRepository.save(mappedEntity)).thenReturn(savedEntity);
            when(recipeMapper.toResponse(savedEntity)).thenReturn(expectedResponse);

            // Act
            RecipeResponse responseFromService = recipeService.createRecipe(request);

            // Assert
            assertThat(responseFromService).isSameAs(expectedResponse);
        }

        @Test
        void whenDuplicateRecipeNameProvided_shouldThrowRecipeNameAlreadyExistsException() {
            // Arrange
            String name = "Steak & Fries";
            RecipeRequest request = aRecipeRequest().withName(name).build();
            when(recipeRepository.existsByName(request.getName())).thenReturn(true);

            // Act & Assert
            assertThatThrownBy(() -> recipeService.createRecipe(request))
                    .isInstanceOf(RecipeNameAlreadyExistsException.class)
                    .hasMessageContaining(name);
            verify(recipeRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("getAllRecipes")
    class GetAllRecipes {
        @Test
        void whenRecipesExist_shouldReturnMappedResponse() {
            // Arrange
            List<RecipeSummaryResponse> responses = List.of(aRecipeSummaryResponse().build(), aRecipeSummaryResponse().build());

            List<Recipe> entities = List.of(aRecipe().withName("Steak").build(), aRecipe().withName("Pizza").build());
            when(recipeRepository.findAll()).thenReturn(entities);
            when(recipeMapper.toResponseList(entities)).thenReturn(responses);

            // Act
            List<RecipeSummaryResponse> fetched = recipeService.getAll();

            // Assert
            assertThat(fetched).isEqualTo(responses);
        }

        @Test
        void whenNoRecipesExist_shouldReturnEmptyListResponse() {
            // Arrange
            List<Recipe> entities = Collections.emptyList();
            List<RecipeSummaryResponse> responses = Collections.emptyList();
            when(recipeRepository.findAll()).thenReturn(entities);
            when(recipeMapper.toResponseList(entities)).thenReturn(responses);

            // Act
            List<RecipeSummaryResponse> fetched = recipeService.getAll();

            // Assert
            assertThat(fetched).isEqualTo(responses);
        }
    }

    @Nested
    @DisplayName("getRecipeById")
    class GetRecipeById {
        @Test
        void whenRecipeExists_shouldReturnMappedRecipeResponse() {
            // Arrange
            Long recipeId = 5L;
            Recipe entity = aRecipe().withId(recipeId).build();
            RecipeResponse expected = aRecipeResponse().withId(recipeId).build();
            when(recipeRepository.findById(recipeId)).thenReturn(Optional.of(entity));
            when(recipeMapper.toResponse(entity)).thenReturn(expected);

            // Act
            RecipeResponse response = recipeService.getById(recipeId);

            // Assert
            assertThat(response).isSameAs(expected);
        }

        @Test
        void whenRecipeDoesNotExist_shouldThrowRecipeDoesNotExistException() {
            // Arrange
            Optional<Recipe> empty = Optional.empty();
            when(recipeRepository.findById(NON_EXISTING_ID)).thenReturn(empty);

            // Act & Assert
            assertThatThrownBy(() -> recipeService.getById(NON_EXISTING_ID))
                    .isInstanceOf(RecipeDoesNotExistException.class)
                    .hasMessageContaining(String.valueOf(NON_EXISTING_ID));
        }
    }

    @Nested
    @DisplayName("deleteRecipeById")
    class DeleteRecipeById {
        @Test
        void whenRecipeExists_shouldDeleteSuccessfully() {
            // Arrange
            Long recipeId = 1L;
            when(recipeRepository.deleteById(recipeId)).thenReturn(true);

            // Act
            recipeService.deleteById(recipeId);

            // Assert
            verify(recipeRepository).deleteById(recipeId);
        }

        @Test
        void whenNonExistingRecipeIdProvided_shouldThrowRecipeDoesNotExistException() {
            // Arrange
            when(recipeRepository.deleteById(NON_EXISTING_ID)).thenReturn(false);

            // Act & Assert
            assertThatThrownBy(() -> recipeService.deleteById(NON_EXISTING_ID))
                    .isInstanceOf(RecipeDoesNotExistException.class)
                    .hasMessageContaining(String.valueOf(NON_EXISTING_ID));
        }
    }

    @Nested
    @DisplayName("updateRecipeById")
    class UpdateRecipeById {
        @Test
        void whenValidRecipeDetailsProvided_shouldReturnMappedUpdatedResponse() {
            // Arrange
            Long recipeId = 5L;

            RecipeRequest carbonaraRequest = aCarbonaraRequest().build();
            Recipe cacioEPepeEntity = aCacioEPepeEntity().withId(recipeId).build();
            RecipeResponse carbonaraResponse = aCarbonaraResponse().build();

            when(recipeRepository.findById(recipeId)).thenReturn(Optional.of(cacioEPepeEntity));
            when(recipeRepository.existsByName(carbonaraRequest.getName())).thenReturn(false);
            when(recipeMapper.toResponse(cacioEPepeEntity)).thenReturn(carbonaraResponse);

            // Act
            RecipeResponse response = recipeService.updateById(recipeId, carbonaraRequest);

            // Assert
            assertThat(response).isSameAs(carbonaraResponse);
            verify(recipeMapper).updateEntity(cacioEPepeEntity, carbonaraRequest);
        }

        @Test
        void whenDuplicateRecipeNameProvided_shouldThrowRecipeNameAlreadyExistsException() {
            // Arrange
            Long recipeId = 5L;
            RecipeRequest request = aRecipeRequest().withName("Pasta").build();
            Recipe oldEntity = aRecipe().withId(recipeId).withName("Steak").build();
            when(recipeRepository.findById(recipeId)).thenReturn(Optional.of(oldEntity));
            when(recipeRepository.existsByName(request.getName())).thenReturn(true);

            // Act & Assert
            assertThatThrownBy(() -> recipeService.updateById(recipeId, request))
                    .isInstanceOf(RecipeNameAlreadyExistsException.class)
                    .hasMessageContaining(request.getName());
        }

        @Test
        void whenNameUnchanged_shouldNotThrowRecipeNameAlreadyExistsException() {
            // Arrange
            Long recipeId = 5L;
            RecipeRequest request = aRecipeRequest().withName("Steak").build();
            Recipe entity = aRecipe().withId(recipeId).withName("Steak").build();
            when(recipeRepository.findById(5L)).thenReturn(Optional.of(entity));

            // Act
            recipeService.updateById(recipeId, request);

            // Assert
            verify(recipeRepository, never()).existsByName(any());
        }

        @Test
        void whenRecipeDoesNotExist_shouldThrowRecipeDoesNotExistException() {
            // Arrange
            RecipeRequest request = aRecipeRequest().build();
            when(recipeRepository.findById(NON_EXISTING_ID)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> recipeService.updateById(NON_EXISTING_ID, request))
                    .isInstanceOf(RecipeDoesNotExistException.class)
                    .hasMessageContaining(String.valueOf(NON_EXISTING_ID));
        }
    }

    private static RecipeRequestTestBuilder aCarbonaraRequest() {
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

    private static RecipeResponseTestBuilder aCarbonaraResponse() {
        return aRecipeResponse()
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

    private static RecipeTestBuilder aCacioEPepeEntity() {
        return aRecipe()
                .withId(5L)
                .withName("Cacio e Pepe")
                .withDescription("A minimalist Roman masterpiece.")
                .withIngredients(
                        new dev.michaelgoldman.recipebookbackend.entity.Ingredient("Tonnarelli or Spaghetti", "grams", new BigDecimal("350")),
                        new dev.michaelgoldman.recipebookbackend.entity.Ingredient("Pecorino Romano", "grams", new BigDecimal("120")),
                        new dev.michaelgoldman.recipebookbackend.entity.Ingredient("Black pepper", "tablespoons", new BigDecimal("1.5")),
                        new dev.michaelgoldman.recipebookbackend.entity.Ingredient("Salt", "to taste", new BigDecimal("1")))
                .withStepDescriptions(CACIO_E_PEPE_STEPS_DESCRIPTIONS);
    }
}

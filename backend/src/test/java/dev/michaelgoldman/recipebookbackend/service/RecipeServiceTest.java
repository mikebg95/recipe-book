package dev.michaelgoldman.recipebookbackend.service;

import dev.michaelgoldman.recipebookbackend.api.model.RecipeRequest;
import dev.michaelgoldman.recipebookbackend.api.model.RecipeResponse;
import dev.michaelgoldman.recipebookbackend.entity.Recipe;
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

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static dev.michaelgoldman.recipebookbackend.api.model.RecipeRequestTestBuilder.aRecipeRequest;
import static dev.michaelgoldman.recipebookbackend.api.model.RecipeResponseTestBuilder.aRecipeResponse;
import static dev.michaelgoldman.recipebookbackend.entity.RecipeTestBuilder.aRecipe;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecipeServiceTest {
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
            List<RecipeResponse> responses = List.of(aRecipeResponse().build(), aRecipeResponse().build());

            List<Recipe> entities = List.of(aRecipe().withName("Steak").build(), aRecipe().withName("Pizza").build());
            when(recipeRepository.findAll()).thenReturn(entities);
            when(recipeMapper.toResponseList(entities)).thenReturn(responses);

            // Act
            List<RecipeResponse> fetched = recipeService.getAll();

            // Assert
            assertThat(fetched).isEqualTo(responses);
        }

        @Test
        void whenNoRecipesExist_shouldReturnEmptyListResponse() {
            // Arrange
            List<Recipe> entities = Collections.emptyList();
            List<RecipeResponse> responses = Collections.emptyList();
            when(recipeRepository.findAll()).thenReturn(entities);
            when(recipeMapper.toResponseList(entities)).thenReturn(responses);

            // Act
            List<RecipeResponse> fetched = recipeService.getAll();

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
            Long doesNotExistId = 99L;
            Optional<Recipe> empty = Optional.empty();
            when(recipeRepository.findById(doesNotExistId)).thenReturn(empty);

            // Act & Assert
            assertThatThrownBy(() -> recipeService.getById(doesNotExistId))
                    .isInstanceOf(RecipeDoesNotExistException.class)
                    .hasMessageContaining(String.valueOf(doesNotExistId));
        }
    }
}

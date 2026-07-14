package dev.michaelgoldman.recipebookbackend.controller;

import dev.michaelgoldman.recipebookbackend.api.model.Ingredient;
import dev.michaelgoldman.recipebookbackend.api.model.RecipeRequest;
import dev.michaelgoldman.recipebookbackend.api.model.RecipeResponse;
import dev.michaelgoldman.recipebookbackend.config.WebConfig;
import dev.michaelgoldman.recipebookbackend.exception.GlobalExceptionHandler;
import dev.michaelgoldman.recipebookbackend.service.RecipeService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import static dev.michaelgoldman.recipebookbackend.api.model.IngredientTestBuilder.anIngredient;
import static dev.michaelgoldman.recipebookbackend.api.model.RecipeRequestTestBuilder.aRecipeRequest;
import static dev.michaelgoldman.recipebookbackend.api.model.RecipeResponseTestBuilder.aRecipeResponse;
import static org.hamcrest.Matchers.endsWith;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RecipeController.class)
@Import({WebConfig.class, GlobalExceptionHandler.class})
public class RecipeControllerTest {
    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    RecipeService recipeService;

    @Nested
    @DisplayName("POST /recipes")
    class CreateRecipe {
        @ParameterizedTest
        @NullSource
        @ValueSource(strings = { "A valid recipe description." })
        void whenValidRequest_shouldReturn201(String description) throws Exception {
            RecipeRequest request = aRecipeRequest().withDescription(description).build();
            assertCreatedAndDelegates(request);
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("limitValues")
        @DisplayName("value at limit should create successfully")
        void whenValuesAtLimit_shouldReturn201(String scenario, RecipeRequest request) throws Exception {
            assertCreatedAndDelegates(request);
        }

        @Test
        void whenDuplicateIngredientNameInsideRecipe_shouldReturn400() throws Exception {
            Ingredient ingredient = new Ingredient("Bacon", "grams", new BigDecimal("150"));
            Ingredient duplicate = new Ingredient("Bacon", "kg", new BigDecimal("2"));
            RecipeRequest request = aRecipeRequest().withIngredients(ingredient, duplicate).build();

            assertReturns400(toJson(request));
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("emptyLists")
        @DisplayName("list with length zero should return 400")
        void whenRequiredListIsEmpty_shouldReturn400(String scenario, RecipeRequest request) throws Exception {
            assertReturns400(toJson(request));
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("nullValues")
        @DisplayName("null values for non-nullable fields should return 400")
        void whenRequiredFieldIsNull_shouldReturn400(String scenario, RecipeRequest request) throws Exception {
            assertReturns400(toJson(request));
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("emptyStrings")
        @DisplayName("empty strings should return 400")
        void whenRequiredStringIsBlank_shouldReturn400(String scenario, RecipeRequest request) throws Exception {
            assertReturns400(toJson(request));
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("tooLongValues")
        @DisplayName("values exceeding limit should return 400")
        void whenValueExceedsLimit_shouldReturn400(String scenario, RecipeRequest request) throws Exception {
            assertReturns400(toJson(request));
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("nonPositiveQuantities")
        @DisplayName("invalid values should return 400")
        void whenQuantityIsZeroOrNegative_shouldReturn400(String scenario, RecipeRequest request) throws Exception {
            assertReturns400(toJson(request));
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("malformedJsonBodies")
        @DisplayName("invalid json should return 400")
        void whenInvalidJson_shouldReturn400(String scenario, String requestJson) throws Exception {
            assertReturns400(requestJson);
        }

        @Test
        void whenValidationFails_shouldReturnProblemDetailBody() throws Exception {
            RecipeRequest request = aRecipeRequest().withName("").build();

            mockMvc.perform(postRecipe().content(toJson(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                    .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                    .andExpect(jsonPath("$.title").exists())
                    .andExpect(jsonPath("$.detail").exists())
                    .andExpect(jsonPath("$.errors.name").exists());
        }

        static Stream<Arguments> malformedJsonBodies() {
            return Stream.of(
                    arguments("missing recipe name", """
                                    {
                                        "name": ,
                                        "description": "A velvety Roman classic of eggs, guanciale, and Pecorino Romano.",
                                        "ingredients": [
                                            { "name": "Guanciale", "unit": "grams", "quantity": 150 },
                                        ],
                                        "steps": [
                                            { "description": "Crisp the diced guanciale in a pan over medium heat; set aside." },
                                        ]
                                    }
                            """),
                    arguments("ingredient quantity not a number", """
                                    {
                                        "name": "Spaghetti alla Carbonara",
                                        "description": "A velvety Roman classic of eggs, guanciale, and Pecorino Romano.",
                                        "ingredients": [
                                            { "name": "Guanciale", "unit": "grams", "quantity": "abc" }
                                        ],
                                        "steps": [
                                            { "description": "Crisp the diced guanciale in a pan over medium heat; set aside." }
                                        ]
                                    }
                            """)
            );
        }

        static Stream<Arguments> nonPositiveQuantities() {
            return Stream.of(
                    arguments("ingredient quantity zero", aRecipeRequest().withIngredients(anIngredient().withQuantity(new BigDecimal("0")).build()).build()),
                    arguments("ingredient quantity negative", aRecipeRequest().withIngredients(anIngredient().withQuantity(new BigDecimal("-1")).build()).build())
            );
        }

        static Stream<Arguments> tooLongValues() {
            return Stream.of(
                    arguments(
                            "recipe name", aRecipeRequest().withName("a".repeat(101)).build()
                    ),
                    arguments(
                            "recipe description", aRecipeRequest().withDescription("a".repeat(501)).build()
                    ),
                    arguments(
                            "ingredient name", aRecipeRequest().withIngredients(anIngredient().withName("a".repeat(101)).build()).build()
                    ),
                    arguments(
                            "ingredient unit", aRecipeRequest().withIngredients(anIngredient().withUnit("a".repeat(51)).build()).build()
                    ),
                    arguments(
                            "ingredient quantity too many integer digits", aRecipeRequest().withIngredients(anIngredient().withQuantity(new BigDecimal("10000000")).build()).build()
                    ),
                    arguments(
                            "ingredient quantity too many decimals", aRecipeRequest().withIngredients(anIngredient().withQuantity(new BigDecimal("1.0001")).build()).build()
                    ),
                    arguments(
                            "step description", aRecipeRequest().withStepDescriptions(List.of("a".repeat(501))).build()
                    )
            );
        }

        static Stream<Arguments> emptyStrings() {
            return Stream.of(
                    arguments("recipe name empty", aRecipeRequest().withName("").build()),
                    arguments("ingredient name empty", aRecipeRequest().withIngredients(anIngredient().withName("").build()).build()),
                    arguments("ingredient unit empty", aRecipeRequest().withIngredients(anIngredient().withUnit("").build()).build()),
                    arguments("step description empty", aRecipeRequest().withStepDescriptions(Collections.singletonList("")).build()),
                    arguments("recipe name whitespace", aRecipeRequest().withName("   ").build()),
                    arguments("ingredient name whitespace", aRecipeRequest().withIngredients(anIngredient().withName("   ").build()).build()),
                    arguments("ingredient unit whitespace", aRecipeRequest().withIngredients(anIngredient().withUnit("   ").build()).build()),
                    arguments("step description whitespace", aRecipeRequest().withStepDescriptions(Collections.singletonList("   ")).build())
            );
        }

        static Stream<Arguments> nullValues() {
            return Stream.of(
                        arguments("recipe name", aRecipeRequest().withName(null).build()),
                        arguments("ingredient name", aRecipeRequest().withIngredients(anIngredient().withName(null).build()).build()),
                        arguments("ingredient unit", aRecipeRequest().withIngredients(anIngredient().withUnit(null).build()).build()),
                        arguments("ingredient quantity", aRecipeRequest().withIngredients(anIngredient().withQuantity(null).build()).build()),
                        arguments("step description", aRecipeRequest().withStepDescriptions(Collections.singletonList(null)).build())
            );
        }

        static Stream<Arguments> emptyLists() {
            return Stream.of(
                    arguments(
                            "ingredients", aRecipeRequest().withIngredients().build()
                    ),
                    arguments(
                            "steps", aRecipeRequest().withStepDescriptions(new ArrayList<>()).build()
                    )
            );
        }

        static Stream<Arguments> limitValues() {
            return Stream.of(
                    arguments(
                            "recipe name", aRecipeRequest().withName("a".repeat(100)).build()
                    ),
                    arguments(
                            "recipe description", aRecipeRequest().withDescription("a".repeat(500)).build()
                    ),
                    arguments(
                            "ingredient name", aRecipeRequest().withIngredients(anIngredient().withName("a".repeat(100)).build()).build()
                    ),
                    arguments(
                            "ingredient unit", aRecipeRequest().withIngredients(anIngredient().withUnit("a".repeat(50)).build()).build()
                    ),
                    arguments(
                            "ingredient quantity", aRecipeRequest().withIngredients(anIngredient().withQuantity(new BigDecimal("9999999.999")).build()).build()
                    ),
                    arguments(
                            "step description", aRecipeRequest().withStepDescriptions(List.of("a".repeat(500))).build()
                    )
            );
        }
    }

    private MockHttpServletRequestBuilder postRecipe() {
        return post("/api/v1/recipes")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON);
    }

    private String toJson(Object obj) {
        return objectMapper.writeValueAsString(obj);
    }

    private void assertCreatedAndDelegates(RecipeRequest request) throws Exception {
        // Arrange
        Long responseId = 3L;
        RecipeResponse response = aRecipeResponse().withId(responseId).build();
        when(recipeService.createRecipe(request)).thenReturn(response);

        // Act & Assert
        mockMvc.perform(postRecipe().content(toJson(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", endsWith("/recipes/" + responseId)))
                .andExpect(jsonPath("$.id").value(responseId));

        // Assert
        verify(recipeService).createRecipe(request);
    }

    private void assertReturns400(String jsonRequest) throws Exception {
        mockMvc.perform(postRecipe().content(jsonRequest))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));

        verify(recipeService, never()).createRecipe(any());
    }
}

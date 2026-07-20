package dev.michaelgoldman.recipebookbackend.integration;

import dev.michaelgoldman.recipebookbackend.TestcontainersConfiguration;
import dev.michaelgoldman.recipebookbackend.api.model.Ingredient;
import dev.michaelgoldman.recipebookbackend.api.model.ProblemDetail;
import dev.michaelgoldman.recipebookbackend.api.model.RecipeRequest;
import dev.michaelgoldman.recipebookbackend.api.model.RecipeResponse;
import dev.michaelgoldman.recipebookbackend.api.model.RecipeSummaryResponse;
import dev.michaelgoldman.recipebookbackend.api.model.StepRequest;
import dev.michaelgoldman.recipebookbackend.api.model.StepResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.client.RestTestClient;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

import static dev.michaelgoldman.recipebookbackend.api.model.RecipeRequestTestBuilder.aRecipeRequest;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
@AutoConfigureRestTestClient
@ActiveProfiles("test")
class RecipeBookApiIT {
    @Autowired
    RestTestClient restTestClient;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @BeforeEach
    void init() {
        jdbcTemplate.execute("TRUNCATE TABLE recipes, ingredients, steps CASCADE");
    }

    @Test
    void createRecipe_thenFetchById_returnsPersistedRecipe() {
        RecipeRequest request = aRecipeRequest().build();

        // save
        Long savedId = Objects.requireNonNull(restTestClient
                        .post()
                        .uri("/api/v1/recipes")
                        .body(request)
                        .exchange()
                        .expectStatus().isCreated()
                        .expectHeader().contentType(MediaType.APPLICATION_JSON)
                        .expectBody(RecipeResponse.class)
                        .returnResult()
                        .getResponseBody())
                .getId();


        // get by id
        RecipeResponse fetched = restTestClient
                .get()
                .uri("/api/v1/recipes/{id}", savedId)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectBody(RecipeResponse.class)
                .returnResult()
                .getResponseBody();

        // assert
        assertThat(fetched).isNotNull();
        assertThat(fetched.getId()).isEqualTo(savedId);
        assertThat(fetched.getName()).isEqualTo(request.getName());
        assertThat(fetched.getDescription()).isEqualTo(request.getDescription());

        List<String> expectedIngredientNames = request.getIngredients().stream().map(Ingredient::getName).toList();
        List<String> expectedIngredientUnits = request.getIngredients().stream().map(Ingredient::getUnit).toList();
        List<BigDecimal> expectedIngredientQuantities = request.getIngredients().stream().map(Ingredient::getQuantity).toList();
        List<String> expectedStepDescriptions = request.getSteps().stream().map(StepRequest::getDescription).toList();

        assertThat(fetched.getIngredients())
                .extracting(Ingredient::getName)
                .containsExactlyInAnyOrderElementsOf(expectedIngredientNames);
        assertThat(fetched.getIngredients())
                .extracting(Ingredient::getUnit)
                .containsExactlyInAnyOrderElementsOf(expectedIngredientUnits);
        assertThat(fetched.getIngredients())
                .extracting(Ingredient::getQuantity)
                .usingElementComparator(BigDecimal::compareTo)
                .containsExactlyInAnyOrderElementsOf(expectedIngredientQuantities);
        assertThat(fetched.getSteps())
                .extracting(StepResponse::getDescription)
                .containsExactlyElementsOf(expectedStepDescriptions);
    }

    @Test
    void createRecipe_withDescriptionNull_thenFetchById_returnsNullDescription() {
        RecipeRequest request = aRecipeRequest().withDescription(null).build();

        // save
        Long savedId = Objects.requireNonNull(restTestClient
                        .post()
                        .uri("/api/v1/recipes")
                        .body(request)
                        .exchange()
                        .expectStatus().isCreated()
                        .expectHeader().contentType(MediaType.APPLICATION_JSON)
                        .expectBody(RecipeResponse.class)
                        .returnResult()
                        .getResponseBody())
                .getId();


        // get by id
        RecipeResponse fetched = restTestClient
                .get()
                .uri("/api/v1/recipes/{id}", savedId)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectBody(RecipeResponse.class)
                .returnResult()
                .getResponseBody();

        // assert
        assertThat(fetched).isNotNull();
        assertThat(fetched.getId()).isEqualTo(savedId);
        assertThat(fetched.getDescription()).isNull();
    }

    @Test
    void createRecipe_withUntrimmedName_thenFetchById_returnsTrimmedName() {
        RecipeRequest request = aRecipeRequest().withName("   Steak & Fries      ").build();

        // save
        Long savedId = Objects.requireNonNull(restTestClient
                        .post()
                        .uri("/api/v1/recipes")
                        .body(request)
                        .exchange()
                        .expectStatus().isCreated()
                        .expectHeader().contentType(MediaType.APPLICATION_JSON)
                        .expectBody(RecipeResponse.class)
                        .returnResult()
                        .getResponseBody())
                .getId();


        // get by id
        RecipeResponse fetched = restTestClient
                .get()
                .uri("/api/v1/recipes/{id}", savedId)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectBody(RecipeResponse.class)
                .returnResult()
                .getResponseBody();

        // assert
        assertThat(fetched).isNotNull();
        assertThat(fetched.getId()).isEqualTo(savedId);
        assertThat(fetched.getName()).isEqualTo("Steak & Fries");
    }

    @Test
    void createTwoRecipes_thenFetchAll_shouldReturnListOfPersistedRecipes() {
        RecipeRequest request1 = aRecipeRequest().withName("Steak").build();
        RecipeRequest request2 = aRecipeRequest().withName("Pizza").build();

        // save
        restTestClient
                .post()
                .uri("/api/v1/recipes")
                .body(request1)
                .exchange()
                .expectStatus().isCreated()
                .expectHeader().contentType(MediaType.APPLICATION_JSON);

        // save another
        restTestClient
                .post()
                .uri("/api/v1/recipes")
                .body(request2)
                .exchange()
                .expectStatus().isCreated()
                .expectHeader().contentType(MediaType.APPLICATION_JSON);

        // get all
        List<RecipeSummaryResponse> responses = restTestClient
                .get()
                .uri("/api/v1/recipes")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectBody(new ParameterizedTypeReference<List<RecipeSummaryResponse>>() {})
                .returnResult()
                .getResponseBody();

        // assert
        assertThat(responses).hasSize(2);
        assertThat(responses).extracting(RecipeSummaryResponse::getId).doesNotContainNull();
        assertThat(responses).extracting(RecipeSummaryResponse::getName).containsExactlyInAnyOrder("Steak", "Pizza");
        assertThat(responses).extracting(RecipeSummaryResponse::getNumberOfIngredients).containsExactlyInAnyOrder(1, 1);
        assertThat(responses).extracting(RecipeSummaryResponse::getNumberOfSteps).containsExactlyInAnyOrder(1, 1);
    }

    @Test
    void createRecipe_thenUpdateById_thenFetchById_shouldReturnPersistedRecipe() {
        RecipeRequest old = aRecipeRequest().withName("Steak").build();

        // save
        Long savedId = Objects.requireNonNull(restTestClient
                        .post()
                        .uri("/api/v1/recipes")
                        .body(old)
                        .exchange()
                        .expectStatus().isCreated()
                        .expectHeader().contentType(MediaType.APPLICATION_JSON)
                        .expectBody(RecipeResponse.class)
                        .returnResult()
                        .getResponseBody())
                .getId();

        RecipeRequest update = aRecipeRequest().withName("Pizza").build();

        // update
        Long updatedId = Objects.requireNonNull(restTestClient
                        .put()
                        .uri("/api/v1/recipes/{id}", savedId)
                        .body(update)
                        .exchange()
                        .expectStatus().isOk()
                        .expectHeader().contentType(MediaType.APPLICATION_JSON)
                        .expectBody(RecipeResponse.class)
                        .returnResult()
                        .getResponseBody())
                .getId();

        // get by id
        RecipeResponse fetched = restTestClient
                .get()
                .uri("/api/v1/recipes/{id}", updatedId)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectBody(RecipeResponse.class)
                .returnResult()
                .getResponseBody();

        // assert
        assertThat(fetched).isNotNull();
        assertThat(fetched.getId()).isEqualTo(savedId).isEqualTo(updatedId);
        assertThat(fetched.getName()).isEqualTo(update.getName());
    }

    @Test
    void createRecipe_thenDeleteById_thenFetchById_shouldReturnNotFoundProblemDetail() {
        RecipeRequest request = aRecipeRequest().withName("Steak").build();

        // save
        Long savedId = Objects.requireNonNull(restTestClient
                        .post()
                        .uri("/api/v1/recipes")
                        .body(request)
                        .exchange()
                        .expectStatus().isCreated()
                        .expectBody(RecipeResponse.class)
                        .returnResult()
                        .getResponseBody())
                .getId();

        // delete
        restTestClient
                .delete()
                .uri("/api/v1/recipes/{id}", savedId)
                .exchange()
                .expectStatus().isNoContent();

        // fetch non-existing recipe -> exception
        ProblemDetail problemDetail = restTestClient
                .get()
                .uri("/api/v1/recipes/{id}", savedId)
                .exchange()
                .expectStatus().isNotFound()
                .expectHeader().contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .expectBody(ProblemDetail.class)
                .returnResult()
                .getResponseBody();

        assertThat(problemDetail).isNotNull();
        assertThat(problemDetail.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        assertThat(problemDetail.getTitle()).isEqualTo("Recipe not found.");
        assertThat(problemDetail.getDetail()).isEqualTo("No recipe found with id " + savedId);
    }

    @Test
    void createRecipe_thenCreateDuplicateRecipeName_shouldReturnConflictProblemDetail() {
        RecipeRequest request = aRecipeRequest().withName("Steak").build();

        // save
        restTestClient
                .post()
                .uri("/api/v1/recipes")
                .body(request)
                .exchange()
                .expectStatus().isCreated();

        RecipeRequest duplicate = aRecipeRequest().withName("Steak").build();

        // save duplicate name -> exception
        ProblemDetail problemDetail = restTestClient
                .post()
                .uri("/api/v1/recipes")
                .body(duplicate)
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.CONFLICT)
                .expectHeader().contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .expectBody(ProblemDetail.class)
                .returnResult()
                .getResponseBody();

        // assert
        assertThat(problemDetail).isNotNull();
        assertThat(problemDetail.getStatus()).isEqualTo(HttpStatus.CONFLICT.value());
        assertThat(problemDetail.getTitle()).isEqualTo("Recipe name already exists.");
        assertThat(problemDetail.getDetail()).isEqualTo("Recipe with name " + duplicate.getName() + " already exists.");
    }

    @Test
    void createRecipe_withDuplicateIngredientNames_shouldReturnBadRequestProblemDetail() {
        RecipeRequest duplicateIngredientNames = aRecipeRequest().withIngredients(
                new Ingredient("Salt", "grams", new BigDecimal("150")),
                new Ingredient("Salt", "kg", new BigDecimal("0.3")))
        .build();

        ProblemDetail problemDetail = restTestClient
                .post()
                .uri("/api/v1/recipes")
                .body(duplicateIngredientNames)
                .exchange()
                .expectStatus().isBadRequest()
                .expectHeader().contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .expectBody(ProblemDetail.class)
                .returnResult()
                .getResponseBody();

        // assert
        assertThat(problemDetail).isNotNull();
        assertThat(problemDetail.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(problemDetail.getTitle()).isEqualTo("Validation failed");
        assertThat(problemDetail.getDetail()).isEqualTo("One or more fields are invalid.");
    }
}

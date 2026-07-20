package dev.michaelgoldman.recipebookbackend.exception;

import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {
    private static final String RECIPE_DUPLICATE_NAME_TITLE = "Recipe name already exists.";
    private static final String RECIPE_NOT_FOUND_TITLE = "Recipe not found.";
    private static final String INVALID_ARGUMENT_TITLE = "Validation failed";
    private static final String INVALID_ARGUMENT_DETAIL = "One or more fields are invalid.";
    private static final String DATA_INTEGRITY_TITLE = "Conflict";
    private static final String DATA_INTEGRITY_DETAIL = "The request conflicts with existing data.";
    private static final String OPTIMISTIC_LOCKING_FAILURE_TITLE = "Concurrent modification";
    private static final String OPTIMISTIC_LOCKING_FAILURE_DETAIL = "This recipe was modified by another request. Reload it and try again.";
    private static final String UNEXPECTED_ERROR_TITLE = "Unexpected error";
    private static final String UNEXPECTED_ERROR_DETAIL = "An unexpected error occurred.";

    @ExceptionHandler(RecipeNameAlreadyExistsException.class)
    public ProblemDetail handleRecipeNameAlreadyExists(RecipeNameAlreadyExistsException e) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, e.getMessage());
        problem.setTitle(RECIPE_DUPLICATE_NAME_TITLE);
        return problem;
    }

    @ExceptionHandler(RecipeDoesNotExistException.class)
    public ProblemDetail handleRecipeDoesNotExist(RecipeDoesNotExistException e) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
        problem.setTitle(RECIPE_NOT_FOUND_TITLE);
        return problem;
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, @NonNull HttpHeaders headers, @NonNull HttpStatusCode status, @NonNull WebRequest request) {
        ProblemDetail problem = ex.getBody();
        problem.setTitle(INVALID_ARGUMENT_TITLE);
        problem.setDetail(INVALID_ARGUMENT_DETAIL);

        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(fe -> {
                    assert fe.getDefaultMessage() != null;
                    errors.merge(fe.getField(), fe.getDefaultMessage(),
                            (a, b) -> a + "; " + b);
                });
        problem.setProperty("errors", errors);

        return handleExceptionInternal(ex, problem, headers, status, request);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail handleDataIntegrityViolation(DataIntegrityViolationException e) {
        log.error("Data integrity violation", e);
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.CONFLICT, DATA_INTEGRITY_DETAIL);
        problem.setTitle(DATA_INTEGRITY_TITLE);
        return problem;
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ProblemDetail handleOptimisticLockingFailure(OptimisticLockingFailureException e) {
        log.error("Optimistic locking failure", e);
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.CONFLICT, OPTIMISTIC_LOCKING_FAILURE_DETAIL);
        problem.setTitle(OPTIMISTIC_LOCKING_FAILURE_TITLE);
        return problem;
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpectedException(Exception e) {
        log.error("Unhandled exception", e);
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, UNEXPECTED_ERROR_DETAIL);
        problem.setTitle(UNEXPECTED_ERROR_TITLE);
        return problem;
    }
}

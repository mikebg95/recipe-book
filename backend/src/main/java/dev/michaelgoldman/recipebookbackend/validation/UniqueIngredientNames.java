package dev.michaelgoldman.recipebookbackend.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Constraint(validatedBy = UniqueIngredientNamesValidator.class)
public @interface UniqueIngredientNames {
    String message() default "Ingredient names inside a recipe must be unique";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}

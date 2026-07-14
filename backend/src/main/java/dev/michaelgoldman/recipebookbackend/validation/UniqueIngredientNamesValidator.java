package dev.michaelgoldman.recipebookbackend.validation;

import dev.michaelgoldman.recipebookbackend.api.model.Ingredient;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

public class UniqueIngredientNamesValidator implements ConstraintValidator<UniqueIngredientNames, List<Ingredient>> {
    @Override
    public boolean isValid(List<Ingredient> ingredients, ConstraintValidatorContext context) {
        if (ingredients == null || ingredients.isEmpty()) {
            return true;
        }

        List<String> names = ingredients.stream()
                .map(Ingredient::getName)
                .filter(Objects::nonNull)
                .map(name -> name.strip().toLowerCase(Locale.ROOT))
                .toList();

        return names.size() == Set.copyOf(names).size();
    }
}

package dev.michaelgoldman.recipebookbackend.repository;

import dev.michaelgoldman.recipebookbackend.entity.Recipe;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

import java.util.List;


@Repository
public class RecipeRepositoryImpl implements RecipeRepository {
    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public Recipe save(Recipe recipe) {
        entityManager.persist(recipe);
        return recipe;
    }

    @Override
    public boolean existsByName(String recipeName) {
        String jpql = "SELECT COUNT(r) FROM Recipe r WHERE r.name = :recipeName";
        Long count = entityManager.createQuery(jpql, Long.class)
                .setParameter("recipeName", recipeName)
                .getSingleResult();

        return count > 0;
    }

    @Override
    public List<Recipe> findAll() {
        String jpql = "SELECT r FROM Recipe r";
        return entityManager.createQuery(jpql, Recipe.class).getResultList();
    }
}

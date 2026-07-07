package dev.michaelgoldman.recipebookbackend;

import org.springframework.boot.SpringApplication;

public class TestRecipeBookBackendApplication {

	static void main(String[] args) {
		SpringApplication.from(RecipeBookBackendApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
